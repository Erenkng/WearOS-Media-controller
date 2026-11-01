package com.erenkng.akis.shared.codec

import kotlinx.serialization.json.Json

/**
 * Protokolün tek serileştirme noktası. İleride protobuf'a geçilirse yalnızca bu dosya değişir.
 */
object AkisJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        classDiscriminator = "type"
    }

    inline fun <reified T> encode(value: T): ByteArray =
        json.encodeToString(value).encodeToByteArray()

    inline fun <reified T> decode(bytes: ByteArray): T =
        json.decodeFromString(bytes.decodeToString())

    /** Bozuk ya da eski sürümden gelen veri uygulamayı çökertmemeli. */
    inline fun <reified T> decodeOrNull(bytes: ByteArray?): T? =
        bytes?.let { runCatching { decode<T>(it) }.getOrNull() }
}
