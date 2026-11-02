package com.erenkng.akis.wear.ui.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
