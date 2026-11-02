package com.erenkng.akis.shared

import com.erenkng.akis.shared.format.DurationFormat
import com.erenkng.akis.shared.model.MediaActions
import com.erenkng.akis.shared.model.NowPlayingState
import com.erenkng.akis.shared.model.PlaybackStatus
import com.erenkng.akis.shared.position.PositionCalculator
import com.erenkng.akis.shared.protocol.AkisPaths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PositionAndFormatTest {

    private val base = NowPlayingState(
        sessionId = "s",
        packageName = "p",
        appLabel = "A",
        durationMs = 200_000,
        positionMs = 10_000,
        positionUpdatedAtEpochMs = 1_000_000,
        speed = 1f,
        state = PlaybackStatus.PLAYING,
    )

    @Test
    fun advancesWhilePlaying() {
        assertEquals(15_000, PositionCalculator.positionAt(base, 1_005_000))
    }

    @Test
    fun respectsSpeed() {
        assertEquals(20_000, PositionCalculator.positionAt(base.copy(speed = 2f), 1_005_000))
    }

    @Test
    fun frozenWhenPaused() {
        assertEquals(10_000, PositionCalculator.positionAt(base.copy(state = PlaybackStatus.PAUSED), 1_900_000))
    }

    @Test
    fun clampedToDuration() {
        assertEquals(200_000, PositionCalculator.positionAt(base, 9_000_000))
    }

    @Test
    fun clockSkewNeverMovesBackwards() {
        assertEquals(10_000, PositionCalculator.positionAt(base, 999_000))
    }

    @Test
    fun liveStreamHasNoUpperBound() {
        val live = base.copy(durationMs = 0)
        assertEquals(510_000, PositionCalculator.positionAt(live, 1_500_000))
        assertEquals(0f, PositionCalculator.progress(live, 1_500_000))
        assertTrue(live.isLive)
    }

    @Test
    fun fiveMinuteDriftIsZeroWithoutUpdates() {
        // Kabul kriteri: 5 dk boyunca en fazla 1 sn sapma. Formül hızla doğrusal, kendi başına sapma üretmez.
        val now = base.positionUpdatedAtEpochMs + 300_000
        assertEquals(base.positionMs + 300_000, PositionCalculator.positionAt(base.copy(durationMs = 900_000), now))
    }

    @Test
    fun formatsDurations() {
        assertEquals("0:00", DurationFormat.format(0))
        assertEquals("1:12", DurationFormat.format(72_999))
        assertEquals("1:02:03", DurationFormat.format(3_723_000))
        assertEquals("1:12 · −2:33", DurationFormat.elapsedAndRemaining(72_000, 225_000))
    }

    @Test
    fun actionHelpers() {
        assertTrue(MediaActions.canPlay(0))
        assertTrue(MediaActions.canPlay(MediaActions.PLAY_PAUSE))
        assertFalse(MediaActions.canPlay(MediaActions.SKIP_TO_NEXT))
        assertTrue(MediaActions.canSkipNext(MediaActions.SKIP_TO_NEXT))
        assertFalse(MediaActions.canSeek(MediaActions.PLAY))
        // android.media.session.PlaybackState sabitleriyle aynı olmalı.
        assertEquals(256L, MediaActions.SEEK_TO)
        assertEquals(512L, MediaActions.PLAY_PAUSE)
        assertEquals(4_194_304L, MediaActions.SET_PLAYBACK_SPEED)
    }

    @Test
    fun iconPaths() {
        assertEquals("/akis/icon/com.example", AkisPaths.icon("com.example"))
        assertEquals("com.example", AkisPaths.packageFromIconPath("/akis/icon/com.example"))
        assertNull(AkisPaths.packageFromIconPath("/akis/state"))
        assertNull(AkisPaths.packageFromIconPath("/akis/icon/"))
    }
}
