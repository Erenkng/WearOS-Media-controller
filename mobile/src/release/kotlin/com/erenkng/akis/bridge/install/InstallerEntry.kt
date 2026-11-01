package com.erenkng.akis.bridge.install

import android.content.Context

/**
 * Release: saate kablosuz kurulum yok. Saat uygulaması Play Store üzerinden kurulur ve uygulama
 * internet izni taşımaz (BRIEF Bölüm 11). Debug karşılığı: `src/debug/.../InstallerEntry.kt`.
 */
object InstallerEntry {
    const val isAvailable: Boolean = false

    @Suppress("UNUSED_PARAMETER")
    fun open(context: Context) = Unit
}
