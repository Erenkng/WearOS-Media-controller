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
import com.erenkng.akis.wear.ui.common.rememberClockText
import com.erenkng.akis.wear.ui.common.rememberHaptics
import com.erenkng.akis.wear.ui.theme.AkisColors
import com.erenkng.akis.wear.ui.theme.AkisType
import com.erenkng.akis.wear.ui.theme.LocalAccent

/** Şimdi Çalıyor ve Odak ekranlarından tetiklenen eylemler. Titreşimler ekranların içinde verilir. */
class NowPlayingActions(
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onVolumeSteps: (Int) -> VolumeFeedback,
    val onResumeLast: () -> Unit,
    val onRetry: () -> Unit,
    /** Desteklenmeyen eylem: "Bu uygulama desteklemiyor". */
    val onUnsupported: () -> Unit,
    /** Uygulamanın "beğen" özel eylemi. */
    val onLike: () -> Unit,
    /** Alt ortadaki buton: Kaynaklar paneline geçer. */
    val onOpenSources: () -> Unit,
)

/** Referans ekran çapı (BRIEF Bölüm 6): tüm ölçüler buna oranla ölçeklenir. */
internal val ReferenceDiameter = 225.dp

/**
 * Uygulamanın açılış ekranı. Yerleşim, kullanıcının referans tasarımına göre (ekran çapının yüzdesi):
 *
 * - y %8 saat · y %18 şarkı adı (büyük, kalın) · y %28,5 uygulama ikonu + sanatçı (vurgu rengi)
 * - y %50 önceki · oynat/duraklat (çevresinde ilerleme halkası) · sonraki
 * - y %75–83 beğen · Kaynaklar · çıkış (harici çıkışta vurgu rengi + alt çizgi)
 *
 * Döner çerçeve sesi değiştirir; gösterge kontrolleri kapatmaz (bkz. [VolumeOverlay]).
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

        ArtworkBackdrop(
            artwork = state.artwork,
            appIcon = state.appIcon,
            scrim = if (state.isPaused) PausedScrim else PlayerScrim,
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
                    onLike = {
                        haptics.play()
                        actions.onLike()
                    },
                    onOpenSources = actions.onOpenSources,
                    onOutputClick = { volumeTrigger++ },
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
    onLike: () -> Unit,
    onOpenSources: () -> Unit,
    onOutputClick: () -> Unit,
    onUnsupported: () -> Unit,
) {
    val accent = LocalAccent.current.accent
    val position = rememberPosition(track, isPlaying = state.isPlaying, active = isActivePage)
    val largeText = LocalDensity.current.fontScale > 1.15f
    val seekMode = state.transportMode == TransportMode.SEEK
    val clock = rememberClockText()

    PercentLayout(Modifier.fillMaxSize()) {
        ShadowedText(text = clock, style = AkisType.clock, modifier = Modifier.percentPosition(0.5f, 0.08f))

        TrackTitle(
            title = track.title,
            style = AkisType.trackTitle,
            width = diameter * 0.74f,
            largeText = largeText,
            modifier = Modifier.percentPosition(0.5f, if (largeText) 0.2f else 0.18f),
        )
        if (!largeText) {
            ArtistLine(
                artist = track.artist.ifBlank { track.appLabel },
                appIcon = state.appIcon,
                color = accent,
                style = AkisType.artist,
                iconSize = 16.dp,
                maxWidth = diameter * 0.70f,
                modifier = Modifier.percentPosition(0.5f, 0.285f),
            )
        }

        PlainIconButton(
            icon = if (seekMode) R.drawable.ic_fast_rewind else R.drawable.ic_skip_previous,
            contentDescription = stringResource(if (seekMode) R.string.cd_seek_back else R.string.cd_previous),
            iconSize = 38.dp * scale,
            enabled = state.canPrevious,
            onClick = onPrevious,
            onDisabledClick = onUnsupported,
            modifier = Modifier.percentPosition(0.18f, 0.5f),
        )
        PlayPauseRing(
            isPlaying = state.isPlaying,
            isBuffering = state.isBuffering,
            enabled = state.canPlayPause,
            progress = if (track.isLive) null else ({ position.value.toFloat() / track.durationMs }),
            accent = accent,
            size = 66.dp * scale,
            onClick = onPlayPause,
            onDisabledClick = onUnsupported,
            modifier = Modifier.percentPosition(0.5f, 0.5f),
        )
        PlainIconButton(
            icon = if (seekMode) R.drawable.ic_fast_forward else R.drawable.ic_skip_next,
            contentDescription = stringResource(if (seekMode) R.string.cd_seek_forward else R.string.cd_next),
            iconSize = 38.dp * scale,
            enabled = state.canNext,
            onClick = onNext,
            onDisabledClick = onUnsupported,
            modifier = Modifier.percentPosition(0.82f, 0.5f),
        )

        if (track.isLive) {
            ShadowedText(
                text = stringResource(R.string.live),
                style = AkisType.live.copy(color = accent),
                modifier = Modifier.percentPosition(0.5f, 0.655f),
            )
        }

        // Alt sıra, ekranın kavsini izler: kenarlar biraz yukarıda, orta biraz aşağıda.
        state.likeAction?.let { like ->
            PlainIconButton(
                icon = if (like.liked) R.drawable.ic_favorite_filled else R.drawable.ic_favorite,
                contentDescription = stringResource(if (like.liked) R.string.cd_unlike else R.string.cd_like),
                iconSize = 28.dp * scale,
                tint = if (like.liked) accent else AkisColors.OnSurface,
                onClick = onLike,
                modifier = Modifier.percentPosition(0.25f, 0.755f),
            )
        }
        PlainIconButton(
            icon = R.drawable.ic_library_music,
            contentDescription = stringResource(R.string.cd_sources),
            iconSize = 28.dp * scale,
            onClick = onOpenSources,
            modifier = Modifier.percentPosition(0.5f, 0.83f),
        )
        PlainIconButton(
            icon = outputIcon(state.outputKind, track.isVolumeControllable),
            contentDescription = if (track.isVolumeControllable) {
                stringResource(R.string.cd_volume, track.volume, track.volumeMax)
            } else {
                stringResource(R.string.cd_volume_locked)
            },
            iconSize = 28.dp * scale,
            tint = if (state.isExternalOutput) accent else AkisColors.OnSurface,
            underline = if (state.isExternalOutput) accent else null,
            onClick = onOutputClick,
            modifier = Modifier.percentPosition(0.75f, 0.755f),
        )
    }
}

internal fun outputIcon(kind: OutputKind, controllable: Boolean): Int = when {
    !controllable -> R.drawable.ic_lock
    kind == OutputKind.HEADPHONES -> R.drawable.ic_headphones
    kind == OutputKind.REMOTE -> R.drawable.ic_cast
    kind == OutputKind.WATCH -> R.drawable.ic_watch
    else -> R.drawable.ic_smartphone
}

/** Şimdi Çalıyor karartması: kapak görünür kalır, üstteki saat/başlık ve alt sıra okunur (referans tasarım). */
internal val PlayerScrim = listOf(0f to 0.62f, 0.32f to 0.42f, 0.68f to 0.42f, 1f to 0.66f)

/** Duraklatıldı: kapak biraz daha karanlık (BRIEF Bölüm 6). */
internal val PausedScrim = PlayerScrim.map { (stop, alpha) -> stop to (alpha + 0.15f).coerceAtMost(0.85f) }

/**
 * Kapak sanatı tam ekran arka plan + dikey karartma. Kapak yoksa uygulama ikonu büyütülüp
 * bulanıklaştırılır (BRIEF Bölüm 11, kenar durumlar).
 *
 * @param scrim (konum 0..1, siyah opaklığı) durakları.
 */
@Composable
internal fun ArtworkBackdrop(
    artwork: Bitmap?,
    appIcon: Bitmap?,
    scrim: List<Pair<Float, Float>>,
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
    if (scrim.isNotEmpty()) {
        val stops = remember(scrim) { scrim.map { (stop, alpha) -> stop to Color.Black.copy(alpha = alpha) }.toTypedArray() }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(*stops)))
    }
}

@Composable
internal fun MessageContent(
    icon: Int,
    message: String,
    buttonLabel: String,
    buttonIcon: Int,
    diameter: Dp,
    onClick: () -> Unit,
) {
    val accent = LocalAccent.current
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
            colors = ButtonDefaults.buttonColors(containerColor = accent.accent, contentColor = accent.onAccent),
            icon = {
                Icon(painter = painterResource(buttonIcon), contentDescription = null, modifier = Modifier.size(20.dp))
            },
            label = { Text(text = buttonLabel, style = AkisType.button.copy(color = accent.onAccent)) },
        )
    }
}
