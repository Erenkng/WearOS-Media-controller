package com.erenkng.akis.bridge.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.erenkng.akis.bridge.BuildConfig
import com.erenkng.akis.bridge.bridgeGraph
import com.erenkng.akis.bridge.install.InstallerEntry
import com.erenkng.akis.bridge.listener.NotificationAccess
import com.erenkng.akis.shared.protocol.AkisCapabilities
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Telefondaki tek ekran: izin durumu, izin butonu, bağlı saat (BRIEF Bölüm 10, `:mobile`). */
class SetupActivity : ComponentActivity() {

    private var uiState by mutableStateOf(SetupUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SetupScreen(
                state = uiState,
                onGrantPermission = ::openPermissionSettings,
                onOpenAppDetails = { startSafely(NotificationAccess.appDetailsIntent(this)) },
                onInstallOnWatch = { InstallerEntry.open(this) },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Kullanıcı ayarlardan dönünce izin durumu ve saate giden köprü durumu tazelenir.
        bridgeGraph.ensureTracking()
        refresh()
    }

    private fun refresh() {
        val granted = NotificationAccess.isGranted(this)
        uiState = uiState.copy(
            permissionGranted = granted,
            versionName = BuildConfig.VERSION_NAME,
            installerAvailable = InstallerEntry.isAvailable,
        )
        lifecycleScope.launch {
            val watches = runCatching { bridgeGraph.nodeClient.connectedNodes.await() }
                .getOrDefault(emptyList())
            // Saat uygulaması `akis_watch` capability'sini ilan eder; bağlı saat yoksa bilinemez.
            val watchAppInstalled = if (watches.isEmpty()) {
                null
            } else {
                runCatching {
                    Wearable.getCapabilityClient(this@SetupActivity)
                        .getCapability(AkisCapabilities.WATCH, CapabilityClient.FILTER_ALL).await()
                        .nodes.isNotEmpty()
                }.getOrNull()
            }
            uiState = uiState.copy(connectedWatches = watches.map { it.displayName }, watchAppInstalled = watchAppInstalled)
        }
    }

    private fun openPermissionSettings() {
        if (!startSafely(NotificationAccess.settingsIntent(this))) {
            startSafely(NotificationAccess.fallbackSettingsIntent())
        }
    }

    private fun startSafely(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
