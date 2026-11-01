package com.erenkng.akis.shared

import com.erenkng.akis.shared.codec.AkisJson
import com.erenkng.akis.shared.model.BridgeStatus
import com.erenkng.akis.shared.model.BridgeStatusCode
import com.erenkng.akis.shared.model.Command
import com.erenkng.akis.shared.model.CommandAck
import com.erenkng.akis.shared.model.CommandEnvelope
import com.erenkng.akis.shared.model.CustomActionInfo
import com.erenkng.akis.shared.model.NowPlayingState
import com.erenkng.akis.shared.model.PlaybackStatus
import com.erenkng.akis.shared.model.RepeatMode
import com.erenkng.akis.shared.model.SessionSummary
import com.erenkng.akis.shared.model.SessionsPayload
import com.erenkng.akis.shared.model.SubscribeLevel
import com.erenkng.akis.shared.model.SubscribeRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProtocolSerializationTest {

    private val allCommands: List<Command> = listOf(
        Command.Play,
        Command.Pause,
        Command.Toggle,
        Command.Next,
        Command.Prev,
        Command.SeekTo(42_000),
        Command.SeekBy(-15_000),
        Command.SetVolume(8),
        Command.AdjustVolume(-3),
        Command.SkipToQueueItem(7),
        Command.Custom("like"),
        Command.SetShuffle(true),
        Command.SetRepeat(RepeatMode.ALL),
        Command.FocusSession("com.example/1a2b"),
        Command.ResumeLast(),
        Command.ResumeLast("com.example.music"),
        Command.OpenOnPhone,
        Command.SleepTimer(30),
        Command.SleepTimer(Command.SleepTimer.END_OF_ITEM),
    )

    @Test
    fun everyCommandRoundTrips() {
        allCommands.forEachIndexed { index, command ->
            val envelope = CommandEnvelope(seq = index.toLong() + 1, command = command)
            val decoded = AkisJson.decode<CommandEnvelope>(AkisJson.encode(envelope))
            assertEquals(envelope, decoded)
        }
    }

    @Test
    fun commandWireNamesMatchBrief() {
        val expected = listOf(
            "PLAY", "PAUSE", "TOGGLE", "NEXT", "PREV", "SEEK_TO", "SEEK_BY", "SET_VOLUME",
            "ADJUST_VOLUME", "SKIP_TO_QUEUE_ITEM", "CUSTOM", "SET_SHUFFLE", "SET_REPEAT",
            "FOCUS_SESSION", "RESUME_LAST", "RESUME_LAST", "OPEN_ON_PHONE", "SLEEP_TIMER", "SLEEP_TIMER",
        )
        val actual = allCommands.map { command ->
            val text = AkisJson.encode(CommandEnvelope(1, command)).decodeToString()
            Regex("\"type\":\"([A-Z_]+)\"").find(text)!!.groupValues[1]
        }
        assertEquals(expected, actual)
    }

    @Test
    fun nowPlayingStateRoundTrips() {
        val state = NowPlayingState(
            sessionId = "com.example.music/abc",
            packageName = "com.example.music",
            appLabel = "Örnek Müzik",
            title = "Dağlar Dağlar — Canlı İzmir Kaydı",
            artist = "Barış Manço",
            album = "Ünlü Şarkılar",
            durationMs = 195_000,
            positionMs = 42_000,
            positionUpdatedAtEpochMs = 1_760_000_000_000,
            speed = 1.5f,
            state = PlaybackStatus.PLAYING,
            actions = 0x1FF,
            customActions = listOf(CustomActionInfo("like", "Beğen", iconPng = "iVBORw0KGgo=")),
            shuffle = true,
            repeat = RepeatMode.ONE,
            volume = 8,
            volumeMax = 15,
            isRemote = false,
            outputLabel = "Sony WH-1000XM5 · Bluetooth",
            artHash = "0123456789abcdef",
        )
        assertEquals(state, AkisJson.decode<NowPlayingState>(AkisJson.encode(state)))
    }

    @Test
    fun unknownFieldsFromNewerVersionsAreIgnored() {
        val json = """{"sessionId":"s","packageName":"p","appLabel":"A","futureField":123}"""
        val state = AkisJson.decode<NowPlayingState>(json.encodeToByteArray())
        assertEquals("s", state.sessionId)
        assertEquals(PlaybackStatus.STOPPED, state.state)
    }

    @Test
    fun corruptBytesDecodeToNull() {
        assertNull(AkisJson.decodeOrNull<NowPlayingState>("{bozuk".encodeToByteArray()))
        assertNull(AkisJson.decodeOrNull<NowPlayingState>(null))
    }

    @Test
    fun otherPayloadsRoundTrip() {
        val sessions = SessionsPayload(
            active = listOf(SessionSummary("a/1", "a", "A", "Şarkı", PlaybackStatus.PAUSED)),
            recent = listOf(SessionSummary("", "b", "B")),
        )
        assertEquals(sessions, AkisJson.decode<SessionsPayload>(AkisJson.encode(sessions)))

        val ack = CommandAck(seq = 9, ok = true, volume = 11)
        assertEquals(ack, AkisJson.decode<CommandAck>(AkisJson.encode(ack)))

        val ackNoVolume = CommandAck(seq = 10, ok = false)
        val ackText = AkisJson.encode(ackNoVolume).decodeToString()
        assertTrue("volume" !in ackText, ackText)
        assertEquals(ackNoVolume, AkisJson.decode<CommandAck>(ackText.encodeToByteArray()))

        val subscribe = SubscribeRequest(SubscribeLevel.TRACK_ONLY)
        assertEquals(subscribe, AkisJson.decode<SubscribeRequest>(AkisJson.encode(subscribe)))

        val bridge = BridgeStatus(BridgeStatusCode.PERMISSION_MISSING, bridgeVersionCode = 3)
        assertEquals(bridge, AkisJson.decode<BridgeStatus>(AkisJson.encode(bridge)))
    }
}
