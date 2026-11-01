package com.erenkng.akis.wear.ui.nowplaying

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.erenkng.akis.wear.ui.theme.AkisType
import java.util.Locale

/** "YOUTUBE MUSIC": 14 dp ikon + 10 sp etiket (BRIEF Bölüm 6). */
@Composable
fun SourceLabel(appLabel: String, icon: Bitmap?, maxWidth: Dp, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.width(maxWidth),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            val image = remember(icon) { icon.asImageBitmap() }
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape),
            )
        }
        Text(
            // Uygulama adları çoğunlukla marka adı; Türkçe büyük harf kuralı "MUSİC" üretmesin diye ROOT.
            text = appLabel.uppercase(Locale.ROOT),
            style = AkisType.sourceLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Şarkı adı: 1 satır, 2 sn bekleyip başlayan kayan yazı. Sistem yazı boyutu büyükse
 * kayan yazı yerine 2 satır (BRIEF Bölüm 11, erişilebilirlik).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackTitle(title: String, width: Dp, largeText: Boolean, modifier: Modifier = Modifier) {
    if (largeText) {
        Text(
            text = title,
            style = AkisType.trackTitle,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.width(width),
        )
    } else {
        Text(
            text = title,
            style = AkisType.trackTitle,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = modifier
                .width(width)
                .basicMarquee(initialDelayMillis = 2_000),
        )
    }
}

@Composable
fun ArtistText(artist: String, width: Dp, modifier: Modifier = Modifier) {
    Text(
        text = artist,
        style = AkisType.artist,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.width(width),
    )
}
