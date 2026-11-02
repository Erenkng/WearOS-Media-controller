package com.erenkng.akis.wear.ui.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.erenkng.akis.wear.R
import com.erenkng.akis.wear.player.VolumeDisplay
import com.erenkng.akis.wear.ui.theme.AkisColors
import com.erenkng.akis.wear.ui.theme.AkisType
import kotlinx.coroutines.delay

/**
 * Geçici ses göstergesi; yalnızca çevirme sırasında görünür (BRIEF Bölüm 6 ve 8).
 *
 * Kontrolleri kapatmaz: sağ kenarda 60°'lik bir yay ve saatin yerinde küçük bir hap ("🔊 8/15").
 * 150 ms giriş, 1,5 sn bekleme, 300 ms çıkış.
 *
 * @param trigger her ses adımında artan sayaç; değiştikçe gösterge (yeniden) açılır.
 */
@Composable
fun VolumeOverlay(trigger: Int, display: VolumeDisplay?, accent: Color, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        visible = true
        delay(ENTER_MS + HOLD_MS)
        visible = false
    }

    AnimatedVisibility(
        visible = visible && display != null,
        enter = fadeIn(tween(ENTER_MS.toInt())),
        exit = fadeOut(tween(EXIT_MS)),
        modifier = modifier.fillMaxSize(),
    ) {
        val value = display ?: return@AnimatedVisibility
        val description = if (value.isLocked) {
            stringResource(R.string.cd_volume_locked)
        } else {
            stringResource(R.string.cd_volume, value.volume, value.max)
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                // 0° = saat 3 yönü. Yay -30°..+30° arasında, alttan yukarı dolar.
                drawRing(AkisColors.OnSurface.copy(alpha = 0.22f), -ARC_HALF, 2 * ARC_HALF, 6.dp, 3.dp)
                if (!value.isLocked && value.max > 0) {
                    val fraction = value.volume.toFloat() / value.max
                    if (fraction > 0f) drawRing(accent, ARC_HALF, -2 * ARC_HALF * fraction, 6.dp, 3.dp)
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxHeight * 0.035f)
                    .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .semantics {
                        contentDescription = description
                        liveRegion = LiveRegionMode.Polite
                    },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(if (value.isLocked) R.drawable.ic_lock else R.drawable.ic_volume_up),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
                if (!value.isLocked) {
                    Text(text = stringResource(R.string.volume_value, value.volume, value.max), style = AkisType.volumeValue)
                }
            }
        }
    }
}

private const val ARC_HALF = 30f
private const val ENTER_MS = 150L
private const val HOLD_MS = 1_500L
private const val EXIT_MS = 300
