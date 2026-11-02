package com.erenkng.akis.wear.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import com.erenkng.akis.wear.R
import com.erenkng.akis.wear.ui.theme.AkisColors

/**
 * Listelerde daire kırpılmış görsel. Horologist kuralı: kapak/ikon için `Icon` değil
 * `Image` + daire + `ContentScale.Crop` (BRIEF Bölüm 4, "Şekil dili").
 */
@Composable
fun CircleBitmap(bitmap: Bitmap?, size: Dp = 32.dp, modifier: Modifier = Modifier) {
    if (bitmap != null) {
        val image = remember(bitmap) { bitmap.asImageBitmap() }
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(AkisColors.SurfaceHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_music_note),
                contentDescription = null,
                tint = AkisColors.OnSurfaceDim,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

fun Context.toast(@StringRes message: Int) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
