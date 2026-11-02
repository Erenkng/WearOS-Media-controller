package com.erenkng.akis.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class BridgeStatusCode {
    /** Bildirim erişimi var, oturumlar okunuyor. */
    OK,

    /** Bildirim erişimi verilmemiş ya da geri alınmış. */
    PERMISSION_MISSING,
}

/** `/akis/bridge` gövdesi (PLAN K3). */
@Serializable
data class BridgeStatus(
    val status: BridgeStatusCode,
    val bridgeVersionCode: Int = 0,
)
