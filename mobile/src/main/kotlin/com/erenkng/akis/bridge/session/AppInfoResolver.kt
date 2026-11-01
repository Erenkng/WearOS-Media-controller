package com.erenkng.akis.bridge.session

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.util.concurrent.ConcurrentHashMap

/** Paket adından kullanıcıya görünen uygulama adı ve ikonu. Sonuçlar süreç boyunca önbelleklenir. */
class AppInfoResolver(private val context: Context) {

    private val labels = ConcurrentHashMap<String, String>()

    fun label(packageName: String): String = labels.getOrPut(packageName) {
        val pm = context.packageManager
        runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
    }

    fun icon(packageName: String): Drawable? = try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}
