package com.erenkng.akis.wear.ui.nowplaying

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import kotlin.math.cos
import kotlin.math.sin

/** Görünen ikon küçük olsa bile dokunma alanı en az 48 dp (BRIEF Bölüm 11). */
private val MinTouchTarget = 48.dp

/** Desteklenmeyen eylem: %30 opaklık (BRIEF Bölüm 6). */
private const val DisabledAlpha = 0.3f

/**
 * Arka plansız, beyaz ikon butonu (referans tasarım): önceki/sonraki ve alt sıradaki butonlar.
 * Etkin değilse yine tıklanabilir; [onDisabledClick] titreşim + "Bu uygulama desteklemiyor" gösterir.
 *
 * @param underline doluysa ikonun altında bu renkte kısa bir çizgi (ör. harici çıkışa bağlı).
 */
@Composable
fun PlainIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = AkisColors.OnSurface,
    underline: Color? = null,
    onDisabledClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .size(max(iconSize + 12.dp, MinTouchTarget))
            .clip(CircleShape)
            .clickable(role = Role.Button) { if (enabled) onClick() else onDisabledClick() }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(iconSize)
                    .alpha(if (enabled) 1f else DisabledAlpha),
            )
            if (underline != null) {
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .width(iconSize * 0.9f)
                        .height(2.5.dp)
                        .background(underline, RoundedCornerShape(50)),
                )
            }
        }
    }
}

/**
 * Oynat/duraklat, çevresinde parça ilerlemesi (referans tasarım): gri iz, vurgu renginde ilerleme ve
 * mevcut konumda beyaz bir nokta. Canlı yayında ([progress] null) yalnızca iz çizilir.
 * İlerleme yalnızca çizim aşamasında okunur: her tikte recomposition değil, sadece yeniden çizim olur.
 */
@Composable
fun PlayPauseRing(
    isPlaying: Boolean,
    isBuffering: Boolean,
    enabled: Boolean,
    progress: (() -> Float)?,
    accent: Color,
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
    val spinner = rememberInfiniteTransition(label = "buffering")
    val spinnerAngle by spinner.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1100, easing = LinearEasing), RepeatMode.Restart),
        label = "spinnerAngle",
    )

    Box(
        modifier = modifier
            .size(max(size, MinTouchTarget))
            .clip(CircleShape)
            .clickable(role = Role.Button) { if (enabled) onClick() else onDisabledClick() }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val stroke = RING_STROKE.toPx()
            // Konum noktası halkanın dışına taşmasın diye halka, nokta yarıçapı kadar içeriden çizilir.
            val inset = THUMB_RADIUS.toPx()
            val arcSize = Size(this.size.width - 2 * inset, this.size.height - 2 * inset)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = Color.White.copy(alpha = 0.25f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (isBuffering) {
                drawArc(accent, spinnerAngle, 80f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            } else if (progress != null) {
                val fraction = progress().coerceIn(0f, 1f)
                if (fraction > 0f) {
                    drawArc(accent, -90f, 360f * fraction, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                val angle = Math.toRadians((-90.0 + 360.0 * fraction))
                val radius = arcSize.width / 2f
                drawCircle(
                    color = Color.White,
                    radius = THUMB_RADIUS.toPx(),
                    center = Offset(
                        x = center.x + (radius * cos(angle)).toFloat(),
                        y = center.y + (radius * sin(angle)).toFloat(),
                    ),
                )
            }
        }
        Icon(
            painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(size * 0.46f)
                .alpha(if (enabled) 1f else DisabledAlpha),
        )
    }
}

/** Halka genişliği ve konum noktası (referans tasarımdaki oranlar). */
private val RING_STROKE = 3.5.dp
private val THUMB_RADIUS = 4.5.dp
