package com.erenkng.akis.bridge.wear

import com.erenkng.akis.shared.model.SubscribeLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Saatin istediği veri yoğunluğu (`/akis/subscribe`). Varsayılan NONE: saat ekranı açılınca
 * FULL ister. Bu sayede saat uygulaması hiç açılmazsa telefon yalnızca parça değişimlerini yollar.
 */
class SubscriptionState {
    private val _level = MutableStateFlow(SubscribeLevel.NONE)
    val level: StateFlow<SubscribeLevel> = _level

    fun set(level: SubscribeLevel) {
        _level.value = level
    }
}
