package com.erenkng.akis.shared.model

import kotlinx.serialization.Serializable

/** Kaynaklar panelindeki bir satır. */
@Serializable
data class SessionSummary(
    val sessionId: String,
    val packageName: String,
    val appLabel: String,
    val title: String = "",
    val state: PlaybackStatus = PlaybackStatus.STOPPED,
)

/**
 * `/akis/sessions` gövdesi.
 *
 * Brief'teki `SessionSummary[]` aktif listesine ek olarak `recent` alanını taşır (PLAN K2).
 * `recent` satırlarında oturum kapanmış olduğu için `sessionId` boştur.
 */
@Serializable
data class SessionsPayload(
    val active: List<SessionSummary> = emptyList(),
    val recent: List<SessionSummary> = emptyList(),
)
