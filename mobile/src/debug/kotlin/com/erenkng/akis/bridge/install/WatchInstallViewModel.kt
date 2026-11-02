package com.erenkng.akis.bridge.install

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erenkng.akis.bridge.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class WatchInstallViewModel(app: Application) : AndroidViewModel(app) {

    enum class Phase { IDLE, PAIRING, CONNECTING, INSTALLING, LAUNCHING, DONE }

    data class UiState(
        val apkAvailable: Boolean = true,
        val paired: Boolean = false,
        val pairingCode: String = "",
        val manualPairing: String = "",
        val manualConnect: String = "",
        val pairingEndpoints: List<AdbEndpoint> = emptyList(),
        val connectEndpoints: List<AdbEndpoint> = emptyList(),
        val phase: Phase = Phase.IDLE,
        val progress: Float = 0f,
        val error: String? = null,
    ) {
        val busy: Boolean get() = phase != Phase.IDLE && phase != Phase.DONE
    }

    private val installer = WatchInstaller(app)
    private val prefs = app.getSharedPreferences("watch_install", Context.MODE_PRIVATE)
    private val pairingDiscovery = AdbServiceDiscovery(app, AdbServiceDiscovery.TYPE_PAIRING)
    private val connectDiscovery = AdbServiceDiscovery(app, AdbServiceDiscovery.TYPE_CONNECT)

    private val form = MutableStateFlow(
        UiState(
            apkAvailable = WatchInstaller.hasEmbeddedApk(app),
            paired = prefs.getBoolean(KEY_PAIRED, false),
        ),
    )

    val state: StateFlow<UiState> = combine(
        form,
        pairingDiscovery.endpoints,
        connectDiscovery.endpoints,
    ) { current, pairing, connect ->
        current.copy(pairingEndpoints = pairing, connectEndpoints = connect)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, form.value)

    init {
        pairingDiscovery.start()
        connectDiscovery.start()
    }

    override fun onCleared() {
        pairingDiscovery.stop()
        connectDiscovery.stop()
    }

    fun onCodeChanged(text: String) = form.update { it.copy(pairingCode = PairingCode.normalize(text), error = null) }

    fun onManualPairingChanged(text: String) = form.update { it.copy(manualPairing = text, error = null) }

    fun onManualConnectChanged(text: String) = form.update { it.copy(manualConnect = text, error = null) }

    /** Saat bu telefonu unuttuysa (ör. "Eşleştirilmiş cihazlar"dan silindiyse) yeniden eşleştirmeye dön. */
    fun onForgetPairing() {
        prefs.edit { putBoolean(KEY_PAIRED, false) }
        form.update { it.copy(paired = false, phase = Phase.IDLE, error = null) }
    }

    fun onPairAndInstall() {
        val current = state.value
        if (current.busy) return
        val endpoint = AdbEndpoint.parse(current.manualPairing) ?: current.pairingEndpoints.firstOrNull()
        if (endpoint == null) return showError(R.string.install_error_no_pairing_window)
        if (!PairingCode.isValid(current.pairingCode)) return showError(R.string.install_error_code)

        viewModelScope.launch {
            form.update { it.copy(phase = Phase.PAIRING, error = null) }
            try {
                installer.pair(endpoint, current.pairingCode)
            } catch (e: Exception) {
                return@launch fail(R.string.install_error_pair, e)
            }
            prefs.edit {
                putBoolean(KEY_PAIRED, true)
                putString(KEY_HOST, endpoint.host)
            }
            form.update { it.copy(paired = true, pairingCode = "", manualPairing = "") }
            installNow(preferredHost = endpoint.host)
        }
    }

    fun onInstall() {
        if (state.value.busy) return
        viewModelScope.launch { installNow(preferredHost = prefs.getString(KEY_HOST, null)) }
    }

    private suspend fun installNow(preferredHost: String?) {
        form.update { it.copy(phase = Phase.CONNECTING, progress = 0f, error = null) }
        val endpoint = findConnectEndpoint(preferredHost)
            ?: return fail(R.string.install_error_no_watch, null)

        val connected = try {
            installer.connect(endpoint)
        } catch (e: Exception) {
            return fail(R.string.install_error_connect, e)
        }
        if (!connected) {
            prefs.edit { putBoolean(KEY_PAIRED, false) }
            form.update { it.copy(paired = false) }
            return fail(R.string.install_error_needs_pairing, null)
        }
        prefs.edit { putString(KEY_HOST, endpoint.host) }

        form.update { it.copy(phase = Phase.INSTALLING) }
        val outcome = try {
            installer.install { progress -> form.update { it.copy(progress = progress) } }
        } catch (e: Exception) {
            installer.disconnect()
            return fail(R.string.install_error_transfer, e)
        }

        when (outcome) {
            InstallOutcome.Success -> {
                form.update { it.copy(phase = Phase.LAUNCHING, progress = 1f) }
                installer.launchOnWatch()
                installer.disconnect()
                form.update { it.copy(phase = Phase.DONE) }
            }

            is InstallOutcome.Failure -> {
                installer.disconnect()
                form.update { it.copy(phase = Phase.IDLE, error = failureMessage(outcome)) }
            }
        }
    }

    /**
     * Önce elle girilen adres; yoksa ağda bulunan bağlantı servisi (eşleşilen saatin IP'si öncelikli).
     * Eşleştirmeden hemen sonra servis henüz görünmemiş olabilir; birkaç saniye beklenir.
     */
    private suspend fun findConnectEndpoint(preferredHost: String?): AdbEndpoint? {
        AdbEndpoint.parse(state.value.manualConnect)?.let { return it }
        val endpoints = state.value.connectEndpoints.ifEmpty {
            withTimeoutOrNull(DISCOVERY_WAIT_MS) { connectDiscovery.endpoints.first { it.isNotEmpty() } }.orEmpty()
        }
        return endpoints.firstOrNull { it.host == preferredHost } ?: endpoints.firstOrNull()
    }

    private fun failureMessage(failure: InstallOutcome.Failure): String {
        val res = when (failure.code) {
            "INSTALL_FAILED_UPDATE_INCOMPATIBLE" -> R.string.install_fail_signature
            "INSTALL_FAILED_INSUFFICIENT_STORAGE" -> R.string.install_fail_storage
            "INSTALL_FAILED_VERSION_DOWNGRADE" -> R.string.install_fail_downgrade
            "INSTALL_FAILED_OLDER_SDK" -> R.string.install_fail_old_watch
            else -> R.string.install_fail_generic
        }
        return getApplication<Application>().getString(res, failure.output.take(MAX_DETAIL))
    }

    private fun showError(@StringRes res: Int) {
        form.update { it.copy(error = getApplication<Application>().getString(res)) }
    }

    private fun fail(@StringRes res: Int, error: Throwable?) {
        val detail = error?.let { it.message ?: it.javaClass.simpleName }.orEmpty().take(MAX_DETAIL)
        form.update {
            it.copy(phase = Phase.IDLE, error = getApplication<Application>().getString(res, detail).trim())
        }
    }

    private companion object {
        const val KEY_PAIRED = "paired"
        const val KEY_HOST = "host"
        const val DISCOVERY_WAIT_MS = 8_000L
        const val MAX_DETAIL = 300
    }
}
