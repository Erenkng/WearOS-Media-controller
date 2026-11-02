package com.erenkng.akis.bridge.session

import android.content.Context
import androidx.core.content.edit
import com.erenkng.akis.shared.codec.AkisJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Oturumu kapanan medya uygulamaları; `SharedPreferences` içinde JSON olarak saklanır. */
class RecentAppsStore(context: Context) {

    private val prefs = context.getSharedPreferences("recent_apps", Context.MODE_PRIVATE)

    private val _apps = MutableStateFlow(load())
    val apps: StateFlow<List<RecentApp>> = _apps

    @Synchronized
    fun record(app: RecentApp) {
        val updated = RecentAppsList.push(_apps.value, app)
        if (updated == _apps.value) return
        _apps.value = updated
        prefs.edit { putString(KEY, AkisJson.json.encodeToString(updated)) }
    }

    private fun load(): List<RecentApp> = prefs.getString(KEY, null)
        ?.let { runCatching { AkisJson.json.decodeFromString<List<RecentApp>>(it) }.getOrNull() }
        .orEmpty()

    private companion object {
        const val KEY = "apps"
    }
}
