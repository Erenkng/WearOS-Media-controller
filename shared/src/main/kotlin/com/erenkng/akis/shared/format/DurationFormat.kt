package com.erenkng.akis.shared.format

object DurationFormat {
    /** Tipografik eksi (U+2212); kısa çizgiden daha iyi okunur. */
    const val MINUS = "−"

    /** 72_000 → "1:12", 3_723_000 → "1:02:03". */
    fun format(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0) / 1000)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "$hours:${minutes.pad()}:${seconds.pad()}"
        } else {
            "$minutes:${seconds.pad()}"
        }
    }

    /** "1:12 · −2:33" (BRIEF Bölüm 6, alt kavis). */
    fun elapsedAndRemaining(positionMs: Long, durationMs: Long): String {
        val remaining = (durationMs - positionMs).coerceAtLeast(0)
        return "${format(positionMs)} · $MINUS${format(remaining)}"
    }

    private fun Long.pad(): String = toString().padStart(2, '0')
}
