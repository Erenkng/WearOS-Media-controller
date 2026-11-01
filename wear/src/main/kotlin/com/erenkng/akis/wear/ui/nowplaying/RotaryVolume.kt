package com.erenkng.akis.wear.ui.nowplaying

import android.view.ViewConfiguration
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import com.erenkng.akis.wear.player.RotaryStepper

/**
 * Döner çerçeve = ses (BRIEF Bölüm 6 ve 8).
 *
 * Compose'da çerçeve olayları yalnızca odaktaki öğeye gelir: sayfa aktif olunca odak **otomatik** istenir.
 * Galaxy Watch'un fiziksel ve dokunmatik çerçevesi aynı API'den gelir (doğrulanacak: OPEN_QUESTIONS S3).
 */
@Composable
fun Modifier.rotaryVolume(active: Boolean, onSteps: (Int) -> Unit): Modifier {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val stepper = remember(context) {
        // Bir çerçeve tıkı ≈ bir "scroll factor" piksel; küçük pay, yuvarlama farklarını yutar.
        RotaryStepper(pixelsPerStep = ViewConfiguration.get(context).scaledVerticalScrollFactor * 0.9f)
    }
    val currentOnSteps = rememberUpdatedState(onSteps)

    LaunchedEffect(active) {
        if (active) runCatching { focusRequester.requestFocus() } else stepper.reset()
    }

    return this
        .onRotaryScrollEvent { event ->
            val steps = stepper.onScroll(event.verticalScrollPixels)
            if (steps != 0) currentOnSteps.value(steps)
            true
        }
        .focusRequester(focusRequester)
        .focusable()
}
