package com.erenkng.akis.wear.ui.common

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Oynatıcıdaki düz saat ("9:30"). Sistem `TimeText`'i kavisli ve hap arka planlı olduğu için oynatıcı
 * ekranlarında kullanılmaz (kullanıcının referans tasarımı). Dakika başında güncellenir; 12/24 saat
 * biçimi sistem ayarını izler.
 */
@Composable
fun rememberClockText(): String {
    val context = LocalContext.current
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = System.currentTimeMillis()
            delay(MINUTE_MS - current % MINUTE_MS + 50)
            now = System.currentTimeMillis()
        }
    }
    return remember(now, pattern) { SimpleDateFormat(pattern, Locale.getDefault()).format(Date(now)) }
}

private const val MINUTE_MS = 60_000L
