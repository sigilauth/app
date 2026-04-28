package com.wagmilabs.sigil.core.models

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class PictogramEntry(
    val index: Int,
    val emoji: String,
    val name: String
)

data class PictogramCategory(
    val name: String,
    @SerializedName("first_index") val firstIndex: Int,
    val count: Int,
    val entries: List<PictogramEntry>
)

data class PictogramPoolData(
    val version: Int,
    @SerializedName("pool_size") val poolSize: Int,
    @SerializedName("spec_ref") val specRef: String,
    val license: String,
    @SerializedName("unicode_range") val unicodeRange: String,
    val note: String,
    val categories: List<PictogramCategory>
)

class PictogramPool private constructor(private val entries: List<PictogramEntry>) {

    companion object {
        @Volatile
        private var instance: PictogramPool? = null

        fun getInstance(context: Context): PictogramPool {
            return instance ?: synchronized(this) {
                instance ?: loadPool(context).also { instance = it }
            }
        }

        private fun loadPool(context: Context): PictogramPool {
            val json = context.assets.open("pictogram-pool-v1.json").bufferedReader().use { it.readText() }
            val poolData = Gson().fromJson(json, PictogramPoolData::class.java)

            val allEntries = poolData.categories
                .flatMap { it.entries }
                .sortedBy { it.index }

            require(allEntries.size == 192) {
                "Pictogram pool must contain exactly 192 entries, got ${allEntries.size}"
            }

            return PictogramPool(allEntries)
        }
    }

    fun entry(index: Int): Pair<String, String>? {
        if (index < 0 || index >= entries.size) return null
        val entry = entries[index]
        return Pair(entry.emoji, entry.name)
    }

    val count: Int get() = entries.size
}
