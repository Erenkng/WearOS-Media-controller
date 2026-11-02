package com.erenkng.akis.wear.player

import kotlin.math.sign

/**
 * Döner çerçeve/kron piksellerini tam "tık"lara çevirir (BRIEF Bölüm 8: her tık bir ses adımı).
 *
 * Galaxy Watch çerçevesi her tıkta sabit bir piksel miktarı üretir; kron (Pixel Watch) ise küçük
 * sürekli değerler. Kalan kısım biriktirilir; yön değişince sıfırlanır ki geri dönüş gecikmesin.
 *
 * Pozitif adım = saat yönü = ses artışı.
 */
class RotaryStepper(private val pixelsPerStep: Float) {

    init {
        require(pixelsPerStep > 0f) { "pixelsPerStep > 0 olmalı" }
    }

    private var remainder = 0f

    fun onScroll(pixels: Float): Int {
        if (pixels == 0f) return 0
        if (remainder != 0f && sign(pixels) != sign(remainder)) remainder = 0f
        remainder += pixels
        val steps = (remainder / pixelsPerStep).toInt()
        remainder -= steps * pixelsPerStep
        return steps
    }

    fun reset() {
        remainder = 0f
    }
}
