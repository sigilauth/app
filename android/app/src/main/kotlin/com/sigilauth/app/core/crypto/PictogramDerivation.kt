package com.wagmilabs.sigil.core.crypto

/**
 * Pictogram derivation from device fingerprint per protocol-spec §3.6.
 *
 * Canonical emoji list (64 entries, indices 0-63).
 * Derivation: Extract first 30 bits from fingerprint, split into 5 x 6-bit indices.
 *
 * Per D10: pictogram_speakable uses SPACE-separated words.
 *
 * AGPL-3.0 License
 */
object PictogramDerivation {

    /**
     * Canonical emoji list per protocol-spec §3.6.
     * Order is fixed and MUST NOT change (fingerprints are deterministic).
     */
    val EMOJI_LIST = listOf(
        "apple", "banana", "grapes", "orange", "lemon", "cherry", "strawberry", "kiwi",
        "carrot", "corn", "broccoli", "mushroom", "pepper", "avocado", "onion", "peanut",
        "pizza", "burger", "taco", "donut", "cookie", "cake", "cupcake", "popcorn",
        "car", "taxi", "bus", "rocket", "plane", "helicopter", "sailboat", "bicycle",
        "dog", "cat", "fish", "butterfly", "bee", "fox", "lion", "elephant",
        "tree", "sunflower", "cactus", "clover", "blossom", "rainbow", "star", "moon",
        "house", "mountain", "peak", "volcano", "island", "moai", "tent", "castle",
        "key", "bell", "books", "guitar", "anchor", "crown", "diamond", "fire"
    )

    /**
     * Derives 5-element pictogram from fingerprint.
     *
     * Algorithm:
     * 1. Read first 4 bytes of fingerprint as big-endian uint32
     * 2. Extract 5 x 6-bit indices from first 30 bits
     * 3. Map each index to emoji name
     *
     * @param fingerprint 32-byte SHA-256 fingerprint
     * @return Pictogram (list of 5 emoji names + space-separated speakable string)
     */
    fun derive(fingerprint: ByteArray): Pictogram {
        require(fingerprint.size == 32) { "Fingerprint must be 32 bytes" }

        // Read first 4 bytes as big-endian unsigned int
        val first4Bytes = (
            (fingerprint[0].toInt() and 0xFF shl 24) or
            (fingerprint[1].toInt() and 0xFF shl 16) or
            (fingerprint[2].toInt() and 0xFF shl 8) or
            (fingerprint[3].toInt() and 0xFF)
        ).toLong() and 0xFFFFFFFFL

        // Extract 5 x 6-bit indices from bits 0-29 (30 bits total)
        // Canonical shifts per protocol-spec §3.6 and Ridge's B8 implementation
        val indices = listOf(
            ((first4Bytes shr 26) and 0x3F).toInt(),  // Bits 31-26
            ((first4Bytes shr 20) and 0x3F).toInt(),  // Bits 25-20
            ((first4Bytes shr 14) and 0x3F).toInt(),  // Bits 19-14
            ((first4Bytes shr 8) and 0x3F).toInt(),   // Bits 13-8
            ((first4Bytes shr 2) and 0x3F).toInt()    // Bits 7-2
        )

        // Map indices to emoji names
        val pictogramList = indices.map { EMOJI_LIST[it] }

        // Speakable: space-separated per D10
        val speakable = pictogramList.joinToString(" ")

        return Pictogram(pictogramList, speakable)
    }

    data class Pictogram(
        val emojis: List<String>,
        val speakable: String
    ) {
        init {
            require(emojis.size == 5) { "Pictogram must have exactly 5 emojis" }
        }
    }
}
