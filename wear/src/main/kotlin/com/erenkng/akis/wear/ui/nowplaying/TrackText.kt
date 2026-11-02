package com.erenkng.akis.wear.ui.nowplaying

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text

/** Kapak üzerinde okunabilirlik için yumuşak gölge (açık kapaklarda beyaz metin kaybolmasın). */
private val TextShadow = Shadow(color = Color.Black.copy(alpha = 0.55f), blurRadius = 8f)

/**
 * Şarkı adı: 1 satır, 2 sn bekleyip başlayan kayan yazı. Sistem yazı boyutu büyükse kayan yazı yerine
 * 2 satır (BRIEF Bölüm 11, erişilebilirlik).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackTitle(title: String, style: TextStyle, width: Dp, largeText: Boolean, modifier: Modifier = Modifier) {
    val shadowed = style.copy(shadow = TextShadow)
    if (largeText) {
        Text(
            text = title,
            style = shadowed,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.width(width),
        )
    } else {
        Text(
            text = title,
            style = shadowed,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = modifier
                .width(width)
                .basicMarquee(initialDelayMillis = 2_000),
        )
    }
}

/**
 * Sanatçı satırı: kaynak uygulamanın ikonu + vurgu renginde sanatçı adı (referans tasarım). İkon, çok
 * uygulamalı Akış'ta müziğin nereden geldiğini ayrı bir etiket satırı olmadan gösterir.
 */
@Composable
fun ArtistLine(
    artist: String,
    appIcon: Bitmap?,
    color: Color,
    style: TextStyle,
    iconSize: Dp,
    maxWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.widthIn(max = maxWidth),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (appIcon != null) {
            val image = remember(appIcon) { appIcon.asImageBitmap() }
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape),
            )
        }
        Text(
            text = artist,
            style = style.copy(color = color, shadow = TextShadow),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Saat ve kısa durum metinleri için gölgeli düz metin. */
@Composable
fun ShadowedText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    Text(text = text, style = style.copy(shadow = TextShadow), maxLines = 1, modifier = modifier)
}
