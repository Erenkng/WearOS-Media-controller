package com.erenkng.akis.bridge.install

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** "Saate yükle": saat APK'sını telefondan saate kablosuz ADB ile kurar (yalnızca debug). */
class WatchInstallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: WatchInstallViewModel = viewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            WatchInstallScreen(
                state = state,
                onCodeChanged = viewModel::onCodeChanged,
                onManualPairingChanged = viewModel::onManualPairingChanged,
                onManualConnectChanged = viewModel::onManualConnectChanged,
                onPairAndInstall = viewModel::onPairAndInstall,
                onInstall = viewModel::onInstall,
                onForgetPairing = viewModel::onForgetPairing,
                onClose = ::finish,
            )
        }
    }
}
