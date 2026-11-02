package com.erenkng.akis.wear.ui.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import com.erenkng.akis.wear.R
import com.erenkng.akis.wear.player.FocusGesture
import com.erenkng.akis.wear.player.FocusGestureTiming
import com.erenkng.akis.wear.player.PlayerScreen
import com.erenkng.akis.wear.player.PlayerUiState
import com.erenkng.akis.wear.player.VolumeDisplay
import com.erenkng.akis.wear.player.VolumeFeedback
import com.erenkng.akis.wear.ui.common.rememberClockText
import com.erenkng.akis.wear.ui.common.rememberHaptics
import com.erenkng.akis.wear.ui.nowplaying.ArtistLine
import com.erenkng.akis.wear.ui.nowplaying.ArtworkBackdrop
import com.erenkng.akis.wear.ui.nowplaying.MessageContent
import com.erenkng.akis.wear.ui.nowplaying.NowPlayingActions
import com.erenkng.akis.wear.ui.nowplaying.PercentLayout
import com.erenkng.akis.wear.ui.nowplaying.ShadowedText
import com.erenkng.akis.wear.ui.nowplaying.TrackTitle
import com.erenkng.akis.wear.ui.nowplaying.VolumeOverlay
import com.erenkng.akis.wear.ui.nowplaying.percentPosition
import com.erenkng.akis.wear.ui.nowplaying.rotaryVolume
import com.erenkng.akis.wear.ui.theme.AkisColors
import com.erenkng.akis.wear.ui.theme.AkisType
import com.erenkng.akis.wear.ui.theme.LocalAccent
import kotlinx.coroutines.delay

/** Kapak neredeyse olduğu gibi; yalnızca üstteki saat ve alttaki başlık için hafif karartma. */
private val FocusScrim = listOf(0f to 0.38f, 0.22f to 0f, 0.58f to 0f, 1f to 0.62f)

/**
 * Odak Modu (BRIEF 7.1, kullanıcının referans tasarımı): Şimdi Çalıyor'da aşağı kaydırınca açılır.
 * Tam ekran kapak, üstte vurgu renginde saat, altta şarkı adı ve uygulama ikonu + sanatçı. Buton yok:
 *
 * - tek dokunuş = oynat/duraklat · çift dokunuş = sonraki · uzun basış (600 ms) = önceki
 * - döner çerçeve = ses · yukarı kaydırma = Şimdi Çalıyor
 *
 * Kullanıcı ekrana bakmadığı için her jestin kendi titreşimi var; ekrana bakana kısa bir ikon geri bildirimi gösterilir.
 * TalkBack kullanıcıları için aynı eylemler erişilebilirlik eylemleri olarak sunulur.
 */
@Composable
fun FocusScreen(
    state: PlayerUiState,
    volume: VolumeDisplay?,
    isActivePage: Boolean,
    doubleTapEnabled: Boolean,
    actions: NowPlayingActions,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val accent = LocalAccent.current.accent
    var volumeTrigger by remember { mutableIntStateOf(0) }
    var flash by remember { mutableStateOf<FocusFlash?>(null) }
    val currentState by rememberUpdatedState(state)

    fun perform(gesture: FocusGesture) {
        val s = currentState
        if (s.track == null) return
        when (gesture) {
            FocusGesture.TAP -> if (s.canPlayPause) {
                if (s.isPlaying) haptics.pause() else haptics.play()
                flash = FocusFlash(if (s.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow)
                actions.onPlayPause()
            } else {
                haptics.error()
            }

            FocusGesture.DOUBLE_TAP -> if (s.canNext) {
                haptics.next()
                flash = FocusFlash(R.drawable.ic_skip_next)
                actions.onNext()
            } else {
                haptics.error()
            }

            FocusGesture.LONG_PRESS -> if (s.canPrevious) {
                haptics.previous()
                flash = FocusFlash(R.drawable.ic_skip_previous)
                actions.onPrevious()
            } else {
                haptics.error()
            }
        }
    }

    // pointerInput, ilk bestedeki lambdayı tutar; her zaman en güncel durumla çalışsın diye.
    val onGesture by rememberUpdatedState<(FocusGesture) -> Unit> { perform(it) }

    val playPauseLabel = stringResource(if (state.isPlaying) R.string.cd_pause else R.string.cd_play)
    val nextLabel = stringResource(R.string.cd_next)
    val previousLabel = stringResource(R.string.cd_previous)
    val description = stringResource(R.string.cd_focus_mode)

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
            }
            .focusGestures(doubleTapEnabled = doubleTapEnabled, onGesture = { onGesture(it) })
            .semantics {
                contentDescription = description
                customActions = listOf(
                    CustomAccessibilityAction(playPauseLabel) { perform(FocusGesture.TAP); true },
                    CustomAccessibilityAction(nextLabel) { perform(FocusGesture.DOUBLE_TAP); true },
                    CustomAccessibilityAction(previousLabel) { perform(FocusGesture.LONG_PRESS); true },
                )
            },
    ) {
        val diameter = maxWidth
        val track = state.track

        ArtworkBackdrop(
            artwork = state.artwork,
            appIcon = state.appIcon,
            scrim = FocusScrim,
            grayscale = state.screen == PlayerScreen.Disconnected,
            visible = track != null,
        )

        if (track == null || state.screen == PlayerScreen.Empty) {
            MessageContent(
                icon = R.drawable.ic_music_note,
                message = stringResource(R.string.empty_title),
                buttonLabel = stringResource(R.string.empty_resume),
                buttonIcon = R.drawable.ic_play_arrow,
                diameter = diameter,
                onClick = actions.onResumeLast,
            )
        } else {
            val clock = rememberClockText()
            val largeText = LocalDensity.current.fontScale > 1.15f
            PercentLayout(Modifier.fillMaxSize()) {
                ShadowedText(
                    text = clock,
                    style = AkisType.focusClock.copy(color = accent),
                    modifier = Modifier.percentPosition(0.5f, 0.095f),
                )
                TrackTitle(
                    title = track.title,
                    style = AkisType.focusTitle,
                    width = diameter * 0.74f,
                    largeText = largeText,
                    modifier = Modifier.percentPosition(0.5f, 0.75f),
                )
                ArtistLine(
                    artist = track.artist.ifBlank { track.appLabel },
                    appIcon = state.appIcon,
                    color = accent,
                    style = AkisType.focusArtist,
                    iconSize = 20.dp,
                    maxWidth = diameter * 0.66f,
                    modifier = Modifier.percentPosition(0.5f, 0.855f),
                )
            }
        }

        GestureFlashOverlay(flash = flash, onDone = { flash = null })
        VolumeOverlay(trigger = volumeTrigger, display = volume, accent = accent)
    }
}

/** Her jestte yeniden tetiklenebilsin diye kimlik taşıyan geri bildirim. */
private class FocusFlash(val icon: Int)

/** Jestten sonra ortada kısa süre görünen ikon (play/pause/next/prev). */
@Composable
private fun GestureFlashOverlay(flash: FocusFlash?, onDone: () -> Unit) {
    val current by rememberUpdatedState(onDone)
    LaunchedEffect(flash) {
        if (flash != null) {
            delay(FLASH_MS)
            current()
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = flash != null,
            enter = fadeIn(tween(120)) + scaleIn(tween(160), initialScale = 0.8f),
            exit = fadeOut(tween(250)),
        ) {
            val icon = flash?.icon ?: R.drawable.ic_play_arrow
            Box(
                Modifier
                    .size(64.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painter = painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
        }
    }
}

/**
 * Tek / çift dokunuş ve uzun basış algılayıcısı. Zamanlama [FocusGestureTiming] ile aynıdır.
 * Kaydırma (pager) başlarsa jest iptal edilir; dikey kaydırma Şimdi Çalıyor'a geçişte çalışmaya devam eder.
 */
private fun Modifier.focusGestures(doubleTapEnabled: Boolean, onGesture: (FocusGesture) -> Unit): Modifier =
    pointerInput(doubleTapEnabled) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var released = false
            val withinLongPress = withTimeoutOrNull(FocusGestureTiming.LONG_PRESS_MS) {
                released = waitForUpOrCancellation() != null
                true
            }
            if (withinLongPress == null) {
                onGesture(FocusGesture.LONG_PRESS)
                waitForUpOrCancellation()
                return@awaitEachGesture
            }
            if (!released) return@awaitEachGesture // kaydırma ya da iptal
            if (!doubleTapEnabled) {
                onGesture(FocusGesture.TAP)
                return@awaitEachGesture
            }
            val second = withTimeoutOrNull(FocusGestureTiming.DOUBLE_TAP_WINDOW_MS) { awaitFirstDown(requireUnconsumed = false) }
            if (second == null) {
                onGesture(FocusGesture.TAP)
            } else {
                waitForUpOrCancellation()
                onGesture(FocusGesture.DOUBLE_TAP)
            }
        }
    }

private const val FLASH_MS = 650L
