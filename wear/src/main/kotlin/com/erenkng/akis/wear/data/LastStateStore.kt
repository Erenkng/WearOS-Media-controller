package com.erenkng.akis.wear.data

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import com.erenkng.akis.shared.codec.AkisJson
import com.erenkng.akis.shared.model.NowPlayingState
import java.io.File

/**
 * Son durum + son kapak diske yazılır; soğuk açılışta canlı veri gelmeden önce gösterilir
 * (BRIEF Bölüm 11: soğuk açılış → kapak görünür < 700 ms).
 */
class LastStateStore(context: Context) {

    private val stateFile = AtomicFile(File(context.filesDir, "last_state.json"))
    private val artFile = AtomicFile(File(context.filesDir, "last_art.bin"))

    class Snapshot(val state: NowPlayingState?, val artBytes: ByteArray?)

    /** IO iş parçacığında çağrılmalı. */
    fun read(): Snapshot? = try {
        if (!stateFile.baseFile.exists()) {
            null
        } else {
            val state = AkisJson.decodeOrNull<NowPlayingState>(stateFile.readFully())
            val art = if (state != null && state.artHash.isNotEmpty() && artFile.baseFile.exists()) artFile.readFully() else null
            Snapshot(state, art)
        }
    } catch (e: Exception) {
        Log.w(TAG, "Son durum okunamadı", e)
        null
    }

    /** IO iş parçacığında çağrılmalı. `artBytes` null ise mevcut kapak dosyasına dokunulmaz. */
    fun write(state: NowPlayingState?, artBytes: ByteArray?) {
        try {
            if (state == null) {
                stateFile.delete()
                artFile.delete()
                return
            }
            stateFile.writeAll(AkisJson.encode(state))
            if (artBytes != null) artFile.writeAll(artBytes)
            if (state.artHash.isEmpty()) artFile.delete()
        } catch (e: Exception) {
            Log.w(TAG, "Son durum yazılamadı", e)
        }
    }

    private fun AtomicFile.writeAll(bytes: ByteArray) {
        val out = startWrite()
        try {
            out.write(bytes)
            finishWrite(out)
        } catch (e: Exception) {
            failWrite(out)
            throw e
        }
    }

    private companion object {
        const val TAG = "LastStateStore"
    }
}
