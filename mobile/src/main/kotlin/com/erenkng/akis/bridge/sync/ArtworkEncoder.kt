package com.erenkng.akis.bridge.sync

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.net.Uri
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/**
 * Kapak: 256×256 WebP (%80), ~15–25 KB (BRIEF Bölüm 10, "Kapak").
 * Uygulama ikonu: 48 px PNG (PLAN K2).
 */
class ArtworkEncoder(private val context: Context) {

    class EncodedArt(val hash: String, val bytes: ByteArray)

    private data class CacheKey(val sessionId: String, val bitmapIdentity: Int, val generationId: Int)

    private var lastKey: CacheKey? = null
    private var lastArt: EncodedArt? = null

    /** Ana iş parçacığı dışında çağrılmalı (dekode/encode). */
    @Synchronized
    fun encode(sessionId: String, metadata: MediaMetadata?): EncodedArt? {
        val source = metadata?.let { bitmapFrom(it) } ?: return null
        val key = CacheKey(sessionId, System.identityHashCode(source), source.generationId)
        if (key == lastKey) return lastArt

        val scaled = centerCrop(source, ART_SIZE)
        val out = ByteArrayOutputStream()
        @Suppress("DEPRECATION")
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        scaled.compress(format, ART_QUALITY, out)
        if (scaled !== source) scaled.recycle()

        val bytes = out.toByteArray()
        val art = EncodedArt(sha1Hex(bytes).take(16), bytes)
        lastKey = key
        lastArt = art
        return art
    }

    fun encodeIcon(drawable: Drawable): ByteArray {
        val bitmap = drawable.toBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
    }

    private fun bitmapFrom(metadata: MediaMetadata): Bitmap? {
        BITMAP_KEYS.forEach { key -> metadata.getBitmap(key)?.let { return it } }
        // Bazı uygulamalar yalnızca URI verir. İnternet izni yok: sadece yerel content/android.resource URI'leri.
        URI_KEYS.forEach { key ->
            val uri = metadata.getString(key)?.let(Uri::parse) ?: return@forEach
            if (uri.scheme == ContentResolver.SCHEME_CONTENT || uri.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }.getOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun centerCrop(source: Bitmap, size: Int): Bitmap {
        if (source.width == size && source.height == size) return source
        val side = minOf(source.width, source.height)
        val left = (source.width - side) / 2
        val top = (source.height - side) / 2
        val target = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        Canvas(target).drawBitmap(
            source,
            Rect(left, top, left + side, top + side),
            Rect(0, 0, size, size),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
        )
        return target
    }

    private fun sha1Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        const val ART_SIZE = 256
        const val ART_QUALITY = 80
        const val ICON_SIZE = 48

        val BITMAP_KEYS = listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART,
            MediaMetadata.METADATA_KEY_ART,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON,
        )
        val URI_KEYS = listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
            MediaMetadata.METADATA_KEY_ART_URI,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
        )
    }
}
