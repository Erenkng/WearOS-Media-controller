package com.erenkng.akis.bridge.install

import android.content.Context
import android.content.Intent

/**
 * Debug: "Saate yükle" ekranı. Saat APK'sı bu APK'nın içinde gelir (`assets/wear/akis-wear.apk`) ve
 * saate kablosuz ADB ile kurulur. Release karşılığı: `src/release/.../InstallerEntry.kt`.
 */
object InstallerEntry {
    const val isAvailable: Boolean = true

    fun open(context: Context) {
        context.startActivity(Intent(context, WatchInstallActivity::class.java))
    }
}
