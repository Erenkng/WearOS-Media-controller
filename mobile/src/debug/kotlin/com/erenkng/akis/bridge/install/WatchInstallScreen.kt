package com.erenkng.akis.bridge.install

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erenkng.akis.bridge.R
import com.erenkng.akis.bridge.install.WatchInstallViewModel.Phase
import com.erenkng.akis.bridge.install.WatchInstallViewModel.UiState
import com.erenkng.akis.bridge.ui.AkisPhoneTheme
import com.erenkng.akis.bridge.ui.PhoneColors
import com.erenkng.akis.bridge.ui.SectionCard

@Composable
fun WatchInstallScreen(
    state: UiState,
    onCodeChanged: (String) -> Unit,
    onManualPairingChanged: (String) -> Unit,
    onManualConnectChanged: (String) -> Unit,
    onPairAndInstall: () -> Unit,
    onInstall: () -> Unit,
    onForgetPairing: () -> Unit,
    onClose: () -> Unit,
) {
    AkisPhoneTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PhoneColors.Bg)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.install_title),
                color = PhoneColors.OnSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(text = stringResource(R.string.install_subtitle), color = PhoneColors.OnSurfaceDim, fontSize = 15.sp)

            if (!state.apkAvailable) {
                Text(text = stringResource(R.string.install_error_no_apk), color = PhoneColors.Danger, fontSize = 15.sp)
            }

            SectionCard(title = stringResource(R.string.install_prepare_title)) {
                listOf(
                    R.string.install_prepare_wifi,
                    R.string.install_prepare_developer,
                    R.string.install_prepare_debugging,
                    R.string.install_prepare_pair,
                ).forEachIndexed { index, res ->
                    Text(text = "${index + 1}. ${stringResource(res)}", color = PhoneColors.OnSurface, fontSize = 14.sp)
                }
            }

            SectionCard(title = stringResource(R.string.install_pair_title)) {
                if (state.paired) {
                    Text(text = stringResource(R.string.install_paired), color = PhoneColors.Accent, fontSize = 15.sp)
                    TextButton(onClick = onForgetPairing, enabled = !state.busy) {
                        Text(stringResource(R.string.install_pair_again), color = PhoneColors.OnSurfaceDim)
                    }
                } else {
                    DiscoveryLine(
                        endpoints = state.pairingEndpoints,
                        searchingText = stringResource(R.string.install_pair_searching),
                        foundTextRes = R.string.install_pair_found,
                    )
                    OutlinedTextField(
                        value = state.pairingCode,
                        onValueChange = onCodeChanged,
                        label = { Text(stringResource(R.string.install_pair_code)) },
                        singleLine = true,
                        enabled = !state.busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.manualPairing,
                        onValueChange = onManualPairingChanged,
                        label = { Text(stringResource(R.string.install_pair_manual)) },
                        singleLine = true,
                        enabled = !state.busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PrimaryButton(
                        text = stringResource(R.string.install_pair_button),
                        enabled = !state.busy && state.apkAvailable,
                        onClick = onPairAndInstall,
                    )
                }
            }

            SectionCard(title = stringResource(R.string.install_install_title)) {
                DiscoveryLine(
                    endpoints = state.connectEndpoints,
                    searchingText = stringResource(R.string.install_watch_searching),
                    foundTextRes = R.string.install_watch_found,
                )
                OutlinedTextField(
                    value = state.manualConnect,
                    onValueChange = onManualConnectChanged,
                    label = { Text(stringResource(R.string.install_connect_manual)) },
                    singleLine = true,
                    enabled = !state.busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                PhaseStatus(state)
                PrimaryButton(
                    text = stringResource(R.string.install_button),
                    enabled = state.paired && !state.busy && state.apkAvailable,
                    onClick = onInstall,
                )
                if (state.phase == Phase.DONE) {
                    Text(text = stringResource(R.string.install_done), color = PhoneColors.Accent, fontSize = 15.sp)
                }
            }

            state.error?.let { Text(text = it, color = PhoneColors.Danger, fontSize = 14.sp) }

            TextButton(onClick = onClose) {
                Text(stringResource(R.string.install_close), color = PhoneColors.OnSurfaceDim)
            }
        }
    }
}

@Composable
private fun DiscoveryLine(endpoints: List<AdbEndpoint>, searchingText: String, foundTextRes: Int) {
    val found = endpoints.firstOrNull()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (found == null) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PhoneColors.OnSurfaceDim)
            Text(text = searchingText, color = PhoneColors.OnSurfaceDim, fontSize = 14.sp)
        } else {
            Text(text = stringResource(foundTextRes, found.label), color = PhoneColors.Accent, fontSize = 14.sp)
        }
    }
}

@Composable
private fun PhaseStatus(state: UiState) {
    val text = when (state.phase) {
        Phase.PAIRING -> R.string.install_phase_pairing
        Phase.CONNECTING -> R.string.install_phase_connecting
        Phase.INSTALLING -> R.string.install_phase_installing
        Phase.LAUNCHING -> R.string.install_phase_launching
        Phase.IDLE, Phase.DONE -> null
    } ?: return
    Text(text = stringResource(text), color = PhoneColors.OnSurface, fontSize = 14.sp)
    if (state.phase == Phase.INSTALLING) {
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth(),
            color = PhoneColors.Accent,
            trackColor = PhoneColors.SurfaceHigh,
        )
    } else {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = PhoneColors.Accent,
            trackColor = PhoneColors.SurfaceHigh,
        )
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = PhoneColors.Accent, contentColor = PhoneColors.Bg),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}
