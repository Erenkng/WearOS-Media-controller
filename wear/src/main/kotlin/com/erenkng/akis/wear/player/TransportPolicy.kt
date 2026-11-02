package com.erenkng.akis.wear.player

import com.erenkng.akis.shared.model.MediaActions

enum class TransportMode {
    /** Önceki / sonraki parça. */
    SKIP,

    /** −15 sn / +30 sn (podcast, sesli kitap). */
    SEEK,
}

/** Yan butonların ne yapacağı (BRIEF Bölüm 6, "Duruma göre görünüm"). */
object TransportPolicy {
    const val SEEK_BACK_MS = -15_000L
    const val SEEK_FORWARD_MS = 30_000L

    /** Uygulama atlamayı desteklemiyor ama sarmayı destekliyorsa −15/+30. */
    fun mode(actions: Long): TransportMode {
        val canSkip = MediaActions.canSkipNext(actions) || MediaActions.canSkipPrevious(actions)
        return if (!canSkip && MediaActions.canSeek(actions)) TransportMode.SEEK else TransportMode.SKIP
    }
}
