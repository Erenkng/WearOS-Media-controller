package com.erenkng.akis.wear.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** BRIEF Bölüm 4, renk token'ları. */
object AkisColors {
    val Bg = Color(0xFF000000)
    val Surface = Color(0xFF141416)
    val SurfaceHigh = Color(0xFF202024)
    val OnSurface = Color(0xFFF2F0EB)
    val OnSurfaceDim = Color(0xFF9C9890)

    /** Kor turuncusu: kapaktan renk çıkmazsa vurgu. */
    val AccentFallback = Color(0xFFFF8A3D)
    val Danger = Color(0xFFFF5A5F)
}

/**
 * O anki vurgu rengi. Faz 1'de her zaman [AkisColors.AccentFallback].
 *
 * Faz 2 kancası: `accentDynamic` (androidx.palette, kontrast ≥ 4.5:1, H 130–150° yasak, 400 ms geçiş)
 * burada sağlanacak; tüketen bileşenler değişmeden kalır.
 */
@Immutable
data class AccentColors(val accent: Color, val onAccent: Color)

val LocalAccent = staticCompositionLocalOf { AccentColors(AkisColors.AccentFallback, AkisColors.Bg) }
