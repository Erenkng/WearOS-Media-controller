package com.erenkng.akis.wear.data

enum class ConnectionStatus {
    /** Henüz ölçülmedi (soğuk açılış). Önbellekteki son durum gösterilir. */
    UNKNOWN,

    /** Köprü kurulu ve erişilebilir. */
    CONNECTED,

    /** Telefona ulaşılamıyor (Bluetooth kapalı, uzakta). */
    UNREACHABLE,

    /** Telefon bağlı ama Akış Köprü kurulu değil → onboarding 2. adım. */
    BRIDGE_MISSING,
}

object ConnectionPolicy {
    /**
     * @param reachableBridgeNodes `akis_bridge` capability'si olan ve şu an erişilebilir düğümler.
     * @param knownBridgeNodes aynı capability, erişilemeyenler dahil.
     * @param connectedNodes capability'den bağımsız, bağlı tüm düğümler.
     */
    fun evaluate(reachableBridgeNodes: Int, knownBridgeNodes: Int, connectedNodes: Int): ConnectionStatus = when {
        reachableBridgeNodes > 0 -> ConnectionStatus.CONNECTED
        knownBridgeNodes > 0 -> ConnectionStatus.UNREACHABLE
        connectedNodes > 0 -> ConnectionStatus.BRIDGE_MISSING
        else -> ConnectionStatus.UNREACHABLE
    }
}
