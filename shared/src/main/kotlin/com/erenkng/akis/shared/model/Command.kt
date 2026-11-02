package com.erenkng.akis.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Saatten telefona giden komutlar (BRIEF Bölüm 10). Kablodaki adlar brief'teki türlerle birebir aynıdır.
 * Kabloda her zaman bir [CommandEnvelope] içinde, artan `seq` ile gider.
 */
@Serializable
sealed interface Command {
    @Serializable @SerialName("PLAY")
    data object Play : Command

    @Serializable @SerialName("PAUSE")
    data object Pause : Command

    @Serializable @SerialName("TOGGLE")
    data object Toggle : Command

    @Serializable @SerialName("NEXT")
    data object Next : Command

    @Serializable @SerialName("PREV")
    data object Prev : Command

    @Serializable @SerialName("SEEK_TO")
    data class SeekTo(val positionMs: Long) : Command

    @Serializable @SerialName("SEEK_BY")
    data class SeekBy(val deltaMs: Long) : Command

    @Serializable @SerialName("SET_VOLUME")
    data class SetVolume(val volume: Int) : Command

    @Serializable @SerialName("ADJUST_VOLUME")
    data class AdjustVolume(val delta: Int) : Command

    @Serializable @SerialName("SKIP_TO_QUEUE_ITEM")
    data class SkipToQueueItem(val queueItemId: Long) : Command

    @Serializable @SerialName("CUSTOM")
    data class Custom(val actionId: String) : Command

    @Serializable @SerialName("SET_SHUFFLE")
    data class SetShuffle(val enabled: Boolean) : Command

    @Serializable @SerialName("SET_REPEAT")
    data class SetRepeat(val mode: RepeatMode) : Command

    @Serializable @SerialName("FOCUS_SESSION")
    data class FocusSession(val sessionId: String) : Command

    /**
     * Aktif oturum yokken çalmayı sürdürür.
     * @property packageName null ise sistem son medya uygulamasını seçer.
     * Doluysa ("Son kullanılanlar" satırı) oynat tuşu o uygulamaya gönderilir.
     */
    @Serializable @SerialName("RESUME_LAST")
    data class ResumeLast(val packageName: String? = null) : Command

    @Serializable @SerialName("OPEN_ON_PHONE")
    data object OpenOnPhone : Command

    /**
     * Uyku zamanlayıcısı (Faz 2).
     * @property minutes 0 = iptal, [END_OF_ITEM] = "Bölüm bitince".
     */
    @Serializable @SerialName("SLEEP_TIMER")
    data class SleepTimer(val minutes: Int) : Command {
        companion object {
            const val END_OF_ITEM = -1
        }
    }
}

@Serializable
data class CommandEnvelope(
    val seq: Long,
    val command: Command,
)

/** `/akis/ack` gövdesi. `volume`, ses komutlarından sonra telefonda ölçülen gerçek değerdir. */
@Serializable
data class CommandAck(
    val seq: Long,
    val ok: Boolean,
    val volume: Int? = null,
)

/** Saatin ekran durumuna göre istediği veri yoğunluğu. */
@Serializable
enum class SubscribeLevel {
    /** Ekran açık: tam durum, ses, pozisyon düzeltmeleri. */
    FULL,

    /** Ambient (Faz 2): yalnızca parça değişimi. */
    TRACK_ONLY,

    /** Ekran kapalı / uygulama arka planda: yalnızca parça değişimi (tile güncellemesi için). */
    NONE,
}

@Serializable
data class SubscribeRequest(val level: SubscribeLevel)
