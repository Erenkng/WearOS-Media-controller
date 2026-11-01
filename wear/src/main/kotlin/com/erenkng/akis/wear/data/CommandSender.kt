package com.erenkng.akis.wear.data

import android.net.Uri
import android.util.Log
import com.erenkng.akis.shared.codec.AkisJson
import com.erenkng.akis.shared.model.Command
import com.erenkng.akis.shared.model.CommandAck
import com.erenkng.akis.shared.model.CommandEnvelope
import com.erenkng.akis.shared.model.SubscribeLevel
import com.erenkng.akis.shared.model.SubscribeRequest
import com.erenkng.akis.shared.protocol.AkisPaths
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Saat → telefon komutları (BRIEF Bölüm 10).
 *
 * Her komut artan bir `seq` taşır. 3 sn içinde ack gelmezse **aynı seq** ile bir kez tekrarlanır;
 * telefon aynı seq'i ikinci kez yürütmez. İkinci zaman aşımında bağlantı yeniden ölçülür.
 */
class CommandSender(
    private val scope: CoroutineScope,
    private val messageClient: MessageClient,
    private val connection: ConnectionMonitor,
) : MessageClient.OnMessageReceivedListener {

    /** Süreç yeniden başlasa da çakışmasın diye zamanla başlar. */
    private val nextSeq = AtomicLong(System.currentTimeMillis())
    private val pending = ConcurrentHashMap<Long, CompletableDeferred<CommandAck>>()

    fun start() {
        messageClient.addListener(this, Uri.parse("wear://*${AkisPaths.ACK}"), MessageClient.FILTER_LITERAL)
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == AkisPaths.ACK) onAck(event.data)
    }

    fun onAck(bytes: ByteArray) {
        val ack = AkisJson.decodeOrNull<CommandAck>(bytes) ?: return
        pending.remove(ack.seq)?.complete(ack)
    }

    /** @return telefonun yanıtı; telefona ulaşılamadıysa null. */
    suspend fun send(command: Command): CommandAck? {
        val seq = nextSeq.incrementAndGet()
        val bytes = AkisJson.encode(CommandEnvelope(seq, command))
        repeat(ATTEMPTS) { attempt ->
            val nodeId = connection.bridgeNode.value?.id
                ?: run { connection.refresh(); connection.bridgeNode.value?.id }
            if (nodeId != null) {
                val deferred = CompletableDeferred<CommandAck>()
                pending[seq] = deferred
                val sent = try {
                    messageClient.sendMessage(nodeId, AkisPaths.CMD, bytes).await()
                    true
                } catch (e: Exception) {
                    Log.w(TAG, "Komut gönderilemedi (deneme ${attempt + 1}): $command", e)
                    false
                }
                if (sent) {
                    withTimeoutOrNull(ACK_TIMEOUT_MS) { deferred.await() }?.let { return it }
                } else {
                    delay(RETRY_DELAY_MS)
                }
                pending.remove(seq)
            }
        }
        connection.onCommandTimedOut()
        return null
    }

    fun fire(command: Command): Job = scope.launch { send(command) }

    /** Ekran açık/kapalı bilgisini telefona bildirir; ack beklenmez. */
    fun subscribe(level: SubscribeLevel): Job = scope.launch {
        val nodeId = connection.bridgeNode.value?.id ?: run { connection.refresh(); connection.bridgeNode.value?.id }
        if (nodeId == null) return@launch
        try {
            messageClient.sendMessage(nodeId, AkisPaths.SUBSCRIBE, AkisJson.encode(SubscribeRequest(level))).await()
        } catch (e: Exception) {
            Log.w(TAG, "Abonelik gönderilemedi: $level", e)
        }
    }

    private companion object {
        const val TAG = "CommandSender"
        const val ATTEMPTS = 2
        const val ACK_TIMEOUT_MS = 3_000L
        const val RETRY_DELAY_MS = 300L
    }
}
