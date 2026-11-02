package com.erenkng.akis.bridge.session

/** Odak seçimi için bir oturumun yalın hali (Android'den bağımsız, test edilebilir). */
data class FocusCandidate(
    val sessionId: String,
    val isPlaying: Boolean,
)

/**
 * "Odaklanan oturum" seçimi (BRIEF Bölüm 10, telefon tarafı akışı, madde 3):
 * 1. Kullanıcı saatten bir oturum seçtiyse o.
 * 2. Değilse listede ilk sıradaki **çalan** oturum (liste, sistemin öncelik sırasıdır).
 * 3. Hiçbiri çalmıyorsa en son çalmış olan.
 * 4. O da yoksa listenin ilki.
 */
object FocusPolicy {

    fun choose(
        candidates: List<FocusCandidate>,
        userSelectedId: String?,
        lastPlayingId: String?,
    ): String? {
        if (candidates.isEmpty()) return null
        val ids = candidates.mapTo(HashSet()) { it.sessionId }
        if (userSelectedId != null && userSelectedId in ids) return userSelectedId
        candidates.firstOrNull { it.isPlaying }?.let { return it.sessionId }
        if (lastPlayingId != null && lastPlayingId in ids) return lastPlayingId
        return candidates.first().sessionId
    }

    /**
     * Kullanıcı seçimi ne zaman bırakılır: seçilen oturum kapandığında ya da **başka** bir oturum
     * çalmaya başladığında. Böylece yeni başlayan müzik saatte kendiliğinden öne gelir.
     */
    fun shouldClearUserSelection(
        userSelectedId: String?,
        newlyPlayingId: String?,
        activeIds: Set<String>,
    ): Boolean {
        if (userSelectedId == null) return false
        if (userSelectedId !in activeIds) return true
        return newlyPlayingId != null && newlyPlayingId != userSelectedId
    }
}
