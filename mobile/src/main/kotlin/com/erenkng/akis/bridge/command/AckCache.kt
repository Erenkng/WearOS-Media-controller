package com.erenkng.akis.bridge.command

import com.erenkng.akis.shared.model.CommandAck

/**
 * Saat, 3 sn içinde ack alamazsa komutu **aynı seq ile** bir kez tekrar gönderir (BRIEF Bölüm 10).
 * Ack yolda kaybolduysa komut ikinci kez çalıştırılmamalı: aksi halde TOGGLE iki kez basılmış olur.
 * Bu önbellek son komutların sonucunu tutar; tekrar gelen seq yalnızca yeniden ack'lenir.
 */
class AckCache(private val capacity: Int = 64) {

    private val entries = object : LinkedHashMap<String, CommandAck>(capacity, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CommandAck>?): Boolean =
            size > capacity
    }

    @Synchronized
    fun get(nodeId: String, seq: Long): CommandAck? = entries[key(nodeId, seq)]

    @Synchronized
    fun put(nodeId: String, ack: CommandAck) {
        entries[key(nodeId, ack.seq)] = ack
    }

    private fun key(nodeId: String, seq: Long) = "$nodeId#$seq"
}
