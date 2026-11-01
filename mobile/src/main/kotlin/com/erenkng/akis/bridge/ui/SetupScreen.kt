package com.erenkng.akis.bridge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erenkng.akis.bridge.R

data class SetupUiState(
    val permissionGranted: Boolean = false,
    val connectedWatches: List<String> = emptyList(),
    /** null: henüz bilinmiyor (saat bağlı değilse öğrenilemez). */
    val watchAppInstalled: Boolean? = null,
    /** "Saate yükle" yalnızca debug sürümünde var. */
    val installerAvailable: Boolean = false,
    val versionName: String = "",
)

@Composable
fun SetupScreen(
    state: SetupUiState,
    onGrantPermission: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onInstallOnWatch: () -> Unit,
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
                text = stringResource(R.string.setup_title),
                color = PhoneColors.OnSurface,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(text = stringResource(R.string.setup_subtitle), color = PhoneColors.OnSurfaceDim, fontSize = 16.sp)

            Spacer(Modifier.height(8.dp))

            SectionCard(title = stringResource(R.string.setup_permission_title)) {
                Text(
                    text = stringResource(
                        if (state.permissionGranted) R.string.setup_permission_granted else R.string.setup_permission_missing,
                    ),
                    color = if (state.permissionGranted) PhoneColors.Accent else PhoneColors.OnSurface,
                    fontSize = 15.sp,
                )
                if (!state.permissionGranted) {
                    Button(
                        onClick = onGrantPermission,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PhoneColors.Accent, contentColor = PhoneColors.Bg),
                    ) {
                        Text(stringResource(R.string.setup_permission_button), fontWeight = FontWeight.Bold)
                    }
                }
                Text(text = stringResource(R.string.setup_privacy), color = PhoneColors.OnSurfaceDim, fontSize = 13.sp)
            }

            if (!state.permissionGranted) {
                SectionCard(title = stringResource(R.string.setup_restricted_title)) {
                    Text(text = stringResource(R.string.setup_restricted_body), color = PhoneColors.OnSurfaceDim, fontSize = 13.sp)
                    OutlinedButton(onClick = onOpenAppDetails, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.setup_restricted_button), color = PhoneColors.OnSurface)
                    }
                }
            }

            SectionCard(title = stringResource(R.string.setup_watch_title)) {
                val text = if (state.connectedWatches.isEmpty()) {
                    stringResource(R.string.setup_watch_none)
                } else {
                    stringResource(R.string.setup_watch_connected, state.connectedWatches.joinToString())
                }
                Text(text = text, color = PhoneColors.OnSurface, fontSize = 15.sp)
                state.watchAppInstalled?.let { installed ->
                    Text(
                        text = stringResource(if (installed) R.string.setup_watch_app_installed else R.string.setup_watch_app_missing),
                        color = if (installed) PhoneColors.Accent else PhoneColors.OnSurfaceDim,
                        fontSize = 14.sp,
                    )
                }
                if (state.installerAvailable) {
                    val installed = state.watchAppInstalled == true
                    Button(
                        onClick = onInstallOnWatch,
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (installed) {
                            ButtonDefaults.buttonColors(containerColor = PhoneColors.SurfaceHigh, contentColor = PhoneColors.OnSurface)
                        } else {
                            ButtonDefaults.buttonColors(containerColor = PhoneColors.Accent, contentColor = PhoneColors.Bg)
                        },
                    ) {
                        Text(
                            stringResource(if (installed) R.string.setup_watch_app_update else R.string.setup_watch_app_install),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (state.versionName.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.setup_version, state.versionName),
                    color = PhoneColors.OnSurfaceDim,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
