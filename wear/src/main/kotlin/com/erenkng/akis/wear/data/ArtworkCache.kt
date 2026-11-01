package com.erenkng.akis.wear.data

import android.graphics.Bitmap
import android.util.LruCache

/** Bellek içi kapak önbelleği: `artHash` → Bitmap, en fazla 10 kapak (BRIEF Bölüm 10). */
class ArtworkCache(maxEntries: Int = 10) {

    private val cache = LruCache<String, Bitmap>(maxEntries)

    fun get(hash: String): Bitmap? = if (hash.isEmpty()) null else cache.get(hash)

    fun put(hash: String, bitmap: Bitmap) {
        if (hash.isNotEmpty()) cache.put(hash, bitmap)
    }
}
