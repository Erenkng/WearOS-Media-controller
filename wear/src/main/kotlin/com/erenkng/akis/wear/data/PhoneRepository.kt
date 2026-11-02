package com.erenkng.akis.wear.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.erenkng.akis.shared.codec.AkisJson
import com.erenkng.akis.shared.model.BridgeStatus
import com.erenkng.akis.shared.model.NowPlayingState
import com.erenkng.akis.shared.model.SessionsPayload
import com.erenkng.akis.shared.protocol.AkisDataKeys
import com.erenkng.akis.shared.protocol.AkisPaths
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Şimdi çalan + kapak birlikte; ikisi ayrı akışlarda olsaydı şarkı değişiminde kapak bir an eski kalırdı. */
data class NowPlaying(
    val state: NowPlayingState? = null,
    val artwork: Bitmap? = null,
    /** Diskten okunan, henüz canlı veriyle doğrulanmamış durum. */
    val fromCache: Boolean = false,
)

/**
 * Telefondan gelen durumun tek kaynağı (BRIEF Bölüm 10: durum telefondan saate tek yönde akar).
 *
 * Kaynak sırası: önce disk (soğuk açılış), sonra Data Layer'ın yerel kopyası, sonra canlı değişiklikler.
 */
class PhoneRepository(
    private val scope: CoroutineScope,
    private val dataClient: DataClient,
    private val artworkCache: ArtworkCache,
    private val lastStateStore: LastStateStore,
) : DataClient.OnDataChangedListener {

    private val _nowPlaying = MutableStateFlow(NowPlaying())
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying

    private val _sessions = MutableStateFlow(SessionsPayload())
    val sessions: StateFlow<SessionsPayload> = _sessions

    private val _bridge = MutableStateFlow<BridgeStatus?>(null)
    val bridge: StateFlow<BridgeStatus?> = _bridge

    private val _icons = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val icons: StateFlow<Map<String, Bitmap>> = _icons

    private val _loaded = MutableStateFlow(false)

    /** Durum güncellemeleri sıralı uygulanır; geç gelen eski bir kapak yeni durumu ezmesin. */
    private val stateMutex = Mutex()

    fun start() {
        dataClient.addListener(this, Uri.parse("wear://*${AkisPaths.PREFIX}"), DataClient.FILTER_PREFIX)
        scope.launch(Dispatchers.IO) {
            restoreFromDisk()
            refreshFromDataLayer()
            _loaded.value = true
        }
    }

    /** Tile gibi kısa ömürlü çağıranlar için: ilk yükleme bitene kadar (en fazla [timeoutMs]) bekle. */
    suspend fun awaitLoaded(timeoutMs: Long = 600) {
        withTimeoutOrNull(timeoutMs) { _loaded.first { it } }
    }

    override fun onDataChanged(events: DataEventBuffer) {
        // Buffer bu metot dönünce serbest bırakılır; öğeler önce dondurulur.
        val items = events.filter { it.type == DataEvent.TYPE_CHANGED }.map { it.dataItem.freeze() }
        handleItems(items)
    }

    /** [WearDataListenerService] de (uygulama kapalıyken) buraya iletir. */
    fun handleItems(items: List<DataItem>) {
        if (items.isEmpty()) return
        scope.launch(Dispatchers.IO) { items.forEach { handle(it) } }
    }

    suspend fun refreshFromDataLayer() {
        try {
            val buffer = dataClient.getDataItems(Uri.parse("wear://*${AkisPaths.PREFIX}"), DataClient.FILTER_PREFIX).await()
            val items = try {
                buffer.map { it.freeze() }
            } finally {
                buffer.release()
            }
            items.forEach { handle(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Data Layer okunamadı", e)
        }
    }

    private suspend fun restoreFromDisk() {
        val snapshot = lastStateStore.read() ?: return
        val art = snapshot.artBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        val state = snapshot.state
        if (state != null && art != null) artworkCache.put(state.artHash, art)
        stateMutex.withLock {
            if (_nowPlaying.value.state == null) _nowPlaying.value = NowPlaying(state, art, fromCache = true)
        }
    }

    private suspend fun handle(item: DataItem) {
        val path = item.uri.path ?: return
        val dataMap = DataMapItem.fromDataItem(item).dataMap
        val json = dataMap.getByteArray(AkisDataKeys.JSON)
        when {
            path == AkisPaths.STATE -> handleState(
                state = AkisJson.decodeOrNull<NowPlayingState>(json),
                artAsset = dataMap.getAsset(AkisDataKeys.ART),
            )

            path == AkisPaths.SESSIONS ->
                _sessions.value = AkisJson.decodeOrNull<SessionsPayload>(json) ?: SessionsPayload()

            path == AkisPaths.BRIDGE ->
                _bridge.value = AkisJson.decodeOrNull<BridgeStatus>(json)

            else -> AkisPaths.packageFromIconPath(path)?.let { packageName ->
                val asset = dataMap.getAsset(AkisDataKeys.ICON) ?: return
                if (packageName in _icons.value) return
                loadAsset(asset)?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { bitmap ->
                        _icons.update { it + (packageName to bitmap) }
                    }
                }
            }
        }
    }

    private suspend fun handleState(state: NowPlayingState?, artAsset: Asset?) = stateMutex.withLock {
        var artBytes: ByteArray? = null
        val artwork: Bitmap? = when {
            state == null || state.artHash.isEmpty() -> null
            else -> artworkCache.get(state.artHash) ?: artAsset?.let { asset ->
                loadAsset(asset)?.also { artBytes = it }
                    ?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                    ?.also { artworkCache.put(state.artHash, it) }
            }
        }
        val previous = _nowPlaying.value
        _nowPlaying.value = NowPlaying(state, artwork, fromCache = false)
        Log.d(TAG, "durum ← ${state?.title} [${state?.state}] art=${artwork != null}")

        // Diske yalnızca parça değişince ya da kapak yeni geldiyse yaz.
        if (previous.fromCache || previous.state?.trackKey != state?.trackKey || artBytes != null) {
            lastStateStore.write(state, artBytes)
        }
    }

    private suspend fun loadAsset(asset: Asset): ByteArray? = try {
        dataClient.getFdForAsset(asset).await().inputStream.use { it.readBytes() }
    } catch (e: Exception) {
        Log.w(TAG, "Asset okunamadı", e)
        null
    }

    private companion object {
        const val TAG = "PhoneRepository"
    }
}
