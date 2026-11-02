package com.erenkng.akis.wear.player

/** Odak Modu jestleri (BRIEF 7.1): bakmadan kontrol. */
enum class FocusGesture {
    /** Oynat / duraklat. */
    TAP,

    /** Sonraki parça. */
    DOUBLE_TAP,

    /** Önceki parça. */
    LONG_PRESS,
}

/**
 * Jest zamanlaması, saf ve test edilebilir. Dokunma algılayıcısı aynı eşikleri kullanır.
 *
 * - Basılı tutma [LONG_PRESS_MS]'e ulaşırsa uzun basış.
 * - Bırakıldıktan sonra [DOUBLE_TAP_WINDOW_MS] içinde ikinci dokunuş gelirse çift dokunuş.
 * - "Çift dokunuşu kapat" ayarında tek dokunuş beklemeden anında çalışır.
 */
object FocusGestureTiming {
    const val DOUBLE_TAP_WINDOW_MS = 250L
    const val LONG_PRESS_MS = 600L

    /**
     * @param pressDurationMs ilk basışın süresi.
     * @param gapToSecondDownMs bırakmadan ikinci basışa kadar geçen süre; ikinci basış yoksa null.
     */
    fun classify(pressDurationMs: Long, gapToSecondDownMs: Long?, doubleTapEnabled: Boolean): FocusGesture = when {
        pressDurationMs >= LONG_PRESS_MS -> FocusGesture.LONG_PRESS
        doubleTapEnabled && gapToSecondDownMs != null && gapToSecondDownMs <= DOUBLE_TAP_WINDOW_MS -> FocusGesture.DOUBLE_TAP
        else -> FocusGesture.TAP
    }
}
