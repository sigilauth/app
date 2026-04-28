package com.wagmilabs.sigil.core.crypto

import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger

/**
 * Test BIP-62 low-S normalization against known test vectors.
 *
 * Per protocol-spec §3.3: ECDSA signatures MUST have S ≤ N/2.
 * If S > N/2, normalize to N - S.
 *
 * Reference implementations:
 * - Ridge's Sigil.Windows.Core/Crypto/LowS.cs
 * - Kai's relay/internal/verify/verify.go (SIG-2026-001 fix)
 *
 * Test vectors from: /Volumes/Expansion/src/sigilauth/security/test-vectors/signatures/
 *
 * AGPL-3.0 License
 */
class LowSNormalizationTest {

    companion object {
        // P-256 curve order N
        private val P256_ORDER = BigInteger(
            "115792089210356248762697446949407573529996955224135760342422259061068512044369"
        )

        // N / 2
        private val P256_HALF_ORDER = P256_ORDER.shiftRight(1)
    }

    @Test
    fun `normalizeLowS does not modify already low-S signature`() {
        // Test with s = 1 (minimum valid s, clearly low-S)
        val r = ByteArray(32) { 0x12 }
        val s = ByteArray(31) { 0x00 } + byteArrayOf(0x01) // s = 1
        val signature = r + s

        val normalized = CryptoUtils.normalizeLowS(signature)

        // Should be unchanged (byte-identical)
        assertArrayEquals(signature, normalized)
        assertTrue(CryptoUtils.isLowS(normalized))
    }

    @Test
    fun `normalizeLowS flips high-S signature`() {
        // Create a high-S signature: s = n - 1 (maximum valid s, clearly high-S)
        val r = ByteArray(32) { 0x12 }
        val highS = P256_ORDER.subtract(BigInteger.ONE).toByteArray()
            .let { bytes ->
                when {
                    bytes.size == 32 -> bytes
                    bytes.size < 32 -> ByteArray(32 - bytes.size) + bytes
                    bytes.size == 33 && bytes[0] == 0.toByte() -> bytes.sliceArray(1..32)
                    else -> bytes.takeLast(32).toByteArray()
                }
            }

        val signature = r + highS

        // Verify it's high-S before normalization
        assertFalse("Signature should be high-S before normalization", CryptoUtils.isLowS(signature))

        val normalized = CryptoUtils.normalizeLowS(signature)

        // After normalization, should be low-S
        assertTrue("Signature should be low-S after normalization", CryptoUtils.isLowS(normalized))

        // r should be unchanged
        assertArrayEquals("r should be unchanged", r, normalized.sliceArray(0 until 32))

        // s should be flipped to n - s = n - (n - 1) = 1
        val normalizedS = BigInteger(1, normalized.sliceArray(32 until 64))
        assertEquals(BigInteger.ONE, normalizedS)
    }

    @Test
    fun `normalizeLowS handles s exactly at n div 2`() {
        // s = n/2 is the boundary — it's still valid (not > n/2)
        val r = ByteArray(32) { 0x12 }
        val s = P256_HALF_ORDER.toByteArray()
            .let { bytes ->
                when {
                    bytes.size == 32 -> bytes
                    bytes.size < 32 -> ByteArray(32 - bytes.size) + bytes
                    bytes.size == 33 && bytes[0] == 0.toByte() -> bytes.sliceArray(1..32)
                    else -> bytes.takeLast(32).toByteArray()
                }
            }

        val signature = r + s

        // s = n/2 is low-S (not > n/2)
        assertTrue("s = n/2 should be considered low-S", CryptoUtils.isLowS(signature))

        val normalized = CryptoUtils.normalizeLowS(signature)

        // Should be unchanged
        assertArrayEquals(signature, normalized)
    }

    @Test
    fun `normalizeLowS handles s just above n div 2`() {
        // s = (n/2) + 1 (just over the boundary, should be normalized)
        val r = ByteArray(32) { 0x12 }
        val s = P256_HALF_ORDER.add(BigInteger.ONE).toByteArray()
            .let { bytes ->
                when {
                    bytes.size == 32 -> bytes
                    bytes.size < 32 -> ByteArray(32 - bytes.size) + bytes
                    bytes.size == 33 && bytes[0] == 0.toByte() -> bytes.sliceArray(1..32)
                    else -> bytes.takeLast(32).toByteArray()
                }
            }

        val signature = r + s

        // Should be high-S
        assertFalse("s = n/2 + 1 should be high-S", CryptoUtils.isLowS(signature))

        val normalized = CryptoUtils.normalizeLowS(signature)

        // After normalization, should be low-S
        assertTrue(CryptoUtils.isLowS(normalized))

        // Normalized s should be n - ((n/2) + 1) = (n/2) - 1
        val normalizedS = BigInteger(1, normalized.sliceArray(32 until 64))
        val expected = P256_HALF_ORDER.subtract(BigInteger.ONE)
        assertEquals(expected, normalizedS)
    }

    @Test
    fun `isLowS returns false for high-S signature`() {
        val r = ByteArray(32) { 0x12 }
        val highS = P256_ORDER.subtract(BigInteger.ONE).toByteArray()
            .let { bytes ->
                when {
                    bytes.size == 32 -> bytes
                    bytes.size < 32 -> ByteArray(32 - bytes.size) + bytes
                    bytes.size == 33 && bytes[0] == 0.toByte() -> bytes.sliceArray(1..32)
                    else -> bytes.takeLast(32).toByteArray()
                }
            }

        val signature = r + highS

        assertFalse(CryptoUtils.isLowS(signature))
    }

    @Test
    fun `isLowS returns false for wrong signature length`() {
        val invalidSignature = ByteArray(32) // Only 32 bytes instead of 64

        assertFalse(CryptoUtils.isLowS(invalidSignature))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `normalizeLowS rejects wrong signature length`() {
        val invalidSignature = ByteArray(32)
        CryptoUtils.normalizeLowS(invalidSignature)
    }

    @Test
    fun `normalization is idempotent`() {
        // Create any signature
        val r = ByteArray(32) { 0x12 }
        val s = P256_ORDER.subtract(BigInteger.valueOf(12345)).toByteArray()
            .let { bytes ->
                when {
                    bytes.size == 32 -> bytes
                    bytes.size < 32 -> ByteArray(32 - bytes.size) + bytes
                    bytes.size == 33 && bytes[0] == 0.toByte() -> bytes.sliceArray(1..32)
                    else -> bytes.takeLast(32).toByteArray()
                }
            }

        val signature = r + s

        // Normalize once
        val normalized1 = CryptoUtils.normalizeLowS(signature)

        // Normalize again
        val normalized2 = CryptoUtils.normalizeLowS(normalized1)

        // Should be identical (idempotent)
        assertArrayEquals(normalized1, normalized2)
        assertTrue(CryptoUtils.isLowS(normalized2))
    }

    @Test
    fun `derToRawSignature converts valid DER signature`() {
        // Minimal valid DER signature: r=1, s=1
        // Format: 0x30 [total-len] 0x02 [r-len] [r-bytes] 0x02 [s-len] [s-bytes]
        val derSignature = byteArrayOf(
            0x30, 0x06,        // SEQUENCE, total length 6
            0x02, 0x01, 0x01,  // INTEGER r = 1
            0x02, 0x01, 0x01   // INTEGER s = 1
        )

        val rawSignature = CryptoUtils.derToRawSignature(derSignature)

        // Should be 64 bytes: 32-byte r || 32-byte s
        assertEquals(64, rawSignature.size)

        // r should be 1 (left-padded with zeros to 32 bytes)
        val expectedR = ByteArray(31) { 0x00 } + byteArrayOf(0x01)
        assertArrayEquals(expectedR, rawSignature.sliceArray(0 until 32))

        // s should be 1 (left-padded with zeros to 32 bytes)
        val expectedS = ByteArray(31) { 0x00 } + byteArrayOf(0x01)
        assertArrayEquals(expectedS, rawSignature.sliceArray(32 until 64))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `derToRawSignature rejects invalid DER format`() {
        val invalidDer = byteArrayOf(0x00, 0x01, 0x02) // Doesn't start with 0x30
        CryptoUtils.derToRawSignature(invalidDer)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `derToRawSignature rejects empty signature`() {
        val emptyDer = byteArrayOf()
        CryptoUtils.derToRawSignature(emptyDer)
    }
}
