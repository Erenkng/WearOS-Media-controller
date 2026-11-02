package com.erenkng.akis.wear

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import com.erenkng.akis.wear.ui.AkisApp

class MainActivity : ComponentActivity() {

    /** `akis://now` her geldiğinde artar; ekran Şimdi Çalıyor'a döner. */
    private val showNowPlaying = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AkisApp(graph = wearGraph, showNowPlayingSignal = showNowPlaying.intValue)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showNowPlaying.intValue += 1
    }
}
