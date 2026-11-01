package com.erenkng.akis.wear.player

import android.graphics.Bitmap
import com.erenkng.akis.shared.model.BridgeStatusCode
import com.erenkng.akis.shared.model.MediaActions
import com.erenkng.akis.shared.model.NowPlayingState
import com.erenkng.akis.shared.model.PlaybackStatus
import com.erenkng.akis.shared.model.SessionsPayload
import com.erenkng.akis.wear.data.ConnectionStatus

enum class OnboardingStep { WELCOME, INSTALL_BRIDGE, GRANT_PERMISSION }

/** Ana ekranda ne gösterilecek (BRIEF Bölüm 6, "Duruma göre görünüm"). */
sealed interface PlayerScreen {
    /** Henüz hiçbir veri yok (ilk kurulumdan hemen sonra, bir an). */
    data object Loading : PlayerScreen

    data class Onboarding(val firstStep: OnboardingStep) : PlayerScreen

    data object Empty : PlayerScreen

    data object NowPlaying : PlayerScreen

    data object Disconnected : PlayerScreen
}

/** Çıkış ikonunu seçmek için kaba sınıflandırma (BRIEF Bölüm 6: kulaklık / telefon / saat / hoparlör). */
enum class OutputKind { HEADPHONES, PHONE, WATCH, REMOTE }

data class PlayerUiState(
    val screen: PlayerScreen = PlayerScreen.Loading,
    val track: NowPlayingState? = null,
    val artwork: Bitmap? = null,
    val appIcon: Bitmap? = null,
    /** Oynat/duraklat iyimser: dokunur dokunmaz değişir, telefondan durum gelince doğrulanır. */
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val canPlayPause: Boolean = false,
    val canPrevious: Boolean = false,
    val canNext: Boolean = false,
    val transportMode: TransportMode = TransportMode.SKIP,
    val outputKind: OutputKind = OutputKind.PHONE,
    val connection: ConnectionStatus = ConnectionStatus.UNKNOWN,
    val bridgeStatus: BridgeStatusCode? = null,
    val bridgeName: String? = null,
    val sessions: SessionsPayload = SessionsPayload(),
    val icons: Map<String, Bitmap> = emptyMap(),
) {
    val isPaused: Boolean get() = track != null && !isPlaying && !isBuffering

    companion object {
        fun reduce(
            track: NowPlayingState?,
            artwork: Bitmap?,
            connection: ConnectionStatus,
            bridgeStatus: BridgeStatusCode?,
            bridgeName: String?,
            welcomeSeen: Boolean,
            optimisticPlaying: Boolean?,
            sessions: SessionsPayload,
            icons: Map<String, Bitmap>,
        ): PlayerUiState {
            val screen = when {
                connection == ConnectionStatus.BRIDGE_MISSING -> PlayerScreen.Onboarding(
                    if (welcomeSeen) OnboardingStep.INSTALL_BRIDGE else OnboardingStep.WELCOME,
                )
                connection == ConnectionStatus.CONNECTED && bridgeStatus == BridgeStatusCode.PERMISSION_MISSING ->
                    PlayerScreen.Onboarding(if (welcomeSeen) OnboardingStep.GRANT_PERMISSION else OnboardingStep.WELCOME)
                connection == ConnectionStatus.UNREACHABLE -> PlayerScreen.Disconnected
                track != null -> PlayerScreen.NowPlaying
                connection == ConnectionStatus.UNKNOWN -> PlayerScreen.Loading
                else -> PlayerScreen.Empty
            }
            val actions = track?.actions ?: 0L
            val playing = optimisticPlaying ?: (track?.state == PlaybackStatus.PLAYING)
            val mode = TransportPolicy.mode(actions)
            return PlayerUiState(
                screen = screen,
                track = track,
                artwork = artwork,
                appIcon = track?.packageName?.let { icons[it] },
                isPlaying = playing,
                isBuffering = optimisticPlaying == null && track?.state == PlaybackStatus.BUFFERING,
                canPlayPause = track != null && (if (playing) MediaActions.canPause(actions) else MediaActions.canPlay(actions)),
                canPrevious = if (mode == TransportMode.SEEK) true else MediaActions.canSkipPrevious(actions),
                canNext = if (mode == TransportMode.SEEK) true else MediaActions.canSkipNext(actions),
                transportMode = mode,
                outputKind = outputKindOf(track),
                connection = connection,
                bridgeStatus = bridgeStatus,
                bridgeName = bridgeName,
                sessions = sessions,
                icons = icons,
            )
        }

        /**
         * `outputLabel` serbest metin; ayrı bir alan protokolde yok. Telefon tarafının ürettiği
         * etiket biçimine göre sınıflandırılır (bkz. `:mobile` VolumeController.outputLabel).
         */
        private fun outputKindOf(track: NowPlayingState?): OutputKind {
            if (track == null) return OutputKind.PHONE
            if (track.isRemote) return OutputKind.REMOTE
            val label = track.outputLabel
            return when {
                label.contains("Bluetooth", ignoreCase = true) -> OutputKind.HEADPHONES
                label.contains("kulaklık", ignoreCase = true) -> OutputKind.HEADPHONES
                else -> OutputKind.PHONE
            }
        }
    }
}

sealed interface PlayerEvent {
    data object Unsupported : PlayerEvent
    data object Unreachable : PlayerEvent
}
