package com.erenkng.akis.bridge.sync

import com.erenkng.akis.shared.model.NowPlayingState
import com.erenkng.akis.shared.model.SubscribeLevel
import com.erenkng.akis.shared.position.PositionCalculator
import kotlin.math.abs

/**
 * `/akis/state` ne zaman gönderilir (BRIEF Bölüm 10 ve 11):
 *
 * - Parça/oynatma durumu değişti → her abonelik seviyesinde hemen gönder (tile güncel kalsın).
 * - Ekran kapalı / ambient (NONE, TRACK_ONLY) → başka hiçbir şey gönderilmez.
 * - Ekran açık (FULL):
 *   - ses, çıkış, hız, özel eylemler gibi alanlar değişti → gönder;
 *   - pozisyon, saatin tahmininden [SEEK_TOLERANCE_MS]'den fazla saptı (sarma) → gönder;
 *   - yalnızca pozisyon ilerledi → en fazla [POSITION_REFRESH_MS]'de bir gönder.
 */
object PublishPolicy {
    const val POSITION_REFRESH_MS = 15_000L
    const val SEEK_TOLERANCE_MS = 2_000L

    fun shouldPublish(
        lastPublished: NowPlayingState?,
        lastPublishedAtMs: Long,
        next: NowPlayingState?,
        nowMs: Long,
        level: SubscribeLevel,
        force: Boolean = false,
    ): Boolean {
        if (force) return true
        if (lastPublished == null || next == null) return lastPublished != next
        if (lastPublished.trackKey != next.trackKey) return true
        if (level != SubscribeLevel.FULL) return false

        val otherFieldsChanged = lastPublished.copy(
            positionMs = next.positionMs,
            positionUpdatedAtEpochMs = next.positionUpdatedAtEpochMs,
        ) != next
        if (otherFieldsChanged) return true
        if (lastPublished == next) return false

        val referenceTime = next.positionUpdatedAtEpochMs.takeIf { it > 0 } ?: nowMs
        val projected = PositionCalculator.positionAt(lastPublished, referenceTime)
        if (abs(projected - next.positionMs) > SEEK_TOLERANCE_MS) return true

        return nowMs - lastPublishedAtMs >= POSITION_REFRESH_MS
    }
}
