package com.wagmilabs.sigil.core.crypto

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CryptoUtils.
 *
 * Per maren-qa-strategy.md: TDD required, 80% coverage for Android.
 *
 * AGPL-3.0 License
 */
class CryptoUtilsTest {

    @Test
    fun `compressP256PublicKey compresses 65-byte key to 33 bytes`() {
        // Test vector: uncompressed P-256 public key
        val uncompressed = byteArrayOf(
            0x04, // Uncompressed prefix
            // x coordinate (32 bytes)
            0x6b, 0x17.toByte(), 0xd1.toByte(), 0xf2.toByte(),
            0xe1.toByte(), 0x2c, 0x42, 0x47,
            0xf8.toByte(), 0xbc.toByte(), 0xe6.toByte(), 0xe5.toByte(),
            0x63, 0xa4.toByte(), 0x40, 0xf2.toByte(),
            0x77, 0x03, 0x7d, 0x81.toByte(),
            0x2d, 0xeb.toByte(), 0x33, 0xa0.toByte(),
            0xf4.toByte(), 0xa1.toByte(), 0x39, 0x45,
            0xd8.toByte(), 0x98.toByte(), 0xc2.toByte(), 0x96.toByte(),
            // y coordinate (32 bytes)
            0x4f, 0xe3.toByte(), 0x42, 0xe2.toByte(),
            0xfe.toByte(), 0x1a, 0x7f, 0x9b.toByte(),
            0x8e.toByte(), 0xe7.toByte(), 0xeb.toByte(), 0x4a,
            0x7c, 0x0f, 0x9e.toByte(), 0x16,
            0x2b, 0xce.toByte(), 0x33, 0x57,
            0x6b, 0x31, 0x5e, 0xce.toByte(),
            0xcb.toByte(), 0xb6.toByte(), 0x40, 0x68,
            0x37, 0xbf.toByte(), 0x51, 0xf5.toByte()
        )

        val compressed = CryptoUtils.compressP256PublicKey(uncompressed)

        // Compressed key should be 33 bytes
        assertEquals(33, compressed.size)

        // First byte should be 0x03 (y is odd: 0xf5 & 1 = 1)
        assertEquals(0x03, compressed[0])

        // x coordinate should match
        val xCoordinate = uncompressed.sliceArray(1..32)
        assertArrayEquals(xCoordinate, compressed.sliceArray(1..32))
    }

    @Test
    fun `compressP256PublicKey handles even y coordinate`() {
        // Create test key with even y coordinate (last byte = 0x00)
        val uncompressed = ByteArray(65) { 0x00 }
        uncompressed[0] = 0x04

        val compressed = CryptoUtils.compressP256PublicKey(uncompressed)

        // Prefix should be 0x02 for even y
        assertEquals(0x02, compressed[0])
    }

    @Test
    fun `deriveFingerprint produces 32-byte SHA256 hash`() {
        val publicKey = ByteArray(33) { it.toByte() }

        val fingerprint = CryptoUtils.deriveFingerprint(publicKey)

        assertEquals(32, fingerprint.size)
    }

    @Test
    fun `deriveFingerprint is deterministic`() {
        val publicKey = ByteArray(33) { it.toByte() }

        val fp1 = CryptoUtils.deriveFingerprint(publicKey)
        val fp2 = CryptoUtils.deriveFingerprint(publicKey)

        assertArrayEquals(fp1, fp2)
    }

    @Test
    fun `bytesToHex converts correctly`() {
        val bytes = byteArrayOf(0x01, 0x0a, 0x10, 0xff.toByte())
        val hex = CryptoUtils.bytesToHex(bytes)

        assertEquals("010a10ff", hex)
    }

    @Test
    fun `hexToBytes converts correctly`() {
        val hex = "010a10ff"
        val bytes = CryptoUtils.hexToBytes(hex)

        assertArrayEquals(byteArrayOf(0x01, 0x0a, 0x10, 0xff.toByte()), bytes)
    }

    @Test
    fun `hex round-trip preserves data`() {
        val original = ByteArray(32) { it.toByte() }

        val hex = CryptoUtils.bytesToHex(original)
        val restored = CryptoUtils.hexToBytes(hex)

        assertArrayEquals(original, restored)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `compressP256PublicKey rejects invalid length`() {
        val invalid = ByteArray(64)
        CryptoUtils.compressP256PublicKey(invalid)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `compressP256PublicKey rejects wrong prefix`() {
        val invalid = ByteArray(65)
        invalid[0] = 0x02 // Wrong prefix (should be 0x04 for uncompressed)
        CryptoUtils.compressP256PublicKey(invalid)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `deriveFingerprint rejects wrong key size`() {
        val invalid = ByteArray(32) // Should be 33 bytes
        CryptoUtils.deriveFingerprint(invalid)
    }
}
