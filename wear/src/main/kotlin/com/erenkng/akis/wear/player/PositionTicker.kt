package com.erenkng.akis.wear.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.erenkng.akis.shared.model.NowPlayingState
import com.erenkng.akis.shared.position.PositionCalculator
import kotlinx.coroutines.delay

/**
 * Pozisyon, ayrı bir `State` olarak yayılır (BRIEF Bölüm 11: recomposition sadece değişen öğede).
 * Yalnızca bu State'i okuyan halka ve süre metni yeniden çizilir; ekranın geri kalanı etkilenmez.
 *
 * Değer telefondan istenmez, [PositionCalculator] ile saatte hesaplanır. Çalmıyorken ya da ekran
 * pasifken döngü durur.
 */
@Composable
fun rememberPosition(track: NowPlayingState?, isPlaying: Boolean, active: Boolean): State<Long> {
    val initial = track?.let { PositionCalculator.positionAt(it, System.currentTimeMillis()) } ?: 0L
    return produceState(initialValue = initial, track, isPlaying, active) {
        if (track == null) {
            value = 0L
            return@produceState
        }
        value = PositionCalculator.positionAt(track, System.currentTimeMillis())
        while (active && isPlaying && track.isPlaying) {
            delay(TICK_MS)
            value = PositionCalculator.positionAt(track, System.currentTimeMillis())
        }
    }
}

private const val TICK_MS = 250L
