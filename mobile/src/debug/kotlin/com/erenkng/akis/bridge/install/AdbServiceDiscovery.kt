package com.erenkng.akis.bridge.install

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.net.NetworkInterface

/**
 * Saatin kablosuz hata ayıklama servislerini yerel ağda mDNS ile bulur:
 * - [TYPE_PAIRING] (`_adb-tls-pairing._tcp`): saatte "Yeni cihaz eşleştir" penceresi açıkken.
 * - [TYPE_CONNECT] (`_adb-tls-connect._tcp`): kablosuz hata ayıklama açıkken.
 *
 * Telefonun kendi servisleri (telefonun kendi kablosuz hata ayıklaması açıksa) listeden çıkarılır.
 * NsdManager aynı anda tek çözümleme yaptığı için bulunan servisler sırayla çözülür.
 */
class AdbServiceDiscovery(context: Context, private val serviceType: String) {

    private val nsd = context.getSystemService(NsdManager::class.java)
    private val lock = Any()
    private val pending = ArrayDeque<NsdServiceInfo>()
    private var resolving = false
    private var running = false

    private val _endpoints = MutableStateFlow<List<AdbEndpoint>>(emptyList())
    val endpoints: StateFlow<List<AdbEndpoint>> = _endpoints

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) = Unit

        override fun onDiscoveryStopped(serviceType: String) = Unit

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            synchronized(lock) { running = false }
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            synchronized(lock) { pending.addLast(serviceInfo) }
            resolveNext()
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            _endpoints.update { list -> list.filterNot { it.serviceName == serviceInfo.serviceName } }
        }
    }

    fun start() {
        synchronized(lock) {
            if (running) return
            running = true
        }
        runCatching { nsd.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener) }
            .onFailure { synchronized(lock) { running = false } }
    }

    fun stop() {
        synchronized(lock) {
            if (!running) return
            running = false
            pending.clear()
        }
        runCatching { nsd.stopServiceDiscovery(discoveryListener) }
    }

    @Suppress("DEPRECATION") // resolveService/host: API 34'te yerine geçenler var ama minSdk 29.
    private fun resolveNext() {
        val next = synchronized(lock) {
            if (resolving || !running) return
            pending.removeFirstOrNull()?.also { resolving = true }
        } ?: return

        nsd.resolveService(
            next,
            object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = finishResolve()

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val host = serviceInfo.host?.hostAddress
                    if (host != null && serviceInfo.port > 0 && !isOwnAddress(host)) {
                        val endpoint = AdbEndpoint(serviceInfo.serviceName, host, serviceInfo.port)
                        _endpoints.update { list -> list.filterNot { it.serviceName == endpoint.serviceName } + endpoint }
                    }
                    finishResolve()
                }
            },
        )
    }

    private fun finishResolve() {
        synchronized(lock) { resolving = false }
        resolveNext()
    }

    companion object {
        const val TYPE_PAIRING = "_adb-tls-pairing._tcp"
        const val TYPE_CONNECT = "_adb-tls-connect._tcp"

        private fun isOwnAddress(host: String): Boolean = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .any { it.hostAddress == host }
        }.getOrDefault(false)
    }
}
