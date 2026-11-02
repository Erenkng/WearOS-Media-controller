package com.erenkng.akis.bridge.install

import android.content.Context
import android.os.Build
import android.util.Log
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Saatin `adbd`'sine bağlanan ADB istemcisi (libadb-android).
 *
 * RSA anahtarı ve kendinden imzalı sertifika uygulamanın özel klasöründe saklanır. Saat, eşleştirmede
 * bu anahtarın açık kısmını hatırlar; bu yüzden eşleştirme bir kez yapılır, sonraki kurulumlar doğrudan bağlanır.
 *
 * Not: `close()` özel anahtarı yok eder; bağlantıyı bitirmek için yalnızca `disconnect()` kullanılır.
 */
class AkisAdbManager private constructor(context: Context) : AbsAdbConnectionManager() {

    private val adbPrivateKey: PrivateKey
    private val adbCertificate: X509Certificate

    init {
        val keyFile = File(context.filesDir, "adb/akis-adb.p12")
        val (key, certificate) = loadKey(keyFile) ?: createKey(keyFile)
        adbPrivateKey = key
        adbCertificate = certificate
        // Uzak cihaz (saat) Android 11+; kablosuz hata ayıklama TLS ve büyük paketler kullanır.
        setApi(Build.VERSION_CODES.R)
        setTimeout(15, TimeUnit.SECONDS)
    }

    override fun getPrivateKey(): PrivateKey = adbPrivateKey

    override fun getCertificate(): Certificate = adbCertificate

    /** Saatte "Eşleştirilmiş cihazlar" listesinde görünen ad. */
    override fun getDeviceName(): String = "Akis Kopru (${Build.MODEL})"

    companion object {
        private const val TAG = "AkisAdbManager"
        private const val ALIAS = "akis-adb"
        private val PASSWORD = "akis-adb".toCharArray()
        private const val DAY_MS = 24L * 60 * 60 * 1000

        @Volatile
        private var instance: AkisAdbManager? = null

        fun get(context: Context): AkisAdbManager =
            instance ?: synchronized(this) {
                instance ?: AkisAdbManager(context.applicationContext).also { instance = it }
            }

        private fun loadKey(file: File): Pair<PrivateKey, X509Certificate>? {
            if (!file.exists()) return null
            return try {
                val store = KeyStore.getInstance("PKCS12")
                file.inputStream().use { store.load(it, PASSWORD) }
                val key = store.getKey(ALIAS, PASSWORD) as? PrivateKey ?: return null
                val certificate = store.getCertificate(ALIAS) as? X509Certificate ?: return null
                key to certificate
            } catch (e: Exception) {
                Log.w(TAG, "ADB anahtarı okunamadı, yenisi oluşturulacak", e)
                null
            }
        }

        private fun createKey(file: File): Pair<PrivateKey, X509Certificate> {
            val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
            val now = System.currentTimeMillis()
            val subject = X500Name("CN=Akis Kopru")
            val holder = JcaX509v3CertificateBuilder(
                subject,
                BigInteger.valueOf(now),
                Date(now - DAY_MS),
                Date(now + 30 * 365 * DAY_MS),
                subject,
                pair.public,
            ).build(JcaContentSignerBuilder("SHA256withRSA").build(pair.private))
            val certificate = JcaX509CertificateConverter().getCertificate(holder)

            val store = KeyStore.getInstance("PKCS12").apply {
                load(null, null)
                setKeyEntry(ALIAS, pair.private, PASSWORD, arrayOf(certificate))
            }
            file.parentFile?.mkdirs()
            file.outputStream().use { store.store(it, PASSWORD) }
            return pair.private to certificate
        }
    }
}
