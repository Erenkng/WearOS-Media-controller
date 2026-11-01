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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erenkng.akis.bridge.R

data class SetupUiState(
    val permissionGranted: Boolean = false,
    val connectedWatches: List<String> = emptyList(),
    val versionName: String = "",
)

// BRIEF Bölüm 4, renk token'ları.
private val Bg = Color(0xFF000000)
private val Surface = Color(0xFF141416)
private val OnSurface = Color(0xFFF2F0EB)
private val OnSurfaceDim = Color(0xFF9C9890)
private val Accent = Color(0xFFFF8A3D)
private val Danger = Color(0xFFFF5A5F)

private val AkisPhoneColors = darkColorScheme(
    primary = Accent,
    onPrimary = Bg,
    background = Bg,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceDim,
    error = Danger,
)

@Composable
fun SetupScreen(
    state: SetupUiState,
    onGrantPermission: () -> Unit,
    onOpenAppDetails: () -> Unit,
) {
    MaterialTheme(colorScheme = AkisPhoneColors) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_title),
                color = OnSurface,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(text = stringResource(R.string.setup_subtitle), color = OnSurfaceDim, fontSize = 16.sp)

            Spacer(Modifier.height(8.dp))

            SectionCard(title = stringResource(R.string.setup_permission_title)) {
                Text(
                    text = stringResource(
                        if (state.permissionGranted) R.string.setup_permission_granted else R.string.setup_permission_missing,
                    ),
                    color = if (state.permissionGranted) Accent else OnSurface,
                    fontSize = 15.sp,
                )
                if (!state.permissionGranted) {
                    Button(
                        onClick = onGrantPermission,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg),
                    ) {
                        Text(stringResource(R.string.setup_permission_button), fontWeight = FontWeight.Bold)
                    }
                }
                Text(text = stringResource(R.string.setup_privacy), color = OnSurfaceDim, fontSize = 13.sp)
            }

            if (!state.permissionGranted) {
                SectionCard(title = stringResource(R.string.setup_restricted_title)) {
                    Text(text = stringResource(R.string.setup_restricted_body), color = OnSurfaceDim, fontSize = 13.sp)
                    OutlinedButton(onClick = onOpenAppDetails, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.setup_restricted_button), color = OnSurface)
                    }
                }
            }

            SectionCard(title = stringResource(R.string.setup_watch_title)) {
                val text = if (state.connectedWatches.isEmpty()) {
                    stringResource(R.string.setup_watch_none)
                } else {
                    stringResource(R.string.setup_watch_connected, state.connectedWatches.joinToString())
                }
                Text(text = text, color = OnSurface, fontSize = 15.sp)
            }

            if (state.versionName.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.setup_version, state.versionName),
                    color = OnSurfaceDim,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = title, color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
