package com.erenkng.akis.wear.ui.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

/**
 * BRIEF Bölüm 6'daki yerleşim tablosu, konumları ekran çapının yüzdesi olarak verir
 * ("y %55, x %22"). Bu düzen her çocuğun **merkezini** o noktaya koyar; böylece ölçüler
 * 225 dp'lik referanstan farklı ekranlara (40 mm, Pixel Watch) orantılı taşınır.
 * Konum verilmeyen çocuklar (arka plan, halka) tüm alanı kaplar.
 */
@Composable
fun PercentLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placed = measurables.map { measurable ->
            val position = measurable.parentData as? PercentPosition
            val placeable = if (position == null) {
                measurable.measure(constraints.copy(minWidth = width, minHeight = height))
            } else {
                measurable.measure(loose)
            }
            placeable to position
        }
        layout(width, height) {
            placed.forEach { (placeable, position) ->
                if (position == null) {
                    placeable.place(0, 0)
                } else {
                    placeable.place(
                        x = (position.x * width - placeable.width / 2f).roundToInt(),
                        y = (position.y * height - placeable.height / 2f).roundToInt(),
                    )
                }
            }
        }
    }
}

@Immutable
private data class PercentPosition(val x: Float, val y: Float) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = this@PercentPosition
}

/** @param x 0..1, ekran genişliğine oranla merkez. @param y 0..1, ekran yüksekliğine oranla merkez. */
fun Modifier.percentPosition(x: Float, y: Float): Modifier = this.then(PercentPosition(x, y))
