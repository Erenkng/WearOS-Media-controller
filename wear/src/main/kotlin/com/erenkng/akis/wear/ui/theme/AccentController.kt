package com.erenkng.akis.wear.ui.theme

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Kapaktan vurgu rengi (BRIEF Bölüm 4): Palette'ten sırasıyla vibrant → lightVibrant → dominant denenir,
 * [ColorRules] ile kontrast ve yasaklı ton kuralları uygulanır. Uygun renk yoksa Kor turuncusu.
 * Şarkı değişince renk 400 ms'de geçer; ekran "yanıp sönmez".
 *
 * @param artKey kapak kimliği (`artHash`); aynı kapak için yeniden hesaplanmaz.
 */
@Composable
fun rememberArtworkAccent(artwork: Bitmap?, artKey: String?): AccentColors {
    val extracted by produceState<Color?>(initialValue = null, artKey, artwork) {
        value = if (artwork == null) {
            null
        } else {
            withContext(Dispatchers.Default) { extractAccent(artwork) }?.let { Color(it) }
        }
    }
    val animated by animateColorAsState(
        targetValue = extracted ?: AkisColors.AccentFallback,
        animationSpec = tween(durationMillis = ACCENT_TRANSITION_MS),
        label = "accent",
    )
    return AccentColors(accent = animated, onAccent = AkisColors.Bg)
}

private fun extractAccent(artwork: Bitmap): Int? = runCatching {
    val palette = Palette.from(artwork).maximumColorCount(16).generate()
    listOfNotNull(palette.vibrantSwatch, palette.lightVibrantSwatch, palette.dominantSwatch)
        .firstNotNullOfOrNull { swatch -> ColorRules.accentFrom(swatch.rgb) }
}.getOrNull()

private const val ACCENT_TRANSITION_MS = 400
