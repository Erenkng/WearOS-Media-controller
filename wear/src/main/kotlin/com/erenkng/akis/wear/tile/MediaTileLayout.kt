package com.erenkng.akis.wear.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.avatarImage
import androidx.wear.protolayout.material3.buttonGroup
import androidx.wear.protolayout.material3.icon
import androidx.wear.protolayout.material3.iconButton
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.types.layoutString
import com.erenkng.akis.shared.model.NowPlayingState
import com.erenkng.akis.wear.MainActivity
import com.erenkng.akis.wear.R
import java.util.Locale

/**
 * Tile düzeni (BRIEF Bölüm 9.1): üstte kaynak etiketi; ortada daire kapak (48 dp) + şarkı adı + sanatçı;
 * altında 3 buton (önceki · oynat/duraklat · sonraki); en altta kenara yaslanan "Aç" hap butonu.
 */
object MediaTileLayout {
    const val ART_ID = "art"
    private const val ICON_PLAY = "ic_play"
    private const val ICON_PAUSE = "ic_pause"
    private const val ICON_NEXT = "ic_next"
    private const val ICON_PREV = "ic_prev"

    /** Tile kaynakları: kimlik → drawable. */
    val ICONS: Map<String, Int> = mapOf(
        ICON_PLAY to R.drawable.ic_play_arrow,
        ICON_PAUSE to R.drawable.ic_pause,
        ICON_NEXT to R.drawable.ic_skip_next,
        ICON_PREV to R.drawable.ic_skip_previous,
    )

    fun build(
        context: Context,
        deviceConfiguration: DeviceParametersBuilders.DeviceParameters,
        state: NowPlayingState?,
        hasArt: Boolean,
        generation: Long,
    ): LayoutElementBuilders.LayoutElement = materialScope(context, deviceConfiguration) {
        if (state == null) {
            emptyLayout(context, generation)
        } else {
            playerLayout(context, state, hasArt, generation)
        }
    }

    private fun MaterialScope.playerLayout(
        context: Context,
        state: NowPlayingState,
        hasArt: Boolean,
        generation: Long,
    ): LayoutElementBuilders.LayoutElement = primaryLayout(
        titleSlot = { text(state.appLabel.uppercase(Locale.ROOT).layoutString, typography = Typography.LABEL_SMALL) },
        mainSlot = {
            val column = LayoutElementBuilders.Column.Builder()
                .setWidth(expand())
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            if (hasArt) {
                column.addContent(avatarImage(protoLayoutResourceId = ART_ID, width = dp(48f), height = dp(48f)))
                column.addContent(spacer(4f))
            }
            column.addContent(text(state.title.layoutString, typography = Typography.TITLE_SMALL, maxLines = 1))
            if (state.artist.isNotBlank()) {
                column.addContent(text(state.artist.layoutString, typography = Typography.BODY_SMALL, maxLines = 1))
            }
            column.addContent(spacer(6f))
            column.addContent(
                buttonGroup {
                    buttonGroupItem {
                        iconButton(
                            onClick = loadClick(TileClicks.id(TileClicks.PREVIOUS, generation)),
                            iconContent = { icon(ICON_PREV) },
                        )
                    }
                    buttonGroupItem {
                        iconButton(
                            onClick = loadClick(TileClicks.id(TileClicks.PLAY_PAUSE, generation)),
                            iconContent = { icon(if (state.isPlaying) ICON_PAUSE else ICON_PLAY) },
                        )
                    }
                    buttonGroupItem {
                        iconButton(
                            onClick = loadClick(TileClicks.id(TileClicks.NEXT, generation)),
                            iconContent = { icon(ICON_NEXT) },
                        )
                    }
                },
            )
            column.build()
        },
        bottomSlot = {
            textEdgeButton(
                onClick = launchAppClick(context),
                labelContent = { text(context.getString(R.string.tile_open).layoutString) },
            )
        },
    )

    private fun MaterialScope.emptyLayout(context: Context, generation: Long): LayoutElementBuilders.LayoutElement =
        primaryLayout(
            titleSlot = { text(context.getString(R.string.app_name).layoutString, typography = Typography.LABEL_SMALL) },
            mainSlot = { text(context.getString(R.string.tile_empty).layoutString, typography = Typography.BODY_MEDIUM, maxLines = 2) },
            bottomSlot = {
                textEdgeButton(
                    onClick = loadClick(TileClicks.id(TileClicks.RESUME, generation)),
                    labelContent = { text(context.getString(R.string.empty_resume).layoutString) },
                )
            },
        )

    private fun spacer(heightDp: Float): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Spacer.Builder().setHeight(dp(heightDp)).build()

    /** Tile'ı yeniden ister; `onTileRequest` tıklama kimliğini okuyup komutu gönderir. */
    private fun loadClick(id: String): ModifiersBuilders.Clickable =
        ModifiersBuilders.Clickable.Builder()
            .setId(id)
            .setOnClick(ActionBuilders.LoadAction.Builder().build())
            .build()

    /** "Aç": doğrudan Şimdi Çalıyor (BRIEF Bölüm 5). */
    private fun launchAppClick(context: Context): ModifiersBuilders.Clickable =
        ModifiersBuilders.Clickable.Builder()
            .setId("open")
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(context.packageName)
                            .setClassName(MainActivity::class.java.name)
                            .build(),
                    )
                    .build(),
            )
            .build()
}
