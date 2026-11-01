package com.erenkng.akis.bridge.command

import android.util.Log
import com.erenkng.akis.shared.codec.AkisJson
import com.erenkng.akis.shared.model.CommandAck
import com.erenkng.akis.shared.model.CommandEnvelope
import com.erenkng.akis.shared.protocol.AkisPaths
import com.google.android.gms.wearable.MessageClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** `/akis/cmd` → yürüt → `/akis/ack` (BRIEF Bölüm 10). Tekrar gelen seq yeniden yürütülmez. */
class CommandHandler(
    private val scope: CoroutineScope,
    private val executor: CommandExecutor,
    private val messageClient: MessageClient,
    private val ackCache: AckCache,
) {

    fun handle(sourceNodeId: String, envelope: CommandEnvelope) {
        scope.launch {
            val ack = ackCache.get(sourceNodeId, envelope.seq) ?: run {
                val result = withContext(Dispatchers.Main) {
                    runCatching { executor.execute(envelope.command) }
                        .onFailure { Log.w(TAG, "Komut başarısız: ${envelope.command}", it) }
                        .getOrDefault(ExecutionResult(ok = false))
                }
                CommandAck(seq = envelope.seq, ok = result.ok, volume = result.volume)
                    .also { ackCache.put(sourceNodeId, it) }
            }
            try {
                messageClient.sendMessage(sourceNodeId, AkisPaths.ACK, AkisJson.encode(ack)).await()
            } catch (e: Exception) {
                Log.w(TAG, "Ack gönderilemedi (seq=${envelope.seq})", e)
            }
        }
    }

    private companion object {
        const val TAG = "CommandHandler"
    }
}
