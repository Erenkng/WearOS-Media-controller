package com.erenkng.akis.wear.data

import android.util.Log
import com.erenkng.akis.shared.protocol.AkisCapabilities
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Köprü kurulu mu, telefona ulaşılıyor mu (BRIEF Bölüm 10, "Bağlantı").
 *
 * Kabul kriteri: Bluetooth kapanınca 5 sn içinde hata durumu. Capability değişiklik bildirimine ek olarak
 * ekran açıkken her [POLL_MS]'de yerel (radyo kullanmayan) bir sorgu yapılır; komut zaman aşımları da
 * hemen yeniden ölçüm tetikler.
 */
class ConnectionMonitor(
    private val scope: CoroutineScope,
    private val capabilityClient: CapabilityClient,
    private val nodeClient: NodeClient,
) {
    private val _status = MutableStateFlow(ConnectionStatus.UNKNOWN)
    val status: StateFlow<ConnectionStatus> = _status

    private val _bridgeNode = MutableStateFlow<Node?>(null)
    val bridgeNode: StateFlow<Node?> = _bridgeNode

    private var pollJob: Job? = null

    private val capabilityListener = CapabilityClient.OnCapabilityChangedListener {
        scope.launch { refresh() }
    }

    fun start() {
        capabilityClient.addListener(capabilityListener, AkisCapabilities.BRIDGE)
        scope.launch { refresh() }
    }

    suspend fun refresh() {
        try {
            val reachable = capabilityClient
                .getCapability(AkisCapabilities.BRIDGE, CapabilityClient.FILTER_REACHABLE).await().nodes
            val known = capabilityClient
                .getCapability(AkisCapabilities.BRIDGE, CapabilityClient.FILTER_ALL).await().nodes
            val connected = nodeClient.connectedNodes.await()
            _bridgeNode.value = reachable.firstOrNull { it.isNearby } ?: reachable.firstOrNull()
            _status.value = ConnectionPolicy.evaluate(reachable.size, known.size, connected.size)
        } catch (e: Exception) {
            Log.w(TAG, "Bağlantı durumu okunamadı", e)
            _bridgeNode.value = null
            _status.value = ConnectionStatus.UNREACHABLE
        }
    }

    /** Ekran açıkken sık, kapalıyken hiç ölçme (pil). */
    fun setActivePolling(active: Boolean) {
        pollJob?.cancel()
        pollJob = if (active) {
            scope.launch {
                while (isActive) {
                    refresh()
                    delay(POLL_MS)
                }
            }
        } else {
            null
        }
    }

    fun onCommandTimedOut() {
        scope.launch { refresh() }
    }

    private companion object {
        const val TAG = "ConnectionMonitor"
        const val POLL_MS = 2_000L
    }
}
