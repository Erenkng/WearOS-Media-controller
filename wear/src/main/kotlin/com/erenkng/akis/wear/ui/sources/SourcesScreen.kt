package com.erenkng.akis.wear.ui.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.erenkng.akis.shared.model.PlaybackStatus
import com.erenkng.akis.shared.model.SessionSummary
import com.erenkng.akis.wear.R
import com.erenkng.akis.wear.player.OutputKind
import com.erenkng.akis.wear.player.PlayerUiState
import com.erenkng.akis.wear.ui.common.CircleBitmap
import com.erenkng.akis.wear.ui.theme.AkisColors
import com.erenkng.akis.wear.ui.theme.AkisType
import com.erenkng.akis.wear.ui.theme.LocalAccent

/**
 * Kaynaklar paneli (BRIEF Bölüm 7.2), ana ekranda yukarı kaydırınca açılır.
 * Liste döner çerçeveyle kaydırılır (TransformingLazyColumn'un yerleşik döner girdi desteği).
 *
 * Faz 3 kancası: en üstte "Telefon | Saat" hedef seçici. Faz 1'de yalnızca telefon olduğu için gizli.
 */
@Composable
fun SourcesScreen(
    state: PlayerUiState,
    onSessionSelected: (sessionId: String) -> Unit,
    onRecentSelected: (packageName: String) -> Unit,
    onOutputClick: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val focusedId = state.track?.sessionId

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { Header(stringResource(R.string.sources_active)) }
            if (state.sessions.active.isEmpty()) {
                item { Hint(stringResource(R.string.sources_active_none)) }
            } else {
                items(state.sessions.active, key = { "active:" + it.sessionId }) { session ->
                    SessionRow(
                        session = session,
                        icon = state.icons[session.packageName],
                        isFocused = session.sessionId == focusedId,
                        onClick = { onSessionSelected(session.sessionId) },
                    )
                }
            }

            if (state.sessions.recent.isNotEmpty()) {
                item { Header(stringResource(R.string.sources_recent)) }
                items(state.sessions.recent, key = { "recent:" + it.packageName }) { app ->
                    SessionRow(
                        session = app,
                        icon = state.icons[app.packageName],
                        isFocused = false,
                        onClick = { onRecentSelected(app.packageName) },
                    )
                }
            }

            item { Header(stringResource(R.string.sources_output)) }
            item {
                val label = state.track?.outputLabel?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.sources_output_unknown)
                FilledTonalButton(
                    onClick = onOutputClick,
                    modifier = Modifier.fillMaxWidth(),
                    icon = { RowIcon(outputIconRes(state.outputKind)) },
                    label = { Text(label, style = AkisType.listTitle, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                )
            }

            item {
                FilledTonalButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    icon = { RowIcon(R.drawable.ic_settings) },
                    label = { Text(stringResource(R.string.sources_settings), style = AkisType.listTitle) },
                )
            }
        }
    }
}

@Composable
private fun SessionRow(session: SessionSummary, icon: android.graphics.Bitmap?, isFocused: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current.accent
    val stateText = when (session.state) {
        PlaybackStatus.PLAYING -> stringResource(R.string.state_playing)
        PlaybackStatus.PAUSED -> stringResource(R.string.state_paused)
        else -> null
    }
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (isFocused) {
            ButtonDefaults.filledTonalButtonColors(containerColor = AkisColors.SurfaceHigh)
        } else {
            ButtonDefaults.filledTonalButtonColors()
        },
        icon = { CircleBitmap(icon, size = 28.dp) },
        label = {
            Text(
                text = session.title.ifBlank { session.appLabel },
                style = AkisType.listTitle.copy(color = if (isFocused) accent else AkisColors.OnSurface),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        secondaryLabel = {
            Text(
                text = listOfNotNull(session.appLabel.takeIf { session.title.isNotBlank() }, stateText).joinToString(" · "),
                style = AkisType.listSubtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun Header(text: String) {
    ListHeader { Text(text, style = AkisType.sectionHeader) }
}

@Composable
private fun Hint(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AkisColors.Surface, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text, style = AkisType.bodyDim)
    }
}

@Composable
private fun RowIcon(res: Int) {
    Icon(painter = painterResource(res), contentDescription = null, modifier = Modifier.size(22.dp))
}

private fun outputIconRes(kind: OutputKind): Int = when (kind) {
    OutputKind.HEADPHONES -> R.drawable.ic_headphones
    OutputKind.REMOTE -> R.drawable.ic_cast
    OutputKind.WATCH -> R.drawable.ic_watch
    OutputKind.PHONE -> R.drawable.ic_smartphone
}
