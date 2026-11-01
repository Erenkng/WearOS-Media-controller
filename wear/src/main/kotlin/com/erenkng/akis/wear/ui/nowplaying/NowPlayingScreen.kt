package com.erenkng.akis.wear.ui.nowplaying

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.erenkng.akis.shared.model.NowPlayingState
import com.erenkng.akis.wear.R
import com.erenkng.akis.wear.player.OutputKind
import com.erenkng.akis.wear.player.PlayerScreen
import com.erenkng.akis.wear.player.PlayerUiState
import com.erenkng.akis.wear.player.TransportMode
import com.erenkng.akis.wear.player.VolumeDisplay
import com.erenkng.akis.wear.player.VolumeFeedback
import com.erenkng.akis.wear.player.rememberPosition
import com.erenkng.akis.wear.ui.common.rememberHaptics
import com.erenkng.akis.wear.ui.theme.AkisColors
import com.erenkng.akis.wear.ui.theme.AkisType
import com.erenkng.akis.wear.ui.theme.LocalAccent

/** Şimdi Çalıyor ekranından tetiklenen eylemler. Titreşimler ekranın içinde verilir. */
class NowPlayingActions(
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onVolumeSteps: (Int) -> VolumeFeedback,
    val onResumeLast: () -> Unit,
    val onRetry: () -> Unit,
    /** Desteklenmeyen eylem: "Bu uygulama desteklemiyor". */
    val onUnsupported: () -> Unit,
    /** ⋯ menüsü Faz 2'de. */
    val onMore: () -> Unit,
)

/** Referans ekran çapı (BRIEF Bölüm 6): tüm ölçüler buna oranla ölçeklenir. */
private val ReferenceDiameter = 225.dp

/**
 * Uygulamanın açılış ekranı (BRIEF Bölüm 6). Wear OS'un 5 butonlu medya düzeni; konumlar ekran
 * çapının yüzdesi olarak [PercentLayout] ile yerleşir.
 */
@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    volume: VolumeDisplay?,
    isActivePage: Boolean,
    actions: NowPlayingActions,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    var volumeTrigger by remember { mutableIntStateOf(0) }
    val accent = LocalAccent.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AkisColors.Bg)
            .rotaryVolume(active = isActivePage) { steps ->
                when (actions.onVolumeSteps(steps)) {
                    VolumeFeedback.STEP -> haptics.volumeStep()
                    VolumeFeedback.LIMIT -> haptics.volumeLimit()
                    VolumeFeedback.NONE -> Unit
                }
                volumeTrigger++
            },
    ) {
        val diameter = maxWidth
        val scale = diameter / ReferenceDiameter
        val disconnected = state.screen == PlayerScreen.Disconnected

        Backdrop(
            artwork = state.artwork,
            appIcon = state.appIcon,
            dimmedForPause = state.isPaused,
            grayscale = disconnected,
            visible = state.track != null,
        )

        when (state.screen) {
            PlayerScreen.NowPlaying -> state.track?.let { track ->
                PlayerContent(
                    state = state,
                    track = track,
                    diameter = diameter,
                    scale = scale,
                    isActivePage = isActivePage,
                    onPlayPause = {
                        if (state.isPlaying) haptics.pause() else haptics.play()
                        actions.onPlayPause()
                    },
                    onNext = {
                        haptics.next()
                        actions.onNext()
                    },
                    onPrevious = {
                        haptics.previous()
                        actions.onPrevious()
                    },
                    onVolumeClick = { volumeTrigger++ },
                    onMore = actions.onMore,
                    onUnsupported = {
                        haptics.error()
                        actions.onUnsupported()
                    },
                )
            }

            PlayerScreen.Empty -> MessageContent(
                icon = R.drawable.ic_music_note,
                message = stringResource(R.string.empty_title),
                buttonLabel = stringResource(R.string.empty_resume),
                buttonIcon = R.drawable.ic_play_arrow,
                diameter = diameter,
                onClick = actions.onResumeLast,
            )

            PlayerScreen.Disconnected -> {
                FullRing(color = AkisColors.Danger)
                MessageContent(
                    icon = R.drawable.ic_bluetooth_disabled,
                    message = stringResource(R.string.disconnected_title),
                    buttonLabel = stringResource(R.string.disconnected_retry),
                    buttonIcon = R.drawable.ic_refresh,
                    diameter = diameter,
                    onClick = actions.onRetry,
                )
            }

            // Onboarding bu ekranın dışında gösterilir; Loading'de siyah zemin yeterli (< 700 ms).
            is PlayerScreen.Onboarding, PlayerScreen.Loading -> Unit
        }

        VolumeOverlay(trigger = volumeTrigger, display = volume, accent = accent.accent)
    }
}

@Composable
private fun PlayerContent(
    state: PlayerUiState,
    track: NowPlayingState,
    diameter: Dp,
    scale: Float,
    isActivePage: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onVolumeClick: () -> Unit,
    onMore: () -> Unit,
    onUnsupported: () -> Unit,
) {
    val accent = LocalAccent.current
    val position = rememberPosition(track, isPlaying = state.isPlaying, active = isActivePage)
    val largeText = LocalDensity.current.fontScale > 1.15f
    val seekMode = state.transportMode == TransportMode.SEEK

    PercentLayout(Modifier.fillMaxSize()) {
        // Konumsuz çocuklar tüm alanı kaplar.
        if (track.isLive) {
            CurvedBottomText(text = stringResource(R.string.live), style = AkisType.sourceLabel.copy(color = accent.accent))
        } else {
            ProgressRing(position = position, durationMs = track.durationMs, color = accent.accent)
            CurvedDuration(position = position, durationMs = track.durationMs)
        }

        SourceLabel(
            appLabel = track.appLabel,
            icon = state.appIcon,
            maxWidth = diameter * 0.6f,
            modifier = Modifier.percentPosition(0.5f, 0.20f),
        )
        TrackTitle(
            title = track.title,
            width = diameter * 0.70f,
            largeText = largeText,
            modifier = Modifier.percentPosition(0.5f, if (largeText) 0.34f else 0.32f),
        )
        if (!largeText) {
            ArtistText(
                artist = track.artist,
                width = diameter * 0.65f,
                modifier = Modifier.percentPosition(0.5f, 0.40f),
            )
        }

        CircleIconButton(
            icon = if (seekMode) R.drawable.ic_fast_rewind else R.drawable.ic_skip_previous,
            contentDescription = stringResource(if (seekMode) R.string.cd_seek_back else R.string.cd_previous),
            size = 44.dp * scale,
            enabled = state.canPrevious,
            onClick = onPrevious,
            onDisabledClick = onUnsupported,
            modifier = Modifier.percentPosition(0.22f, 0.55f),
        )
        PlayPauseButton(
            isPlaying = state.isPlaying,
            isBuffering = state.isBuffering,
            enabled = state.canPlayPause,
            accent = accent.accent,
            onAccent = accent.onAccent,
            size = 64.dp * scale,
            onClick = onPlayPause,
            onDisabledClick = onUnsupported,
            modifier = Modifier.percentPosition(0.5f, 0.55f),
        )
        CircleIconButton(
            icon = if (seekMode) R.drawable.ic_fast_forward else R.drawable.ic_skip_next,
            contentDescription = stringResource(if (seekMode) R.string.cd_seek_forward else R.string.cd_next),
            size = 44.dp * scale,
            enabled = state.canNext,
            onClick = onNext,
            onDisabledClick = onUnsupported,
            modifier = Modifier.percentPosition(0.78f, 0.55f),
        )

        CircleIconButton(
            icon = outputIcon(state.outputKind, track.isVolumeControllable),
            contentDescription = if (track.isVolumeControllable) {
                stringResource(R.string.cd_volume, track.volume, track.volumeMax)
            } else {
                stringResource(R.string.cd_volume_locked)
            },
            size = 36.dp * scale,
            enabled = true,
            onClick = onVolumeClick,
            onDisabledClick = onVolumeClick,
            modifier = Modifier.percentPosition(0.35f, 0.77f),
        )
        CircleIconButton(
            icon = R.drawable.ic_more_horiz,
            contentDescription = stringResource(R.string.cd_more),
            size = 36.dp * scale,
            // Faz 2 kancası: ⋯ menüsü (sıra, uyku zamanlayıcısı, karıştır/tekrarla, özel eylemler).
            enabled = false,
            onClick = onMore,
            onDisabledClick = onMore,
            modifier = Modifier.percentPosition(0.65f, 0.77f),
        )
    }
}

private fun outputIcon(kind: OutputKind, controllable: Boolean): Int = when {
    !controllable -> R.drawable.ic_lock
    kind == OutputKind.HEADPHONES -> R.drawable.ic_headphones
    kind == OutputKind.REMOTE -> R.drawable.ic_cast
    kind == OutputKind.WATCH -> R.drawable.ic_watch
    else -> R.drawable.ic_smartphone
}

/**
 * Kapak sanatı tam ekran arka plan + dikey karartma (üst %35, orta %55, alt %85).
 * Kapak yoksa uygulama ikonu büyütülüp bulanıklaştırılır (BRIEF Bölüm 11, kenar durumlar).
 */
@Composable
private fun Backdrop(
    artwork: Bitmap?,
    appIcon: Bitmap?,
    dimmedForPause: Boolean,
    grayscale: Boolean,
    visible: Boolean,
) {
    if (!visible) return
    val colorFilter = remember(grayscale) {
        if (grayscale) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null
    }
    Crossfade(targetState = artwork ?: appIcon, animationSpec = tween(300), label = "artwork") { bitmap ->
        if (bitmap != null) {
            val image = remember(bitmap) { bitmap.asImageBitmap() }
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = if (artwork == null) Modifier.fillMaxSize().blur(24.dp) else Modifier.fillMaxSize(),
            )
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.35f),
                    0.5f to Color.Black.copy(alpha = 0.55f),
                    1f to Color.Black.copy(alpha = 0.85f),
                ),
            ),
    )
    // Duraklatıldı: kapak %20 daha karanlık.
    if (dimmedForPause) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
}

@Composable
private fun MessageContent(
    icon: Int,
    message: String,
    buttonLabel: String,
    buttonIcon: Int,
    diameter: Dp,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = AkisColors.OnSurface,
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = message,
            style = AkisType.body,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(diameter * 0.72f),
        )
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = LocalAccent.current.accent,
                contentColor = LocalAccent.current.onAccent,
            ),
            icon = {
                Icon(painter = painterResource(buttonIcon), contentDescription = null, modifier = Modifier.size(20.dp))
            },
            label = { Text(text = buttonLabel, style = AkisType.button.copy(color = LocalAccent.current.onAccent)) },
        )
    }
}
