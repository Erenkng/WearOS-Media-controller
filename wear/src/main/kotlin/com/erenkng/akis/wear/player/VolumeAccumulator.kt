package com.erenkng.akis.wear.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Saatte gösterilen ses. */
data class VolumeDisplay(val volume: Int, val max: Int) {
    val isLocked: Boolean get() = max <= 0
}

enum class VolumeFeedback {
    /** Normal adım: çok hafif tık. */
    STEP,

    /** 0'a ya da maksimuma bu adımda ulaşıldı: belirgin tık. */
    LIMIT,

    /** Değişiklik yok (sınırda itmeye devam, ses bilinmiyor ya da kilitli): titreşim yok, tekrar etmez. */
    NONE,
}

/**
 * Döner çerçeve → ses (BRIEF Bölüm 8, "Döner giriş ayrıntıları"):
 *
 * - [windowMs] (120 ms) içindeki tıklar toplanır ve tek `ADJUST_VOLUME(delta)` olarak gönderilir.
 * - Gösterge **iyimser** güncellenir: tık anında değişir, telefonun ölçtüğü gerçek değer gelince düzeltilir.
 * - Komut başarısız olursa iyimser kısım geri alınır.
 *
 * Tek iş parçacığından (ana) kullanılmak üzere tasarlanmıştır.
 *
 * @param send deltayı gönderir; telefonun ölçtüğü gerçek sesi ya da başarısızsa null döner.
 */
class VolumeAccumulator(
    private val scope: CoroutineScope,
    private val windowMs: Long = WINDOW_MS,
    private val send: suspend (delta: Int) -> Int?,
) {
    private var confirmed: Int? = null
    private var max: Int = 0
    private var pending = 0
    private var inFlight = 0
    private var flushJob: Job? = null

    private val _display = MutableStateFlow<VolumeDisplay?>(null)
    val display: StateFlow<VolumeDisplay?> = _display

    /** Telefondan gelen durum. Saatte bekleyen değişiklik varken iyimser değeri ezmez. */
    fun onRemoteVolume(volume: Int, max: Int) {
        this.max = max
        if (pending == 0 && inFlight == 0) confirmed = volume
        publish()
    }

    fun onSteps(steps: Int): VolumeFeedback {
        if (steps == 0 || confirmed == null || max <= 0) return VolumeFeedback.NONE
        val current = shown()
        val target = (current + steps).coerceIn(0, max)
        val applied = target - current
        if (applied == 0) return VolumeFeedback.NONE

        pending += applied
        publish()
        if (flushJob == null) {
            flushJob = scope.launch {
                delay(windowMs)
                flush()
            }
        }
        return if (target == 0 || target == max) VolumeFeedback.LIMIT else VolumeFeedback.STEP
    }

    private suspend fun flush() {
        flushJob = null
        val delta = pending
        if (delta == 0) return
        pending = 0
        inFlight += delta
        val actual = runCatching { send(delta) }.getOrNull()
        inFlight -= delta
        if (actual != null) confirmed = actual
        publish()
    }

    private fun shown(): Int = ((confirmed ?: 0) + inFlight + pending).coerceIn(0, max.coerceAtLeast(0))

    private fun publish() {
        _display.value = confirmed?.let { VolumeDisplay(shown(), max) }
    }

    companion object {
        const val WINDOW_MS = 120L
    }
}
