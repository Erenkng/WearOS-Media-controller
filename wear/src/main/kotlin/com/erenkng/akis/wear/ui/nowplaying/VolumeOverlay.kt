package com.erenkng.akis.wear.ui.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.erenkng.akis.wear.R
import com.erenkng.akis.wear.player.VolumeDisplay
import com.erenkng.akis.wear.ui.theme.AkisColors
import kotlinx.coroutines.delay

/**
 * Geçici ses göstergesi (BRIEF Bölüm 6 ve 8): yalnızca çevirme sırasında görünür.
 * Sağ kenarda 60°'lik kavisli çubuk + ortada büyük "8/15". 150 ms giriş, 1,5 sn bekleme, 300 ms çıkış.
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
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                // 0° = saat 3 yönü. Çubuk -30°..+30° arasında, alttan yukarı dolar.
                drawRing(AkisColors.OnSurface.copy(alpha = 0.2f), -ARC_HALF, 2 * ARC_HALF, 6.dp, 2.dp)
                if (!value.isLocked && value.max > 0) {
                    val fraction = value.volume.toFloat() / value.max
                    if (fraction > 0f) drawRing(accent, ARC_HALF, -2 * ARC_HALF * fraction, 6.dp, 2.dp)
                }
            }
            val description = if (value.isLocked) {
                stringResource(R.string.cd_volume_locked)
            } else {
                stringResource(R.string.cd_volume, value.volume, value.max)
            }
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color.Black.copy(alpha = 0.72f), CircleShape)
                    .semantics {
                        contentDescription = description
                        liveRegion = LiveRegionMode.Polite
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (value.isLocked) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock),
                        contentDescription = null,
                        tint = AkisColors.OnSurface,
                        modifier = Modifier.size(32.dp),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.volume_value, value.volume, value.max),
                        style = com.erenkng.akis.wear.ui.theme.AkisType.volumeNumber,
                    )
                }
            }
        }
    }
}

private const val ARC_HALF = 30f
private const val ENTER_MS = 150L
private const val HOLD_MS = 1_500L
private const val EXIT_MS = 300
