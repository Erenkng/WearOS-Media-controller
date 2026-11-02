package com.erenkng.akis.wear.ui.theme

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Kapaktan çıkan vurgu renginin kuralları (BRIEF Bölüm 4, "Dinamik renk kuralı"). Saf Kotlin, ARGB Int.
 *
 * - Siyah üzerinde en az [MIN_CONTRAST] (4.5:1) kontrast: sağlanmıyorsa HSL açıklığı artırılır (L ≥ 0.55).
 * - H 130–150° (yasaklı yeşil tonlar) düşerse ton 20° kaydırılır.
 * - Neredeyse gri renkler vurgu olamaz; o zaman sabit vurguya dönülür.
 */
object ColorRules {
    const val MIN_CONTRAST = 4.5
    const val MIN_SATURATION = 0.12f
    private const val BANNED_HUE_START = 130f
    private const val BANNED_HUE_END = 150f
    private const val HUE_SHIFT = 20f
    private const val MIN_LIGHTNESS_WHEN_RAISED = 0.55f
    private const val LIGHTNESS_STEP = 0.02f
    private const val MAX_HUE_ATTEMPTS = 3
    private const val BOUNDARY_MARGIN = 3f

    /** @return siyah üzerinde kullanılabilir vurgu; renk uygun değilse null (sabit vurgu kullanılır). */
    fun accentFrom(argb: Int): Int? {
        val (hue, saturation, lightness) = rgbToHsl(argb)
        if (saturation < MIN_SATURATION) return null
        var targetHue = avoidBannedHue(hue)
        repeat(MAX_HUE_ATTEMPTS) {
            val color = withContrast(targetHue, saturation, lightness)
            // 8 bitlik yuvarlama, sınırdaki bir tonu yasaklı aralığa itebilir: son rengin tonu yeniden denetlenir.
            val actualHue = rgbToHsl(color).first
            if (!isBannedHue(actualHue)) return color
            targetHue = if (actualHue >= (BANNED_HUE_START + BANNED_HUE_END) / 2f) {
                BANNED_HUE_END + BOUNDARY_MARGIN * (it + 1)
            } else {
                BANNED_HUE_START - BOUNDARY_MARGIN * (it + 1)
            }
        }
        return null
    }

    /** Siyah üzerinde [MIN_CONTRAST] sağlanana kadar açıklığı artırır (en az 0.55). */
    private fun withContrast(hue: Float, saturation: Float, lightness: Float): Int {
        var l = lightness
        var color = hslToRgb(hue, saturation, l)
        if (contrastOnBlack(color) < MIN_CONTRAST) {
            l = max(l, MIN_LIGHTNESS_WHEN_RAISED)
            color = hslToRgb(hue, saturation, l)
        }
        while (contrastOnBlack(color) < MIN_CONTRAST && l < 1f) {
            l = min(1f, l + LIGHTNESS_STEP)
            color = hslToRgb(hue, saturation, l)
        }
        return color
    }

    fun avoidBannedHue(hue: Float): Float = when {
        hue < BANNED_HUE_START || hue > BANNED_HUE_END -> hue
        hue >= (BANNED_HUE_START + BANNED_HUE_END) / 2f -> hue + HUE_SHIFT
        else -> hue - HUE_SHIFT
    }

    fun isBannedHue(hue: Float): Boolean = hue in BANNED_HUE_START..BANNED_HUE_END

    /** WCAG 2.x göreli parlaklığa göre siyah (#000) üzerindeki kontrast oranı. */
    fun contrastOnBlack(argb: Int): Double = (relativeLuminance(argb) + 0.05) / 0.05

    fun relativeLuminance(argb: Int): Double {
        fun channel(value: Int): Double {
            val c = value / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val r = channel((argb shr 16) and 0xFF)
        val g = channel((argb shr 8) and 0xFF)
        val b = channel(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** @return (ton 0..360, doygunluk 0..1, açıklık 0..1) */
    fun rgbToHsl(argb: Int): Triple<Float, Float, Float> {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val delta = maxC - minC
        val l = (maxC + minC) / 2f
        if (delta == 0f) return Triple(0f, 0f, l)
        val s = delta / (1f - abs(2f * l - 1f))
        val h = when (maxC) {
            r -> 60f * (((g - b) / delta) % 6f)
            g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        return Triple((h + 360f) % 360f, s.coerceIn(0f, 1f), l)
    }

    fun hslToRgb(hue: Float, saturation: Float, lightness: Float): Int {
        val h = ((hue % 360f) + 360f) % 360f
        val c = (1f - abs(2f * lightness - 1f)) * saturation
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = lightness - c / 2f
        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun to255(v: Float) = ((v + m) * 255f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (to255(r) shl 16) or (to255(g) shl 8) or to255(b)
    }
}
