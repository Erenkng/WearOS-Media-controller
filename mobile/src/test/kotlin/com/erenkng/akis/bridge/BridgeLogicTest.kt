package com.erenkng.akis.bridge

import com.erenkng.akis.bridge.command.AckCache
import com.erenkng.akis.bridge.session.FocusCandidate
import com.erenkng.akis.bridge.session.FocusPolicy
import com.erenkng.akis.bridge.session.RecentApp
import com.erenkng.akis.bridge.session.RecentAppsList
import com.erenkng.akis.bridge.sync.PublishPolicy
import com.erenkng.akis.shared.model.CommandAck
import com.erenkng.akis.shared.model.NowPlayingState
import com.erenkng.akis.shared.model.PlaybackStatus
import com.erenkng.akis.shared.model.SubscribeLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FocusPolicyTest {
    private val music = FocusCandidate("music/1", isPlaying = false)
    private val youtube = FocusCandidate("video/2", isPlaying = true)
    private val podcast = FocusCandidate("pod/3", isPlaying = true)

    @Test
    fun userSelectionWins() {
        assertEquals("music/1", FocusPolicy.choose(listOf(music, youtube), "music/1", null))
    }

    @Test
    fun firstPlayingInPriorityOrder() {
        assertEquals("video/2", FocusPolicy.choose(listOf(music, youtube, podcast), null, null))
    }

    @Test
    fun lastPlayingWhenNothingPlays() {
        val paused = listOf(music, youtube.copy(isPlaying = false))
        assertEquals("video/2", FocusPolicy.choose(paused, null, "video/2"))
        assertEquals("music/1", FocusPolicy.choose(paused, null, "gone/9"))
    }

    @Test
    fun emptyListHasNoFocus() {
        assertNull(FocusPolicy.choose(emptyList(), "x", "y"))
    }

    @Test
    fun selectionClearedWhenAnotherStartsPlayingOrSessionCloses() {
        val active = setOf("a", "b")
        assertFalse(FocusPolicy.shouldClearUserSelection("a", null, active))
        assertFalse(FocusPolicy.shouldClearUserSelection("a", "a", active))
        assertTrue(FocusPolicy.shouldClearUserSelection("a", "b", active))
        assertTrue(FocusPolicy.shouldClearUserSelection("a", null, setOf("b")))
        assertFalse(FocusPolicy.shouldClearUserSelection(null, "b", active))
    }
}

class RecentAppsListTest {
    @Test
    fun pushDeduplicatesAndCaps() {
        var list = emptyList<RecentApp>()
        (1..12).forEach { list = RecentAppsList.push(list, RecentApp("p$it", "P$it")) }
        list = RecentAppsList.push(list, RecentApp("p5", "P5", "Son şarkı"))
        assertEquals(RecentAppsList.STORED, list.size)
        assertEquals("p5", list.first().packageName)
        assertEquals("Son şarkı", list.first().lastTitle)
        assertEquals(1, list.count { it.packageName == "p5" })
    }

    @Test
    fun visibleExcludesActiveAndShowsFive() {
        val list = (1..8).map { RecentApp("p$it", "P$it") }
        val visible = RecentAppsList.visible(list, activePackages = setOf("p1", "p3"))
        assertEquals(listOf("p2", "p4", "p5", "p6", "p7"), visible.map { it.packageName })
    }
}

class PublishPolicyTest {
    private val base = NowPlayingState(
        sessionId = "s",
        packageName = "p",
        appLabel = "A",
        title = "Şarkı",
        durationMs = 200_000,
        positionMs = 10_000,
        positionUpdatedAtEpochMs = 1_000_000,
        state = PlaybackStatus.PLAYING,
        volume = 5,
        volumeMax = 15,
    )

    private fun decide(next: NowPlayingState?, now: Long, level: SubscribeLevel = SubscribeLevel.FULL, last: NowPlayingState? = base) =
        PublishPolicy.shouldPublish(last, lastPublishedAtMs = 1_000_000, next = next, nowMs = now, level = level)

    @Test
    fun trackChangeAlwaysPublishes() {
        val next = base.copy(title = "Yeni şarkı")
        SubscribeLevel.entries.forEach { assertTrue(decide(next, 1_001_000, it), "level=$it") }
        assertTrue(decide(base.copy(state = PlaybackStatus.PAUSED), 1_001_000, SubscribeLevel.NONE))
    }

    @Test
    fun screenOffSkipsVolumeAndPosition() {
        assertFalse(decide(base.copy(volume = 6), 1_001_000, SubscribeLevel.NONE))
        assertFalse(decide(base.copy(positionMs = 90_000, positionUpdatedAtEpochMs = 1_001_000), 1_001_000, SubscribeLevel.NONE))
    }

    @Test
    fun volumeChangePublishesWhenScreenOn() {
        assertTrue(decide(base.copy(volume = 6), 1_000_100))
    }

    @Test
    fun naturalProgressIsThrottledTo15Seconds() {
        // 5 sn sonra uygulama yeni pozisyon bildirdi; tahminle uyumlu → gönderme.
        val after5s = base.copy(positionMs = 15_000, positionUpdatedAtEpochMs = 1_005_000)
        assertFalse(decide(after5s, 1_005_000))
        // 16 sn sonra → düzeltme için gönder.
        val after16s = base.copy(positionMs = 26_000, positionUpdatedAtEpochMs = 1_016_000)
        assertTrue(decide(after16s, 1_016_000))
    }

    @Test
    fun seekPublishesImmediately() {
        val seeked = base.copy(positionMs = 120_000, positionUpdatedAtEpochMs = 1_002_000)
        assertTrue(decide(seeked, 1_002_000))
    }

    @Test
    fun identicalStateIsSkipped() {
        assertFalse(decide(base, 1_100_000))
    }

    @Test
    fun sessionAppearingOrDisappearingPublishes() {
        assertTrue(decide(null, 1_001_000, SubscribeLevel.NONE))
        assertTrue(decide(base, 1_001_000, SubscribeLevel.NONE, last = null))
        assertFalse(decide(null, 1_001_000, SubscribeLevel.NONE, last = null))
    }

    @Test
    fun forceAlwaysPublishes() {
        assertTrue(PublishPolicy.shouldPublish(base, 0, base, 1, SubscribeLevel.NONE, force = true))
    }
}

class AckCacheTest {
    @Test
    fun remembersPerNodeAndEvictsOldest() {
        val cache = AckCache(capacity = 2)
        cache.put("watch", CommandAck(1, ok = true))
        cache.put("watch", CommandAck(2, ok = false))
        assertEquals(CommandAck(1, ok = true), cache.get("watch", 1))
        assertNull(cache.get("other", 1))
        cache.put("watch", CommandAck(3, ok = true, volume = 7))
        assertNull(cache.get("watch", 1))
        assertEquals(7, cache.get("watch", 3)?.volume)
    }
}
