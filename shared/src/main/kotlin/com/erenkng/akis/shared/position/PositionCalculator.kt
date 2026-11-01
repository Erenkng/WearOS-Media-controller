package com.erenkng.akis.shared.position

import com.erenkng.akis.shared.model.NowPlayingState
import com.erenkng.akis.shared.model.PlaybackStatus

/**
 * Pozisyon saatte hesaplanır, telefondan her saniye istenmez (BRIEF Bölüm 10):
 *
 * `pos = positionMs + (now − positionUpdatedAtEpochMs) × speed`
 *
 * Yalnızca PLAYING iken ilerler ve `durationMs` ile sınırlanır.
 */
object PositionCalculator {

    fun positionAt(state: NowPlayingState, nowEpochMs: Long): Long = positionAt(
        positionMs = state.positionMs,
        positionUpdatedAtEpochMs = state.positionUpdatedAtEpochMs,
        speed = state.speed,
        playing = state.state == PlaybackStatus.PLAYING,
        durationMs = state.durationMs,
        nowEpochMs = nowEpochMs,
    )

    fun positionAt(
        positionMs: Long,
        positionUpdatedAtEpochMs: Long,
        speed: Float,
        playing: Boolean,
        durationMs: Long,
        nowEpochMs: Long,
    ): Long {
        var position = positionMs
        if (playing && positionUpdatedAtEpochMs > 0) {
            // Saatlerin küçük farkı geriye gitmeye yol açmasın.
            val elapsed = (nowEpochMs - positionUpdatedAtEpochMs).coerceAtLeast(0)
            position += (elapsed * speed.toDouble()).toLong()
        }
        position = position.coerceAtLeast(0)
        if (durationMs > 0) position = position.coerceAtMost(durationMs)
        return position
    }

    /** 0f..1f; süre bilinmiyorsa 0f. */
    fun progress(state: NowPlayingState, nowEpochMs: Long): Float {
        if (state.durationMs <= 0) return 0f
        return (positionAt(state, nowEpochMs).toFloat() / state.durationMs).coerceIn(0f, 1f)
    }
}
