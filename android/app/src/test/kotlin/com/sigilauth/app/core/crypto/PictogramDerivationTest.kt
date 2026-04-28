package com.wagmilabs.sigil.core.crypto

import org.junit.Assert.*
import org.junit.Test

/**
 * Test pictogram derivation against official test vectors.
 *
 * Test vectors from: /Volumes/Expansion/src/sigilauth/api/test-vectors/pictogram.json
 *
 * Per D10: pictogram_speakable MUST use space-separated words.
 *
 * AGPL-3.0 License
 */
class PictogramDerivationTest {

    @Test
    fun `derive pictogram from protocol-spec example`() {
        // Test vector: "Example from protocol-spec §11.4"
        val fingerprint = CryptoUtils.hexToBytes(
            "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2"
        )

        val pictogram = PictogramDerivation.derive(fingerprint)

        // Expected indices: [40, 27, 11, 3, 53]
        val expectedEmojis = listOf("tree", "rocket", "mushroom", "orange", "moai")
        val expectedSpeakable = "tree rocket mushroom orange moai"

        assertEquals(expectedEmojis, pictogram.emojis)
        assertEquals(expectedSpeakable, pictogram.speakable)
    }

    @Test
    fun `derive pictogram from all-zeros fingerprint`() {
        // Test vector: "All zeros fingerprint"
        val fingerprint = ByteArray(32) { 0x00 }

        val pictogram = PictogramDerivation.derive(fingerprint)

        // Expected indices: [0, 0, 0, 0, 0]
        val expectedEmojis = listOf("apple", "apple", "apple", "apple", "apple")
        val expectedSpeakable = "apple apple apple apple apple"

        assertEquals(expectedEmojis, pictogram.emojis)
        assertEquals(expectedSpeakable, pictogram.speakable)
    }

    @Test
    fun `derive pictogram from all-FF fingerprint (max indices)`() {
        // Test vector: "All 0xFF fingerprint (max indices)"
        val fingerprint = ByteArray(32) { 0xFF.toByte() }

        val pictogram = PictogramDerivation.derive(fingerprint)

        // Expected indices: [63, 63, 63, 63, 63]
        val expectedEmojis = listOf("fire", "fire", "fire", "fire", "fire")
        val expectedSpeakable = "fire fire fire fire fire"

        assertEquals(expectedEmojis, pictogram.emojis)
        assertEquals(expectedSpeakable, pictogram.speakable)
    }

    @Test
    fun `derive pictogram from sequential indices test`() {
        // Test vector: "Sequential indices test"
        // Binary: 00000100 00010000 01000001 00000100 -> indices [1, 0, 16, 4, 4]
        val fingerprint = CryptoUtils.hexToBytes(
            "0410410400000000000000000000000000000000000000000000000000000000"
        )

        val pictogram = PictogramDerivation.derive(fingerprint)

        // Expected indices: [1, 0, 16, 4, 4]
        val expectedEmojis = listOf("banana", "apple", "pizza", "lemon", "lemon")
        val expectedSpeakable = "banana apple pizza lemon lemon"

        assertEquals(expectedEmojis, pictogram.emojis)
        assertEquals(expectedSpeakable, pictogram.speakable)
    }

    @Test
    fun `pictogram derivation is deterministic`() {
        val fingerprint = CryptoUtils.hexToBytes(
            "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2"
        )

        val pictogram1 = PictogramDerivation.derive(fingerprint)
        val pictogram2 = PictogramDerivation.derive(fingerprint)

        assertEquals(pictogram1.emojis, pictogram2.emojis)
        assertEquals(pictogram1.speakable, pictogram2.speakable)
    }

    @Test
    fun `speakable format uses spaces not hyphens (D10)`() {
        val fingerprint = CryptoUtils.hexToBytes(
            "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2"
        )

        val pictogram = PictogramDerivation.derive(fingerprint)

        // D10: JSON speakable uses SPACES
        assertTrue(pictogram.speakable.contains(" "))
        assertFalse(pictogram.speakable.contains("-"))
    }

    @Test
    fun `emoji list has exactly 64 entries`() {
        assertEquals(64, PictogramDerivation.EMOJI_LIST.size)
    }

    @Test
    fun `emoji list has no duplicates`() {
        val uniqueEmojis = PictogramDerivation.EMOJI_LIST.toSet()
        assertEquals(PictogramDerivation.EMOJI_LIST.size, uniqueEmojis.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `derive rejects wrong fingerprint size`() {
        val invalidFingerprint = ByteArray(16) // Should be 32 bytes
        PictogramDerivation.derive(invalidFingerprint)
    }

    @Test
    fun `all indices 0-63 are valid`() {
        // Ensure no index can produce out-of-bounds error
        for (i in 0..63) {
            assertNotNull(PictogramDerivation.EMOJI_LIST[i])
        }
    }
}
