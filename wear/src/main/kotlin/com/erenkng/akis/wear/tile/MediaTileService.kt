package com.erenkng.akis.wear.tile

import android.graphics.Bitmap
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.erenkng.akis.shared.model.Command
import com.erenkng.akis.shared.model.NowPlayingState
import com.erenkng.akis.shared.model.PlaybackStatus
import com.erenkng.akis.wear.wearGraph
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Medya tile'ı (BRIEF Bölüm 9.1).
 *
 * Buton → `LoadAction` + tıklama kimliği → `onTileRequest` içinde `lastClickableId` okunur → komut
 * telefona gider → iyimser düzen döner. Canlı ilerleme yoktur; parça değişince [com.erenkng.akis.wear.WearGraph]
 * güncelleme ister.
 *
 * Tıklama kimlikleri her düzende benzersiz bir nesil numarası taşır ("play_pause:1730000000000").
 * Böylece sistem aynı `lastClickableId` ile tekrar istek yaparsa (ör. bizim güncelleme isteğimiz)
 * komut ikinci kez gönderilmez.
 */
class MediaTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        future { buildTile(requestParams) }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> = future { buildResources(requestParams.version) }

    private suspend fun buildTile(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        val graph = wearGraph
        graph.repository.awaitLoaded()
        val nowPlaying = graph.repository.nowPlaying.value
        var state = nowPlaying.state

        val clicked = requestParams.currentState.lastClickableId
        if (clicked.isNotEmpty() && clicked != graph.prefs.lastHandledTileClick) {
            graph.prefs.lastHandledTileClick = clicked
            when (TileClicks.action(clicked)) {
                TileClicks.PLAY_PAUSE -> {
                    graph.commandSender.fire(Command.Toggle)
                    state = state?.copy(
                        state = if (state.isPlaying) PlaybackStatus.PAUSED else PlaybackStatus.PLAYING,
                    )
                }
                TileClicks.NEXT -> graph.commandSender.fire(Command.Next)
                TileClicks.PREVIOUS -> graph.commandSender.fire(Command.Prev)
                TileClicks.RESUME -> graph.commandSender.fire(Command.ResumeLast())
            }
        }

        val hasArt = nowPlaying.artwork != null && state?.artHash?.isNotEmpty() == true
        val layout = MediaTileLayout.build(
            context = this,
            deviceConfiguration = requestParams.deviceConfiguration,
            state = state,
            hasArt = hasArt,
            generation = System.currentTimeMillis(),
        )
        return TileBuilders.Tile.Builder()
            .setResourcesVersion(resourcesVersion(state, hasArt))
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
            .build()
    }

    private suspend fun buildResources(version: String): ResourceBuilders.Resources {
        val builder = ResourceBuilders.Resources.Builder().setVersion(version)
        MediaTileLayout.ICONS.forEach { (id, res) -> builder.addIdToImageMapping(id, androidImage(res)) }

        val art = wearGraph.repository.nowPlaying.value.artwork
        if (art != null) {
            val png = withContext(Dispatchers.Default) { art.toTilePng() }
            builder.addIdToImageMapping(
                MediaTileLayout.ART_ID,
                ResourceBuilders.ImageResource.Builder()
                    .setInlineResource(
                        ResourceBuilders.InlineImageResource.Builder()
                            .setData(png)
                            .setWidthPx(TILE_ART_PX)
                            .setHeightPx(TILE_ART_PX)
                            .build(),
                    )
                    .build(),
            )
        }
        return builder.build()
    }

    private fun resourcesVersion(state: NowPlayingState?, hasArt: Boolean): String =
        "v1:" + (if (hasArt) state?.artHash.orEmpty() else "none")

    private fun androidImage(res: Int) = ResourceBuilders.ImageResource.Builder()
        .setAndroidResourceByResId(ResourceBuilders.AndroidImageResourceByResId.Builder().setResourceId(res).build())
        .build()

    private fun Bitmap.toTilePng(): ByteArray {
        val scaled = Bitmap.createScaledBitmap(this, TILE_ART_PX, TILE_ART_PX, true)
        return ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
    }

    /** Tile istekleri ana iş parçacığında, grafiğin kapsamında çözülür. */
    private fun <T : Any> future(block: suspend CoroutineScope.() -> T): ListenableFuture<T> =
        CallbackToFutureAdapter.getFuture { completer ->
            val job = wearGraph.scope.launch {
                try {
                    completer.set(block())
                } catch (t: Throwable) {
                    completer.setException(t)
                }
            }
            completer.addCancellationListener({ job.cancel() }, Runnable::run)
            "MediaTileService"
        }

    private companion object {
        const val TILE_ART_PX = 96
    }
}

/** Tile tıklama kimlikleri. */
object TileClicks {
    const val PLAY_PAUSE = "play_pause"
    const val NEXT = "next"
    const val PREVIOUS = "prev"
    const val RESUME = "resume"

    fun id(action: String, generation: Long) = "$action:$generation"

    fun action(clickableId: String): String = clickableId.substringBefore(':')
}
