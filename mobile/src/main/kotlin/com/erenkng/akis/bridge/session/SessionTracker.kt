package com.erenkng.akis.bridge.session

import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Takip edilen oturumların anlık görüntüsü. `version` her değişiklikte artar. */
data class TrackerSnapshot(
    val version: Long,
    val focusedId: String?,
    /** Sistemin öncelik sırasıyla aktif oturumlar. */
    val sessions: List<Pair<String, MediaController>>,
) {
    val focused: MediaController? get() = sessions.firstOrNull { it.first == focusedId }?.second

    companion object {
        val EMPTY = TrackerSnapshot(0, null, emptyList())
    }
}

/**
 * Telefonun aktif medya oturumlarını izler (BRIEF Bölüm 10, telefon tarafı akışı, 1–3).
 *
 * Tüm durum ana iş parçacığında değişir. Dışarıya yalnızca değişmez [TrackerSnapshot] verilir.
 * Debounce (150 ms) [com.erenkng.akis.bridge.sync.StatePublisher]'dadır.
 */
class SessionTracker(
    context: Context,
    private val listenerComponent: ComponentName,
    private val appInfo: AppInfoResolver,
    private val recentApps: RecentAppsStore,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val sessionManager = appContext.getSystemService(MediaSessionManager::class.java)
    private val audioManager = appContext.getSystemService(AudioManager::class.java)

    private class Tracked(
        val controller: MediaController,
        val callback: MediaController.Callback,
        var wasPlaying: Boolean,
        var lastTitle: String,
    )

    private val tracked = LinkedHashMap<String, Tracked>()
    private var userSelectedId: String? = null
    private var lastPlayingId: String? = null
    private var version = 0L

    private val _snapshot = MutableStateFlow(TrackerSnapshot.EMPTY)
    val snapshot: StateFlow<TrackerSnapshot> = _snapshot

    var isRunning: Boolean = false
        private set

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        onSessionsChanged(controllers.orEmpty())
    }

    /** Yerel medya sesi değişimi için MediaController callback'i yok; ayarlar tablosu izlenir. */
    private val volumeObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) = recompute()
    }

    /** Kulaklık bağlanınca/çıkınca `outputLabel` güncellensin. */
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = recompute()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = recompute()
    }

    /** @return izin yoksa false. */
    fun start(): Boolean {
        check(Looper.myLooper() == Looper.getMainLooper()) { "SessionTracker ana iş parçacığında başlatılmalı" }
        if (isRunning) return true
        val initial = try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, mainHandler)
            sessionManager.getActiveSessions(listenerComponent)
        } catch (e: SecurityException) {
            Log.w(TAG, "Bildirim erişimi yok", e)
            return false
        }
        appContext.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, volumeObserver)
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)
        isRunning = true
        onSessionsChanged(initial)
        return true
    }

    fun stop() {
        if (!isRunning) return
        runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsListener) }
        appContext.contentResolver.unregisterContentObserver(volumeObserver)
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        tracked.values.forEach { it.controller.unregisterCallback(it.callback) }
        tracked.clear()
        isRunning = false
        recompute()
    }

    /** Saatten `FOCUS_SESSION`. */
    fun focus(sessionId: String): Boolean {
        if (sessionId !in tracked) return false
        userSelectedId = sessionId
        recompute()
        return true
    }

    fun controllerForPackage(packageName: String): MediaController? =
        tracked.values.firstOrNull { it.controller.packageName == packageName }?.controller

    private fun onSessionsChanged(controllers: List<MediaController>) {
        val incoming = LinkedHashMap<String, MediaController>()
        controllers.forEach { incoming[idOf(it)] = it }

        // Kapananlar → "Son kullanılanlar".
        (tracked.keys - incoming.keys).forEach { id ->
            tracked.remove(id)?.let { gone ->
                gone.controller.unregisterCallback(gone.callback)
                recentApps.record(
                    RecentApp(
                        packageName = gone.controller.packageName,
                        appLabel = appInfo.label(gone.controller.packageName),
                        lastTitle = gone.lastTitle,
                    ),
                )
            }
        }

        // Yeni gelenler; sıra sistemin öncelik sırasıyla aynı tutulur.
        val reordered = LinkedHashMap<String, Tracked>()
        incoming.forEach { (id, controller) ->
            reordered[id] = tracked[id] ?: Tracked(
                controller = controller,
                callback = ControllerCallback(id).also { controller.registerCallback(it, mainHandler) },
                wasPlaying = controller.playbackState.isPlaying(),
                lastTitle = controller.metadata.title(),
            ).also { if (it.wasPlaying) lastPlayingId = id }
        }
        tracked.clear()
        tracked.putAll(reordered)
        recompute()
    }

    private inner class ControllerCallback(private val id: String) : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            val entry = tracked[id] ?: return
            val playing = state.isPlaying()
            var newlyPlaying: String? = null
            if (playing && !entry.wasPlaying) {
                lastPlayingId = id
                newlyPlaying = id
            }
            entry.wasPlaying = playing
            recompute(newlyPlayingId = newlyPlaying)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            tracked[id]?.let { entry ->
                metadata.title().takeIf { it.isNotEmpty() }?.let { entry.lastTitle = it }
            }
            recompute()
        }

        override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) = recompute()

        override fun onAudioInfoChanged(info: MediaController.PlaybackInfo?) = recompute()

        override fun onSessionDestroyed() {
            // Liste değişikliği de gelecektir; yine de hemen düşür ki saat beklemesin.
            mainHandler.post {
                if (id in tracked) {
                    onSessionsChanged(tracked.filterKeys { it != id }.values.map { it.controller })
                }
            }
        }
    }

    private fun recompute(newlyPlayingId: String? = null) {
        if (FocusPolicy.shouldClearUserSelection(userSelectedId, newlyPlayingId, tracked.keys)) {
            userSelectedId = null
        }
        val candidates = tracked.map { (id, t) -> FocusCandidate(id, t.wasPlaying) }
        val focusedId = FocusPolicy.choose(candidates, userSelectedId, lastPlayingId)
        version++
        _snapshot.value = TrackerSnapshot(
            version = version,
            focusedId = focusedId,
            sessions = tracked.map { (id, t) -> id to t.controller },
        )
    }

    companion object {
        private const val TAG = "SessionTracker"

        /**
         * Oturum kimliği: paket + token. Token'ın `hashCode`'u `equals` ile tutarlıdır,
         * aynı oturum farklı `getActiveSessions` çağrılarında aynı kimliği alır.
         */
        fun idOf(controller: MediaController): String =
            controller.packageName + "/" + Integer.toHexString(controller.sessionToken.hashCode())

        fun PlaybackState?.isPlaying(): Boolean = when (this?.state) {
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING,
            -> true

            else -> false
        }

        fun MediaMetadata?.title(): String =
            this?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: this?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                ?: ""
    }
}
