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
import com.erenkng.akis.bridge.listener.NotificationAccess
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
        uiState = uiState.copy(permissionGranted = granted, versionName = BuildConfig.VERSION_NAME)
        lifecycleScope.launch {
            val watches = runCatching { bridgeGraph.nodeClient.connectedNodes.await() }
                .getOrDefault(emptyList())
                .map { it.displayName }
            uiState = uiState.copy(connectedWatches = watches)
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
