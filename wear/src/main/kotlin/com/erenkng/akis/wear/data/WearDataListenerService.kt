package com.erenkng.akis.wear.data

import com.erenkng.akis.shared.protocol.AkisPaths
import com.erenkng.akis.wear.wearGraph
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Uygulama kapalıyken gelen durum değişiklikleri (tile güncellemesi için) ve ack'ler.
 * Uygulama açıkken aynı olaylar canlı dinleyicilere de gelir; işleme idempotenttir.
 */
class WearDataListenerService : WearableListenerService() {

    override fun onDataChanged(events: DataEventBuffer) {
        val items = events.filter { it.type == DataEvent.TYPE_CHANGED }.map { it.dataItem.freeze() }
        wearGraph.repository.handleItems(items)
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == AkisPaths.ACK) wearGraph.commandSender.onAck(event.data)
    }
}
