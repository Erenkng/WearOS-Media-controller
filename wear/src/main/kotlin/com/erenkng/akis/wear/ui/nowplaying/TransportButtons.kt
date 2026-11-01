package com.erenkng.akis.wear.ui.nowplaying

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.wear.compose.material3.Icon
import com.erenkng.akis.wear.R
import com.erenkng.akis.wear.ui.theme.AkisColors

/** Görünen buton küçük olsa bile dokunma alanı en az 48 dp (BRIEF Bölüm 11). */
private val MinTouchTarget = 48.dp

/** Desteklenmeyen eylem: %30 opaklık (BRIEF Bölüm 6, "Butonlar uygulamaya göre"). */
private const val DisabledAlpha = 0.3f

/**
 * Daire yan buton: önceki/sonraki (44 dp, `surface` %70), ses/⋯ (36 dp).
 * Etkin değilse yine tıklanabilir; [onDisabledClick] titreşim + "Bu uygulama desteklemiyor" gösterir.
 */
@Composable
fun CircleIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
    onDisabledClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = AkisColors.Surface.copy(alpha = 0.7f),
    iconTint: Color = AkisColors.OnSurface,
) {
    Box(
        modifier = modifier
            .size(max(size, MinTouchTarget))
            .clip(CircleShape)
            .clickable(role = Role.Button) { if (enabled) onClick() else onDisabledClick() }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .alpha(if (enabled) 1f else DisabledAlpha)
                .clip(CircleShape)
                .background(background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

/**
 * Oynat/duraklat, 64 dp, vurgu dolgulu, ikon siyah (BRIEF Bölüm 6).
 * Faz 1: her zaman daire. Faz 2 kancası: çalarken yuvarlatılmış kare (%30), 250 ms spring şekil değişimi.
 */
@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    enabled: Boolean,
    accent: Color,
    onAccent: Color,
    size: Dp,
    onClick: () -> Unit,
    onDisabledClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        when {
            isBuffering -> R.string.cd_buffering
            isPlaying -> R.string.cd_pause
            else -> R.string.cd_play
        },
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircleIconButton(
            icon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
            contentDescription = description,
            size = size,
            enabled = enabled,
            onClick = onClick,
            onDisabledClick = onDisabledClick,
            background = accent,
            iconTint = onAccent,
        )
        if (isBuffering) BufferingSpinner(color = onAccent, modifier = Modifier.size(size).padding(4.dp))
    }
}

/** Arabelleğe alırken buton içinde dönen ince halka; diğer butonlar etkin kalır. */
@Composable
private fun BufferingSpinner(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "buffering")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )
    Canvas(modifier.fillMaxSize()) {
        drawArc(
            color = color.copy(alpha = 0.6f),
            startAngle = rotation,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}
