package com.erenkng.akis.bridge.session

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.SystemClock
import com.erenkng.akis.bridge.command.VolumeController
import com.erenkng.akis.bridge.session.SessionTracker.Companion.isPlaying
import com.erenkng.akis.bridge.session.SessionTracker.Companion.title
import com.erenkng.akis.bridge.sync.ArtworkEncoder
import com.erenkng.akis.shared.model.CustomActionInfo
import com.erenkng.akis.shared.model.NowPlayingState
import com.erenkng.akis.shared.model.PlaybackStatus
import com.erenkng.akis.shared.model.SessionSummary

class MappedState(
    val state: NowPlayingState,
    val art: ArtworkEncoder.EncodedArt?,
)

/** `MediaController` → protokol modelleri. Kapak sıkıştırdığı için ana iş parçacığı dışında çağrılır. */
class StateMapper(
    private val appInfo: AppInfoResolver,
    private val artwork: ArtworkEncoder,
    private val volume: VolumeController,
) {

    fun map(sessionId: String, controller: MediaController): MappedState {
        val metadata = controller.metadata
        val playback = controller.playbackState
        val volumeInfo = volume.read(controller)
        val art = artwork.encode(sessionId, metadata)

        val state = NowPlayingState(
            sessionId = sessionId,
            packageName = controller.packageName,
            appLabel = appInfo.label(controller.packageName),
            title = metadata.title(),
            artist = metadata?.firstString(
                MediaMetadata.METADATA_KEY_ARTIST,
                MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
                MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
            ).orEmpty(),
            album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0) ?: 0,
            positionMs = playback?.position?.coerceAtLeast(0) ?: 0,
            positionUpdatedAtEpochMs = playback.positionUpdatedAtEpochMs(),
            speed = playback?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
            state = playback.toStatus(),
            actions = playback?.actions ?: 0,
            customActions = playback?.customActions.orEmpty().map { CustomActionInfo(it.action, it.name.toString()) },
            volume = volumeInfo.volume,
            volumeMax = volumeInfo.max,
            isRemote = volumeInfo.isRemote,
            outputLabel = volume.outputLabel(volumeInfo.isRemote),
            artHash = art?.hash.orEmpty(),
            // shuffle/repeat: platform MediaController bunları açığa çıkarmaz (OPEN_QUESTIONS S6), Faz 2.
        )
        return MappedState(state, art)
    }

    fun summary(sessionId: String, controller: MediaController): SessionSummary = SessionSummary(
        sessionId = sessionId,
        packageName = controller.packageName,
        appLabel = appInfo.label(controller.packageName),
        title = controller.metadata.title(),
        state = controller.playbackState.toStatus(),
    )

    private companion object {
        fun MediaMetadata.firstString(vararg keys: String): String? =
            keys.firstNotNullOfOrNull { key -> getString(key)?.takeIf { it.isNotBlank() } }

        /**
         * `PlaybackState.lastPositionUpdateTime`, telefonun `elapsedRealtime` saatine göredir.
         * Saat bunu kendi saatiyle kıyaslayabilsin diye duvar saatine (epoch) çevrilir.
         */
        fun PlaybackState?.positionUpdatedAtEpochMs(): Long {
            val nowEpoch = System.currentTimeMillis()
            val updatedElapsed = this?.lastPositionUpdateTime ?: 0L
            if (updatedElapsed <= 0L) return nowEpoch
            return nowEpoch - (SystemClock.elapsedRealtime() - updatedElapsed)
        }

        fun PlaybackState?.toStatus(): PlaybackStatus {
            if (this.isPlaying()) return PlaybackStatus.PLAYING
            return when (this?.state) {
                PlaybackState.STATE_PAUSED -> PlaybackStatus.PAUSED
                PlaybackState.STATE_BUFFERING,
                PlaybackState.STATE_CONNECTING,
                PlaybackState.STATE_SKIPPING_TO_NEXT,
                PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
                PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM,
                -> PlaybackStatus.BUFFERING

                PlaybackState.STATE_ERROR -> PlaybackStatus.ERROR
                else -> PlaybackStatus.STOPPED
            }
        }
    }
}
