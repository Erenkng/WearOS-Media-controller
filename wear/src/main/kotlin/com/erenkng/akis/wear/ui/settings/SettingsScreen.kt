package com.erenkng.akis.wear.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.erenkng.akis.shared.model.BridgeStatusCode
import com.erenkng.akis.wear.BuildConfig
import com.erenkng.akis.wear.R
import com.erenkng.akis.wear.data.ConnectionStatus
import com.erenkng.akis.wear.player.PlayerUiState
import com.erenkng.akis.wear.ui.theme.AkisColors
import com.erenkng.akis.wear.ui.theme.AkisType

/**
 * Ayarlar (PLAN K5): Faz 1'de yalnızca durum bilgisi. BRIEF Bölüm 7.6'daki anahtarlar
 * (çift dokunuş, dinamik renk, çerçeve davranışı, titreşim yoğunluğu…) ilgili fazda eklenecek.
 */
@Composable
fun SettingsScreen(state: PlayerUiState) {
    val listState = rememberTransformingLazyColumnState()

    val bridgeText = when (state.connection) {
        ConnectionStatus.CONNECTED -> stringResource(R.string.settings_bridge_connected, state.bridgeName ?: "—")
        ConnectionStatus.UNREACHABLE -> stringResource(R.string.settings_bridge_unreachable)
        ConnectionStatus.BRIDGE_MISSING -> stringResource(R.string.settings_bridge_missing)
        ConnectionStatus.UNKNOWN -> stringResource(R.string.settings_bridge_checking)
    }
    val permissionText = when (state.bridgeStatus) {
        BridgeStatusCode.OK -> stringResource(R.string.settings_permission_ok)
        BridgeStatusCode.PERMISSION_MISSING -> stringResource(R.string.settings_permission_missing)
        null -> stringResource(R.string.settings_permission_unknown)
    }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader {
                    Text(stringResource(R.string.settings_title), style = AkisType.screenTitle, textAlign = TextAlign.Center)
                }
            }
            item { InfoRow(stringResource(R.string.settings_bridge), bridgeText) }
            item { InfoRow(stringResource(R.string.settings_permission), permissionText) }
            item { InfoRow(stringResource(R.string.settings_version), BuildConfig.VERSION_NAME) }
            item { InfoRow(stringResource(R.string.settings_licenses), stringResource(R.string.settings_licenses_body)) }
        }
    }
}

/** Tıklanamayan bilgi satırı; tam hap şekli (BRIEF Bölüm 4). */
@Composable
private fun InfoRow(title: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(AkisColors.Surface, RoundedCornerShape(26.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = AkisType.listTitle)
        Text(value, style = AkisType.listSubtitle)
    }
}
