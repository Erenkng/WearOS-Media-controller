package com.erenkng.akis.wear.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Saatteki küçük kalıcı tercihler. Faz 2'deki Ayarlar anahtarları da buraya eklenecek. */
class WearPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("akis", Context.MODE_PRIVATE)

    private val _onboardingWelcomeSeen = MutableStateFlow(prefs.getBoolean(KEY_WELCOME_SEEN, false))
    val onboardingWelcomeSeen: StateFlow<Boolean> = _onboardingWelcomeSeen

    fun markWelcomeSeen() {
        _onboardingWelcomeSeen.value = true
        prefs.edit { putBoolean(KEY_WELCOME_SEEN, true) }
    }

    /** Tile tıklamalarını tekilleştirmek için son işlenen tıklama kimliği (bkz. MediaTileService). */
    var lastHandledTileClick: String
        get() = prefs.getString(KEY_TILE_CLICK, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_TILE_CLICK, value) }

    private companion object {
        const val KEY_WELCOME_SEEN = "onboarding_welcome_seen"
        const val KEY_TILE_CLICK = "tile_last_click"
    }
}
