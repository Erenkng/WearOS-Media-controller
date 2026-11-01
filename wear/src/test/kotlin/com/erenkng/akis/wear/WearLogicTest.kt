package com.erenkng.akis.wear

import com.erenkng.akis.shared.model.MediaActions
import com.erenkng.akis.wear.data.ConnectionPolicy
import com.erenkng.akis.wear.data.ConnectionStatus
import com.erenkng.akis.wear.player.RotaryStepper
import com.erenkng.akis.wear.player.TransportMode
import com.erenkng.akis.wear.player.TransportPolicy
import com.erenkng.akis.wear.player.VolumeAccumulator
import com.erenkng.akis.wear.player.VolumeDisplay
import com.erenkng.akis.wear.player.VolumeFeedback
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class VolumeAccumulatorTest {

    @Test
    fun fifteenTicksWithinWindowBecomeOneCommand() = runTest {
        val sent = mutableListOf<Int>()
        val acc = VolumeAccumulator(this, windowMs = 120) { delta -> sent += delta; 0 + delta }
        acc.onRemoteVolume(0, 15)
        repeat(15) { acc.onSteps(1) }
        // İyimser: gösterge anında 15.
        assertEquals(VolumeDisplay(15, 15), acc.display.value)
        assertEquals(emptyList(), sent)
        advanceTimeBy(121)
        runCurrent()
        assertEquals(listOf(15), sent)
        assertEquals(VolumeDisplay(15, 15), acc.display.value)
    }

    @Test
    fun ticksAcrossWindowsAreSentSeparatelyAndSumCorrectly() = runTest {
        var phoneVolume = 5
        val sent = mutableListOf<Int>()
        val acc = VolumeAccumulator(this, windowMs = 120) { delta ->
            sent += delta
            phoneVolume = (phoneVolume + delta).coerceIn(0, 15)
            phoneVolume
        }
        acc.onRemoteVolume(5, 15)
        repeat(3) { acc.onSteps(1) }
        advanceTimeBy(121); runCurrent()
        repeat(2) { acc.onSteps(-1) }
        advanceTimeBy(121); runCurrent()
        assertEquals(listOf(3, -2), sent)
        assertEquals(6, phoneVolume)
        assertEquals(VolumeDisplay(6, 15), acc.display.value)
    }

    @Test
    fun optimisticValueCorrectedByAck() = runTest {
        // Telefon başka bir yerden de değişmiş: ack gerçek değeri getirir.
        val acc = VolumeAccumulator(this, windowMs = 120) { _ -> 11 }
        acc.onRemoteVolume(8, 15)
        acc.onSteps(1)
        assertEquals(9, acc.display.value?.volume)
        advanceTimeBy(121); runCurrent()
        assertEquals(11, acc.display.value?.volume)
    }

    @Test
    fun failedCommandRevertsOptimisticValue() = runTest {
        val acc = VolumeAccumulator(this, windowMs = 120) { _ -> null }
        acc.onRemoteVolume(8, 15)
        acc.onSteps(2)
        assertEquals(10, acc.display.value?.volume)
        advanceTimeBy(121); runCurrent()
        assertEquals(8, acc.display.value?.volume)
    }

    @Test
    fun remoteUpdatesDoNotOverrideInFlightChanges() = runTest {
        val gate = CompletableDeferred<Int>()
        val acc = VolumeAccumulator(this, windowMs = 120) { _ -> gate.await() }
        acc.onRemoteVolume(4, 15)
        acc.onSteps(3)
        advanceTimeBy(121); runCurrent()
        // Ack gelmeden eski bir durum yayını ulaştı: gösterge 7'de kalmalı, 4'e zıplamamalı.
        acc.onRemoteVolume(4, 15)
        assertEquals(7, acc.display.value?.volume)
        gate.complete(7)
        runCurrent()
        assertEquals(7, acc.display.value?.volume)
    }

    @Test
    fun limitFeedbackOnceThenSilent() = runTest {
        val acc = VolumeAccumulator(this, windowMs = 120) { d -> 13 + d }
        acc.onRemoteVolume(13, 15)
        assertEquals(VolumeFeedback.STEP, acc.onSteps(1))
        assertEquals(VolumeFeedback.LIMIT, acc.onSteps(1))
        assertEquals(VolumeFeedback.NONE, acc.onSteps(1))
        assertEquals(VolumeFeedback.NONE, acc.onSteps(5))
        assertEquals(15, acc.display.value?.volume)
        advanceTimeBy(121); runCurrent()
    }

    @Test
    fun lockedOrUnknownVolumeIgnoresSteps() = runTest {
        val acc = VolumeAccumulator(this) { error("gönderilmemeli") }
        assertEquals(VolumeFeedback.NONE, acc.onSteps(1))
        assertNull(acc.display.value)
        acc.onRemoteVolume(5, 0)
        assertEquals(VolumeFeedback.NONE, acc.onSteps(1))
        assertEquals(true, acc.display.value?.isLocked)
    }
}

class RotaryStepperTest {
    @Test
    fun bezelDetentsMapOneToOne() {
        val stepper = RotaryStepper(pixelsPerStep = 64f)
        assertEquals(1, stepper.onScroll(64f))
        assertEquals(-1, stepper.onScroll(-64f))
        assertEquals(3, stepper.onScroll(192f))
    }

    @Test
    fun crownIncrementsAccumulate() {
        val stepper = RotaryStepper(pixelsPerStep = 64f)
        assertEquals(0, stepper.onScroll(30f))
        assertEquals(0, stepper.onScroll(30f))
        assertEquals(1, stepper.onScroll(30f))
    }

    @Test
    fun directionChangeDropsRemainder() {
        val stepper = RotaryStepper(pixelsPerStep = 64f)
        assertEquals(0, stepper.onScroll(60f))
        assertEquals(0, stepper.onScroll(-10f))
        assertEquals(-1, stepper.onScroll(-54f))
    }
}

class TransportAndConnectionPolicyTest {
    @Test
    fun podcastWithoutSkipUsesSeekButtons() {
        assertEquals(TransportMode.SEEK, TransportPolicy.mode(MediaActions.PLAY_PAUSE or MediaActions.SEEK_TO))
        assertEquals(
            TransportMode.SKIP,
            TransportPolicy.mode(MediaActions.SEEK_TO or MediaActions.SKIP_TO_NEXT or MediaActions.SKIP_TO_PREVIOUS),
        )
        assertEquals(TransportMode.SKIP, TransportPolicy.mode(MediaActions.PLAY))
    }

    @Test
    fun connectionStatus() {
        assertEquals(ConnectionStatus.CONNECTED, ConnectionPolicy.evaluate(1, 1, 1))
        assertEquals(ConnectionStatus.UNREACHABLE, ConnectionPolicy.evaluate(0, 1, 0))
        assertEquals(ConnectionStatus.BRIDGE_MISSING, ConnectionPolicy.evaluate(0, 0, 1))
        assertEquals(ConnectionStatus.UNREACHABLE, ConnectionPolicy.evaluate(0, 0, 0))
    }
}
