package com.erenkng.akis.shared.protocol

/**
 * Wearable Data Layer yolları (BRIEF Bölüm 10, "Protokol").
 *
 * Durum telefondan saate tek yönde akar (DataItem). Saat yalnızca komut gönderir (Message).
 */
object AkisPaths {
    const val PREFIX = "/akis"

    /** DataItem, telefon → saat. Odaklanan oturumun [com.erenkng.akis.shared.model.NowPlayingState]'i + `art` Asset'i. */
    const val STATE = "/akis/state"

    /** DataItem, telefon → saat. [com.erenkng.akis.shared.model.SessionsPayload] (aktif + son kullanılanlar). */
    const val SESSIONS = "/akis/sessions"

    /** DataItem, telefon → saat. [com.erenkng.akis.shared.model.BridgeStatus] (PLAN K3). */
    const val BRIDGE = "/akis/bridge"

    /** DataItem öneki, telefon → saat. `/akis/icon/<paket>` + `icon` Asset'i, 48 px PNG (PLAN K2). */
    const val ICON_PREFIX = "/akis/icon/"

    /** Message, saat → telefon. [com.erenkng.akis.shared.model.CommandEnvelope]. */
    const val CMD = "/akis/cmd"

    /** Message, saat → telefon. [com.erenkng.akis.shared.model.SubscribeRequest]. */
    const val SUBSCRIBE = "/akis/subscribe"

    /** Message, telefon → saat. [com.erenkng.akis.shared.model.CommandAck]. */
    const val ACK = "/akis/ack"

    fun icon(packageName: String): String = ICON_PREFIX + packageName

    fun packageFromIconPath(path: String): String? =
        if (path.startsWith(ICON_PREFIX)) path.removePrefix(ICON_PREFIX).takeIf { it.isNotEmpty() } else null
}

object AkisCapabilities {
    /** Telefondaki köprü uygulamasının ilan ettiği capability (`res/values/wear.xml`). */
    const val BRIDGE = "akis_bridge"

    /** Saatteki Akış uygulamasının ilan ettiği capability; telefon "saatte kurulu mu" bilgisini buradan alır. */
    const val WATCH = "akis_watch"
}

/** DataItem içindeki DataMap anahtarları. */
object AkisDataKeys {
    /** JSON gövde (ByteArray). */
    const val JSON = "json"

    /** `/akis/state` içindeki kapak sanatı Asset'i (256×256 WebP). */
    const val ART = "art"

    /** `/akis/icon/<paket>` içindeki uygulama ikonu Asset'i (48 px PNG). */
    const val ICON = "icon"
}
