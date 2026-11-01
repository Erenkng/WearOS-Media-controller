package com.erenkng.akis.wear.ui.nowplaying

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.curvedText
import com.erenkng.akis.shared.format.DurationFormat
import com.erenkng.akis.wear.ui.theme.AkisType

/** Alt kavisli süre: "1:12 · −2:33" (BRIEF Bölüm 6). Yalnızca bu bileşen pozisyonla yeniden çizilir. */
@Composable
fun CurvedDuration(position: State<Long>, durationMs: Long, modifier: Modifier = Modifier) {
    // 250 ms'lik tikler metni her seferinde değiştirmez; derivedStateOf yalnızca saniye değişince recomposition yapar.
    val text by remember(position, durationMs) {
        derivedStateOf { DurationFormat.elapsedAndRemaining(position.value, durationMs) }
    }
    CurvedBottomText(text = text, style = AkisType.duration, modifier = modifier)
}

@Composable
fun CurvedBottomText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    CurvedLayout(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        anchor = 90f,
        angularDirection = CurvedDirection.Angular.CounterClockwise,
    ) {
        curvedText(text = text, style = CurvedTextStyle(style))
    }
}
