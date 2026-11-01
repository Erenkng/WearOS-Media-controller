package com.erenkng.akis.wear.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erenkng.akis.shared.model.Command
import com.erenkng.akis.shared.model.CommandAck
import com.erenkng.akis.shared.model.SubscribeLevel
import com.erenkng.akis.wear.WearGraph
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(private val graph: WearGraph) : ViewModel() {

    private val repository = graph.repository
    private val sender = graph.commandSender
    private val connection = graph.connection

    private val optimisticPlaying = MutableStateFlow<Boolean?>(null)

    private val _events = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<PlayerEvent> = _events

    val volume = VolumeAccumulator(viewModelScope) { delta ->
        sender.send(Command.AdjustVolume(delta))?.takeIf { it.ok }?.volume
    }

    private val trackAndArt = combine(repository.nowPlaying, optimisticPlaying) { nowPlaying, optimistic ->
        Triple(nowPlaying.state, nowPlaying.artwork, optimistic)
    }

    private val environment = combine(
        connection.status,
        repository.bridge,
        connection.bridgeNode,
        graph.prefs.onboardingWelcomeSeen,
    ) { status, bridge, node, welcomeSeen -> Environment(status, bridge?.status, node?.displayName, welcomeSeen) }

    val uiState: StateFlow<PlayerUiState> = combine(
        trackAndArt,
        environment,
        repository.sessions,
        repository.icons,
    ) { (track, art, optimistic), env, sessions, icons ->
        PlayerUiState.reduce(
            track = track,
            artwork = art,
            connection = env.status,
            bridgeStatus = env.bridge,
            bridgeName = env.bridgeName,
            welcomeSeen = env.welcomeSeen,
            optimisticPlaying = optimistic,
            sessions = sessions,
            icons = icons,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayerUiState())

    init {
        viewModelScope.launch {
            repository.nowPlaying.map { it.state }.distinctUntilChanged().collect { state ->
                // Telefondan gerçek durum geldi: iyimser değer artık gereksiz.
                optimisticPlaying.value = null
                if (state != null) volume.onRemoteVolume(state.volume, state.volumeMax)
            }
        }
    }

    /** Ekran açık/kapalı: telefona abonelik seviyesi + bağlantı yoklaması (BRIEF Bölüm 11, pil). */
    fun onVisible(visible: Boolean) {
        sender.subscribe(if (visible) SubscribeLevel.FULL else SubscribeLevel.NONE)
        connection.setActivePolling(visible)
    }

    fun onPlayPause() {
        val current = uiState.value
        optimisticPlaying.value = !current.isPlaying
        launchCommand(Command.Toggle) { optimisticPlaying.value = null }
    }

    fun onNext() = launchCommand(
        if (uiState.value.transportMode == TransportMode.SEEK) Command.SeekBy(TransportPolicy.SEEK_FORWARD_MS) else Command.Next,
    )

    fun onPrevious() = launchCommand(
        if (uiState.value.transportMode == TransportMode.SEEK) Command.SeekBy(TransportPolicy.SEEK_BACK_MS) else Command.Prev,
    )

    fun onResumeLast(packageName: String? = null) = launchCommand(Command.ResumeLast(packageName))

    fun onFocusSession(sessionId: String) = launchCommand(Command.FocusSession(sessionId))

    fun onVolumeSteps(steps: Int): VolumeFeedback = volume.onSteps(steps)

    fun onRetry() {
        viewModelScope.launch {
            connection.refresh()
            repository.refreshFromDataLayer()
        }
    }

    fun onWelcomeDone() = graph.prefs.markWelcomeSeen()

    private fun launchCommand(command: Command, onFailure: () -> Unit = {}) {
        viewModelScope.launch {
            val ack: CommandAck? = sender.send(command)
            when {
                ack == null -> {
                    onFailure()
                    _events.tryEmit(PlayerEvent.Unreachable)
                }
                !ack.ok -> {
                    onFailure()
                    _events.tryEmit(PlayerEvent.Unsupported)
                }
            }
        }
    }

    private data class Environment(
        val status: com.erenkng.akis.wear.data.ConnectionStatus,
        val bridge: com.erenkng.akis.shared.model.BridgeStatusCode?,
        val bridgeName: String?,
        val welcomeSeen: Boolean,
    )
}
