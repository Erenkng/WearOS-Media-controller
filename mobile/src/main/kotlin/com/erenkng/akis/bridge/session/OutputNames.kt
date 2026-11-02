package com.erenkng.akis.bridge.session

/**
 * Bluetooth cihaz adındaki sahiplik ön ekini atar: Samsung cihazları kendini
 * "Eren Doğu adlı kişiye ait Buds3 FE" ya da "Eren's Buds3 FE" diye adlandırır. Saatte yalnızca
 * "Buds3 FE" görünür. Saf fonksiyon.
 */
object OutputNames {
    private val PATTERNS = listOf(
        Regex("""^.+?\s+adlı\s+kişiye\s+ait\s+(.+)$""", RegexOption.IGNORE_CASE),
        Regex("""^.+?['’]s\s+(.+)$"""),
        Regex("""^.+?['’](?:in|ın|un|ün|nin|nın|nun|nün)\s+(.+)$"""),
    )

    fun shorten(name: String): String {
        val trimmed = name.trim()
        for (pattern in PATTERNS) {
            val rest = pattern.find(trimmed)?.groupValues?.get(1)?.trim()
            if (!rest.isNullOrEmpty()) return rest
        }
        return trimmed
    }
}
