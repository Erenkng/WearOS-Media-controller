package com.erenkng.akis.bridge.install

import android.content.Context
import io.github.muntashirakon.adb.AdbPairingRequiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Saat APK'sını saate kablosuz ADB ile kurar. Tüm çağrılar engelleyici olduğu için IO iş parçacığında çalışır.
 *
 * Akış: eşleştir (bir kez) → bağlan → `cmd package install -S` ile APK'yı akıt → saatte uygulamayı aç → bağlantıyı kes.
 */
class WatchInstaller(private val context: Context) {

    private val adb: AkisAdbManager get() = AkisAdbManager.get(context)

    /** Saatteki "Yeni cihaz eşleştir" penceresinin portu ve 6 haneli kodu. Hata durumunda istisna fırlatır. */
    suspend fun pair(endpoint: AdbEndpoint, code: String) {
        withContext(Dispatchers.IO) {
            adb.pair(endpoint.host, endpoint.port, code)
        }
    }

    /** @return false: saat bu telefonu tanımıyor, önce eşleştirme gerekiyor. */
    suspend fun connect(endpoint: AdbEndpoint): Boolean = withContext(Dispatchers.IO) {
        val manager = adb
        if (manager.isConnected) runCatching { manager.disconnect() }
        try {
            manager.connect(endpoint.host, endpoint.port)
            true
        } catch (_: AdbPairingRequiredException) {
            false
        }
    }

    /** Bağlıyken çağrılır. [onProgress] 0..1 arası, IO iş parçacığından. */
    suspend fun install(onProgress: (Float) -> Unit): InstallOutcome = withContext(Dispatchers.IO) {
        val apk = copyEmbeddedApk()
        val total = apk.length()
        val manager = adb
        // Her WRTE paketi saatin bildirdiği en büyük boyutu aşmamalı; aksi halde adbd bağlantıyı kapatır.
        val maxData = manager.adbConnection?.maxData ?: throw IOException("Saate bağlı değil")
        val chunkSize = maxData.coerceIn(MIN_CHUNK, MAX_CHUNK).coerceAtMost(maxData)

        manager.openStream("exec:cmd package install -r -S $total").use { stream ->
            val output = stream.openOutputStream()
            apk.inputStream().use { input ->
                val buffer = ByteArray(chunkSize)
                var sent = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    output.write(buffer, 0, read)
                    output.flush()
                    sent += read
                    onProgress(sent.toFloat() / total)
                }
            }
            InstallOutcome.parse(readAll(stream.openInputStream()))
        }.also { apk.delete() }
    }

    /** Kurulumdan sonra saatte Akış'ı açar; başarısız olursa önemli değil. */
    suspend fun launchOnWatch() {
        withContext(Dispatchers.IO) {
            runCatching {
                adb.openStream("shell:am start -n $WATCH_PACKAGE/$WATCH_ACTIVITY").use { readAll(it.openInputStream()) }
            }
        }
    }

    suspend fun disconnect() {
        withContext(Dispatchers.IO) { runCatching { adb.disconnect() } }
    }

    /** Asset'ler sıkıştırılmış olabilir; boyutu bilmek için önce önbelleğe kopyalanır (`install -S` boyut ister). */
    private fun copyEmbeddedApk(): File {
        val target = File(context.cacheDir, "akis-wear.apk")
        context.assets.open(EMBEDDED_APK).use { input -> target.outputStream().use { input.copyTo(it) } }
        return target
    }

    /** Uzak taraf akışı kapatana kadar okur. Kapanış bazen istisna olarak gelir; o ana kadarki çıktı geçerlidir. */
    private fun readAll(input: InputStream): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        try {
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                out.write(buffer, 0, read)
            }
        } catch (e: IOException) {
            if (out.size() == 0) throw e
        }
        return out.toString(Charsets.UTF_8.name())
    }

    companion object {
        const val EMBEDDED_APK = "wear/akis-wear.apk"
        private const val WATCH_PACKAGE = "com.erenkng.akis"
        private const val WATCH_ACTIVITY = "com.erenkng.akis.wear.MainActivity"
        private const val MIN_CHUNK = 4 * 1024
        private const val MAX_CHUNK = 256 * 1024

        fun hasEmbeddedApk(context: Context): Boolean =
            runCatching { context.assets.list("wear")?.contains("akis-wear.apk") == true }.getOrDefault(false)
    }
}
