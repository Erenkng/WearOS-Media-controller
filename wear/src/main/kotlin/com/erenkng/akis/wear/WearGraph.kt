package com.erenkng.akis.wear

import android.content.Context
import androidx.wear.tiles.TileService
import com.erenkng.akis.wear.data.ArtworkCache
import com.erenkng.akis.wear.data.CommandSender
import com.erenkng.akis.wear.data.ConnectionMonitor
import com.erenkng.akis.wear.data.LastStateStore
import com.erenkng.akis.wear.data.PhoneRepository
import com.erenkng.akis.wear.data.WearPrefs
import com.erenkng.akis.wear.tile.MediaTileService
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Saat tarafının elle DI kabı. Uygulama süreci boyunca tek örnek. */
class WearGraph(context: Context) {

    val appContext: Context = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val dataClient = Wearable.getDataClient(appContext)
    private val messageClient = Wearable.getMessageClient(appContext)
    private val capabilityClient = Wearable.getCapabilityClient(appContext)
    private val nodeClient = Wearable.getNodeClient(appContext)

    val prefs = WearPrefs(appContext)
    val artworkCache = ArtworkCache()
    val lastStateStore = LastStateStore(appContext)
    val connection = ConnectionMonitor(scope, capabilityClient, nodeClient)
    val repository = PhoneRepository(scope, dataClient, artworkCache, lastStateStore)
    val commandSender = CommandSender(scope, messageClient, connection)

    init {
        repository.start()
        connection.start()
        commandSender.start()
        updateTileOnTrackChange()
    }

    /**
     * Tile canlı ilerleme göstermez (BRIEF Bölüm 9.1); yalnızca parça ya da oynatma durumu
     * değişince güncellenir. Sistem bu istekleri kendisi de seyreltir.
     */
    private fun updateTileOnTrackChange() {
        scope.launch {
            repository.nowPlaying
                .map { it.state?.trackKey }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    runCatching { TileService.getUpdater(appContext).requestUpdate(MediaTileService::class.java) }
                }
        }
    }
}
