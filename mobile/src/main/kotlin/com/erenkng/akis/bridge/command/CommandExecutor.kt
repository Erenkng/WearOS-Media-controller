package com.erenkng.akis.bridge.command

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import com.erenkng.akis.bridge.session.SessionTracker
import com.erenkng.akis.bridge.session.SessionTracker.Companion.isPlaying
import com.erenkng.akis.shared.model.Command

/** Komut sonucu; `volume` yalnızca ses komutlarında dolu. */
data class ExecutionResult(val ok: Boolean, val volume: Int? = null)

/**
 * [Command] → `MediaController.TransportControls`. Ana iş parçacığında çağrılır.
 *
 * Desteklenmeyen eylemler `ok=false` döner; saat bunu "Bu uygulama desteklemiyor" olarak gösterir.
 */
class CommandExecutor(
    private val context: Context,
    private val tracker: SessionTracker,
    private val volume: VolumeController,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    fun execute(command: Command): ExecutionResult {
        val controller = tracker.snapshot.value.focused
        val controls = controller?.transportControls

        return when (command) {
            Command.Play -> controls?.play().asResult() ?: resumeViaMediaKey()
            Command.Pause -> controls?.pause().asResult()
            Command.Toggle -> when {
                controller == null -> resumeViaMediaKey()
                controller.playbackState.isPlaying() -> controls?.pause().asResult()
                else -> controls?.play().asResult()
            }

            Command.Next -> controls?.skipToNext().asResult()
            Command.Prev -> controls?.skipToPrevious().asResult()
            is Command.SeekTo -> controls?.seekTo(command.positionMs.coerceAtLeast(0)).asResult()
            is Command.SeekBy -> controller?.let { seekBy(it, command.deltaMs) }
            is Command.AdjustVolume -> volumeResult(volume.adjust(controller, command.delta))
            is Command.SetVolume -> volumeResult(volume.set(controller, command.volume))
            is Command.SkipToQueueItem -> controls?.skipToQueueItem(command.queueItemId).asResult()
            is Command.Custom -> controls?.sendCustomAction(command.actionId, null).asResult()
            is Command.FocusSession -> ExecutionResult(tracker.focus(command.sessionId))
            is Command.ResumeLast -> resume(command.packageName)
            Command.OpenOnPhone -> openOnPhone(controller)

            // Platform MediaController shuffle/repeat sunmaz (OPEN_QUESTIONS S6); uyku zamanlayıcısı Faz 2.
            is Command.SetShuffle,
            is Command.SetRepeat,
            is Command.SleepTimer,
            -> null
        } ?: ExecutionResult(ok = false)
    }

    private fun Unit?.asResult(): ExecutionResult? = this?.let { ExecutionResult(ok = true) }

    private fun volumeResult(actual: Int?): ExecutionResult =
        if (actual == null) ExecutionResult(ok = false) else ExecutionResult(ok = true, volume = actual)

    private fun seekBy(controller: MediaController, deltaMs: Long): ExecutionResult {
        val state = controller.playbackState ?: return ExecutionResult(false)
        var position = state.position
        if (state.state == PlaybackState.STATE_PLAYING && state.lastPositionUpdateTime > 0) {
            val elapsed = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
            position += (elapsed * state.playbackSpeed).toLong()
        }
        val duration = controller.metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        var target = (position + deltaMs).coerceAtLeast(0)
        if (duration > 0) target = target.coerceAtMost(duration)
        controller.transportControls.seekTo(target)
        return ExecutionResult(true)
    }

    /**
     * "Son çalanı sürdür" / "Son kullanılanlar" satırı (BRIEF Bölüm 7.2 ve 10).
     * Paket verildiyse önce o uygulamanın açık oturumu, yoksa o uygulamaya doğrudan oynat tuşu denenir.
     */
    private fun resume(packageName: String?): ExecutionResult {
        if (packageName == null) {
            tracker.snapshot.value.focused?.let {
                it.transportControls.play()
                return ExecutionResult(true)
            }
            return resumeViaMediaKey()
        }
        tracker.controllerForPackage(packageName)?.let {
            it.transportControls.play()
            return ExecutionResult(true)
        }
        if (sendMediaButtonTo(packageName)) return ExecutionResult(true)
        return resumeViaMediaKey()
    }

    /** Sistem, oynat tuşunu son medya uygulamasına iletir. */
    private fun resumeViaMediaKey(): ExecutionResult {
        val now = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0))
        audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 0))
        return ExecutionResult(true)
    }

    private fun sendMediaButtonTo(packageName: String): Boolean {
        val query = Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(packageName)
        val receiver = context.packageManager.queryBroadcastReceivers(query, 0).firstOrNull()?.activityInfo
            ?: return false
        val component = ComponentName(receiver.packageName, receiver.name)
        return try {
            val now = SystemClock.uptimeMillis()
            listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).forEach { action ->
                context.sendBroadcast(
                    Intent(Intent.ACTION_MEDIA_BUTTON)
                        .setComponent(component)
                        .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(now, now, action, KeyEvent.KEYCODE_MEDIA_PLAY, 0)),
                )
            }
            true
        } catch (e: RuntimeException) {
            Log.w(TAG, "MEDIA_BUTTON gönderilemedi: $packageName", e)
            false
        }
    }

    /** Arka plandan Activity başlatma kısıtlı; en iyi çaba (OPEN_QUESTIONS S7). */
    private fun openOnPhone(controller: MediaController?): ExecutionResult? {
        val pendingIntent = controller?.sessionActivity ?: return null
        return try {
            val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    .toBundle()
            } else {
                null
            }
            pendingIntent.send(context, 0, null, null, null, null, options)
            ExecutionResult(true)
        } catch (e: Exception) {
            Log.w(TAG, "Uygulama telefonda açılamadı", e)
            ExecutionResult(false)
        }
    }

    private companion object {
        const val TAG = "CommandExecutor"
    }
}
