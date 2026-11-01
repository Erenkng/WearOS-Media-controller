package com.erenkng.akis.wear.ui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * BRIEF Bölüm 8, titreşim tablosu. Her jest farklı hissettirir; kullanıcı ekrana bakmadan
 * ne olduğunu anlayabilsin.
 *
 * Faz 2 kancası: "Titreşim yoğunluğu" (Kapalı / Hafif / Normal) ayarı buraya bağlanacak.
 */
class Haptics(context: Context, private val view: View) {

    private val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator

    fun play() = predefined(VibrationEffect.EFFECT_CLICK)

    fun pause() = predefined(VibrationEffect.EFFECT_TICK)

    fun next() = predefined(VibrationEffect.EFFECT_DOUBLE_CLICK)

    fun previous() = predefined(VibrationEffect.EFFECT_HEAVY_CLICK)

    fun volumeStep() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        view.performHapticFeedback(constant)
    }

    fun volumeLimit() = predefined(VibrationEffect.EFFECT_HEAVY_CLICK)

    /** Hata / desteklenmeyen eylem: çift kısa titreşim. */
    fun error() {
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 70, 30), -1))
    }

    private fun predefined(effect: Int) {
        vibrator?.vibrate(VibrationEffect.createPredefined(effect))
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) { Haptics(context, view) }
}
