package com.erenkng.akis.bridge.sync

import android.util.Log
import com.erenkng.akis.bridge.session.AppInfoResolver
import com.erenkng.akis.bridge.session.RecentAppsList
import com.erenkng.akis.bridge.session.RecentAppsStore
import com.erenkng.akis.bridge.session.SessionTracker
import com.erenkng.akis.bridge.session.StateMapper
import com.erenkng.akis.bridge.session.TrackerSnapshot
import com.erenkng.akis.bridge.wear.SubscriptionState
import com.erenkng.akis.shared.codec.AkisJson
import com.erenkng.akis.shared.model.BridgeStatus
import com.erenkng.akis.shared.model.BridgeStatusCode
import com.erenkng.akis.shared.model.NowPlayingState
import com.erenkng.akis.shared.model.SessionSummary
import com.erenkng.akis.shared.model.SessionsPayload
import com.erenkng.akis.shared.model.SubscribeLevel
import com.erenkng.akis.shared.protocol.AkisDataKeys
import com.erenkng.akis.shared.protocol.AkisPaths
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Tracker değişikliklerini 150 ms debounce ile `/akis/state` ve `/akis/sessions` DataItem'larına yazar
 * (BRIEF Bölüm 10, madde 4). Ne zaman yazılacağına [PublishPolicy] karar verir.
 */
class StatePublisher(
    private val scope: CoroutineScope,
    private val dataClient: DataClient,
    private val tracker: SessionTracker,
    private val mapper: StateMapper,
    private val recentApps: RecentAppsStore,
    private val appInfo: AppInfoResolver,
    private val artwork: ArtworkEncoder,
    private val subscription: SubscriptionState,
    private val bridgeVersionCode: Int,
) {
    private val forceCounter = MutableStateFlow(0)

    // Yalnızca publish korutini (tek iş parçacıklı sıra) içinden okunur/yazılır.
    private var lastPublished: NowPlayingState? = null
    private var lastPublishedAtMs = 0L
    private var hasPublishedOnce = false
    private var lastArtHash: String? = null
    private var lastArtAsset: Asset? = null
    private var lastSessions: SessionsPayload? = null
    private var lastForce = 0
    private val publishedIcons = HashSet<String>()

    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch {
            combine(tracker.snapshot, recentApps.apps, forceCounter) { snapshot, _, force -> snapshot to force }
                .debounce(DEBOUNCE_MS)
                .conflate()
                .collect { (snapshot, force) ->
                    withContext(Dispatchers.Default) { publish(snapshot, force) }
                }
        }
        // Saat ekranı açılınca (FULL) güncel durumu hemen gönder.
        scope.launch {
            subscription.level.drop(1).distinctUntilChanged().collect { level ->
                if (level == SubscribeLevel.FULL) forcePublish()
            }
        }
    }

    fun forcePublish() {
        forceCounter.value += 1
    }

    fun publishBridgeStatus(code: BridgeStatusCode) {
        scope.launch {
            putJson(AkisPaths.BRIDGE, AkisJson.encode(BridgeStatus(code, bridgeVersionCode)), urgent = true)
        }
    }

    private suspend fun publish(snapshot: TrackerSnapshot, force: Int) {
        val forced = force != lastForce || !hasPublishedOnce
        lastForce = force
        val now = System.currentTimeMillis()

        val focusedId = snapshot.focusedId
        val focused = snapshot.focused
        val mapped = if (focusedId != null && focused != null) {
            runCatching { mapper.map(focusedId, focused) }
                .onFailure { Log.w(TAG, "Oturum okunamadı", it) }
                .getOrNull()
        } else {
            null
        }

        val next = mapped?.state
        if (PublishPolicy.shouldPublish(lastPublished, lastPublishedAtMs, next, now, subscription.level.value, forced)) {
            if (putState(next, mapped?.art)) {
                // Kabul kriteri ölçümü için (docs/TESTING.md, K1): saatteki "durum ←" satırıyla karşılaştırılır.
                Log.d(TAG, "durum → ${next?.title} [${next?.state}] art=${next?.artHash}")
                lastPublished = next
                lastPublishedAtMs = now
                hasPublishedOnce = true
            }
        }

        val active = snapshot.sessions.mapNotNull { (id, controller) ->
            runCatching { mapper.summary(id, controller) }.getOrNull()
        }
        val recent = RecentAppsList.visible(recentApps.apps.value, active.mapTo(HashSet()) { it.packageName })
            .map { SessionSummary(sessionId = "", packageName = it.packageName, appLabel = it.appLabel, title = it.lastTitle) }
        val sessions = SessionsPayload(active, recent)
        if (shouldPublishSessions(sessions, forced)) {
            if (putJson(AkisPaths.SESSIONS, AkisJson.encode(sessions), urgent = true)) lastSessions = sessions
        }

        (active + recent).map { it.packageName }.distinct().forEach { publishIconOnce(it) }
    }

    /** Ekran kapalıyken yalnızca oturum listesinin kendisi (kimler var) değişirse gönder. */
    private fun shouldPublishSessions(sessions: SessionsPayload, forced: Boolean): Boolean {
        val last = lastSessions ?: return true
        if (forced) return sessions != last
        if (subscription.level.value == SubscribeLevel.FULL) return sessions != last
        fun SessionsPayload.shape() = active.map { it.sessionId } to recent.map { it.packageName }
        return sessions.shape() != last.shape()
    }

    private suspend fun putState(state: NowPlayingState?, art: ArtworkEncoder.EncodedArt?): Boolean {
        val request = PutDataMapRequest.create(AkisPaths.STATE)
        if (state != null) request.dataMap.putByteArray(AkisDataKeys.JSON, AkisJson.encode(state))
        if (art != null) {
            // Aynı kapak için aynı Asset nesnesi: Data Layer içerik özetiyle tekilleştirir, yeniden aktarmaz.
            val asset = if (art.hash == lastArtHash) lastArtAsset else Asset.createFromBytes(art.bytes)
            request.dataMap.putAsset(AkisDataKeys.ART, asset!!)
            lastArtHash = art.hash
            lastArtAsset = asset
        }
        return put(request.asPutDataRequest().setUrgent())
    }

    private suspend fun publishIconOnce(packageName: String) {
        if (packageName in publishedIcons) return
        val drawable = appInfo.icon(packageName) ?: return
        val request = PutDataMapRequest.create(AkisPaths.icon(packageName))
        request.dataMap.putAsset(AkisDataKeys.ICON, Asset.createFromBytes(artwork.encodeIcon(drawable)))
        // Acil değilse Data Layer ikonu dakikalarca bekletebilir; saat o sürede yedek ikon gösterir.
        if (put(request.asPutDataRequest().setUrgent())) publishedIcons += packageName
    }

    private suspend fun putJson(path: String, bytes: ByteArray, urgent: Boolean): Boolean {
        val request = PutDataMapRequest.create(path)
        request.dataMap.putByteArray(AkisDataKeys.JSON, bytes)
        val put = request.asPutDataRequest()
        if (urgent) put.setUrgent()
        return put(put)
    }

    private suspend fun put(request: com.google.android.gms.wearable.PutDataRequest): Boolean = try {
        dataClient.putDataItem(request).await()
        true
    } catch (e: Exception) {
        // Wear OS eşlemesi yoksa ya da Play hizmetleri hazır değilse: sessizce atla, bir sonraki değişiklikte yeniden denenir.
        Log.w(TAG, "DataItem yazılamadı: ${request.uri}", e)
        false
    }

    private companion object {
        const val TAG = "StatePublisher"
        const val DEBOUNCE_MS = 150L
    }
}
