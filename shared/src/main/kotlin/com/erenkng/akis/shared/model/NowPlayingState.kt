package com.erenkng.akis.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class PlaybackStatus { PLAYING, PAUSED, BUFFERING, STOPPED, ERROR }

@Serializable
enum class RepeatMode { NONE, ONE, ALL }

/**
 * Uygulamanın kendi özel eylemi (ör. "Beğen"). Faz 2'deki ⋯ menüsünde listelenir.
 *
 * @property iconPng Base64 kodlu PNG. ByteArray yerine String kullanılır; böylece
 * data class eşitliği içerikle çalışır ve değişiklik tespiti bozulmaz.
 */
@Serializable
data class CustomActionInfo(
    val id: String,
    val label: String,
    val iconPng: String? = null,
)

/**
 * Odaklanan medya oturumunun tam durumu (BRIEF Bölüm 10). `/akis/state` ile gönderilir.
 *
 * Alanlar brief'teki listeyle birebir aynıdır. Sözleşmeler:
 * - `positionUpdatedAtEpochMs`: telefonun duvar saatine göre (epoch ms), `elapsedRealtime` değil.
 * - `volumeMax == 0`: oturumun sesi değiştirilemiyor (sabit ses). Saat kilit ikonu gösterir.
 * - `durationMs <= 0`: süre bilinmiyor (canlı yayın). Halka yerine "CANLI" gösterilir, sarma kapalıdır.
 * - `artHash` boş: kapak yok. Saat uygulama ikonunu kullanır.
 */
@Serializable
data class NowPlayingState(
    val sessionId: String,
    val packageName: String,
    val appLabel: String,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val durationMs: Long = 0,
    val positionMs: Long = 0,
    val positionUpdatedAtEpochMs: Long = 0,
    val speed: Float = 1f,
    val state: PlaybackStatus = PlaybackStatus.STOPPED,
    val actions: Long = 0,
    val customActions: List<CustomActionInfo> = emptyList(),
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.NONE,
    val volume: Int = 0,
    val volumeMax: Int = 0,
    val isRemote: Boolean = false,
    val outputLabel: String = "",
    val artHash: String = "",
) {
    val isPlaying: Boolean get() = state == PlaybackStatus.PLAYING

    val isLive: Boolean get() = durationMs <= 0

    val isVolumeControllable: Boolean get() = volumeMax > 0

    /**
     * Parça kimliği: bu değişirse "şarkı değişti" sayılır (tile, Ongoing Activity, ekran kapalıyken gönderim).
     * Oynatma durumu da dahildir, çünkü tile oynat/duraklat ikonunu gösterir.
     */
    val trackKey: TrackKey
        get() = TrackKey(sessionId, title, artist, album, artHash, durationMs, state, actions)
}

@Serializable
data class TrackKey(
    val sessionId: String,
    val title: String,
    val artist: String,
    val album: String,
    val artHash: String,
    val durationMs: Long,
    val state: PlaybackStatus,
    val actions: Long,
)
