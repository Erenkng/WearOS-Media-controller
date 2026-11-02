package com.erenkng.akis.bridge.wear

import com.erenkng.akis.bridge.bridgeGraph
import com.erenkng.akis.shared.codec.AkisJson
import com.erenkng.akis.shared.model.CommandEnvelope
import com.erenkng.akis.shared.model.SubscribeRequest
import com.erenkng.akis.shared.protocol.AkisPaths
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Saatten gelen mesajlar. Süreç ölüyse sistem bu servisi başlatır, `AkisBridgeApp` grafiği kurar. */
class BridgeListenerService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        val graph = bridgeGraph
        when (event.path) {
            AkisPaths.CMD -> {
                val envelope = AkisJson.decodeOrNull<CommandEnvelope>(event.data) ?: return
                graph.scope.launch(Dispatchers.Main) {
                    graph.ensureTracking()
                    graph.commandHandler.handle(event.sourceNodeId, envelope)
                }
            }

            AkisPaths.SUBSCRIBE -> {
                val request = AkisJson.decodeOrNull<SubscribeRequest>(event.data) ?: return
                graph.scope.launch(Dispatchers.Main) {
                    graph.ensureTracking()
                    graph.subscription.set(request.level)
                }
            }
        }
    }
}
