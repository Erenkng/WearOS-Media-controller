package com.erenkng.akis.wear.ui.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.erenkng.akis.wear.ui.theme.AkisColors

/**
 * Kenardaki ilerleme halkası (BRIEF Bölüm 6): kenardan 2 dp içeride, 4 dp kalınlık,
 * saat 12'den saat yönünde. İz `onSurface` %15, dolgu vurgu rengi.
 *
 * Pozisyon State'i yalnızca çizim aşamasında okunur: her tikte recomposition değil, sadece yeniden çizim olur.
 */
@Composable
fun ProgressRing(
    position: State<Long>,
    durationMs: Long,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 4.dp,
    inset: Dp = 2.dp,
) {
    Canvas(modifier.fillMaxSize()) {
        val progress = if (durationMs > 0) (position.value.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
        drawRing(AkisColors.OnSurface.copy(alpha = 0.15f), 0f, 360f, strokeWidth, inset)
        if (progress > 0f) drawRing(color, -90f, 360f * progress, strokeWidth, inset)
    }
}

/** "Telefon bağlı değil" durumu: kırmızı ince halka (BRIEF Bölüm 6). */
@Composable
fun FullRing(color: Color, modifier: Modifier = Modifier, strokeWidth: Dp = 2.dp, inset: Dp = 2.dp) {
    Canvas(modifier.fillMaxSize()) { drawRing(color, 0f, 360f, strokeWidth, inset) }
}

internal fun DrawScope.drawRing(color: Color, startAngle: Float, sweep: Float, strokeWidth: Dp, inset: Dp) {
    val stroke = strokeWidth.toPx()
    val pad = inset.toPx() + stroke / 2f
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(pad, pad),
        size = Size(size.width - 2 * pad, size.height - 2 * pad),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
}
