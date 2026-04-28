package com.wagmilabs.sigil.core.crypto

import timber.log.Timber
import java.math.BigInteger
import java.security.MessageDigest
import java.security.PublicKey

/**
 * Cryptographic utility functions.
 *
 * Per protocol-spec and mobile-platform-spec.
 *
 * AGPL-3.0 License
 */
object CryptoUtils {

    /**
     * Compresses P-256 public key from 65 bytes (uncompressed) to 33 bytes (compressed).
     *
     * Per protocol-spec §1.2: Wire format requires compressed 33-byte format.
     *
     * Format:
     * - Uncompressed: 0x04 || x (32 bytes) || y (32 bytes) = 65 bytes
     * - Compressed: prefix (1 byte) || x (32 bytes) = 33 bytes
     *   where prefix = 0x02 if y is even, 0x03 if y is odd
     *
     * @param uncompressed 65-byte uncompressed public key
     * @return 33-byte compressed public key
     */
    fun compressP256PublicKey(uncompressed: ByteArray): ByteArray {
        require(uncompressed.size == 65 && uncompressed[0] == 0x04.toByte()) {
            "Invalid uncompressed public key format"
        }

        val x = uncompressed.sliceArray(1..32)
        val y = uncompressed.sliceArray(33..64)

        // Prefix: 0x02 if y is even, 0x03 if y is odd
        val prefix = if (y.last() and 1 == 0.toByte()) 0x02 else 0x03

        return byteArrayOf(prefix.toByte()) + x
    }

    /**
     * Derives device fingerprint from public key.
     *
     * Per protocol-spec §2.1: fingerprint = SHA256(compressed_public_key)
     *
     * @param publicKey Compressed 33-byte public key
     * @return 32-byte SHA-256 hash (device fingerprint)
     */
    fun deriveFingerprint(publicKey: ByteArray): ByteArray {
        require(publicKey.size == 33) {
            "Public key must be 33 bytes (compressed format)"
        }

        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(publicKey)
    }

    /**
     * Derives device fingerprint from PublicKey object.
     *
     * @param publicKey Java PublicKey object
     * @return 32-byte SHA-256 hash (device fingerprint)
     */
    fun deriveFingerprint(publicKey: PublicKey): ByteArray {
        val encoded = publicKey.encoded
        // Android KeyStore returns keys in X.509 format
        // Extract raw public key bytes (last 65 bytes for uncompressed P-256)
        val rawPublicKey = if (encoded.size == 91) {
            // X.509 format: header (26 bytes) + uncompressed key (65 bytes)
            encoded.sliceArray(26 until 91)
        } else {
            Timber.w("Unexpected public key encoding size: ${encoded.size}")
            encoded
        }

        val compressed = compressP256PublicKey(rawPublicKey)
        return deriveFingerprint(compressed)
    }

    /**
     * Converts byte array to hex string.
     *
     * @param bytes Byte array
     * @return Lowercase hex string
     */
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Converts hex string to byte array.
     *
     * @param hex Hex string
     * @return Byte array
     */
    fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Hex string must have even length" }
        return hex.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    /**
     * P-256 (secp256r1) curve order N.
     * Per protocol-spec §3.3 and Ridge's LowS.cs.
     */
    private val P256_ORDER = BigInteger(
        "115792089210356248762697446949407573529996955224135760342422259061068512044369"
    )

    /**
     * N / 2 — boundary between low-S and high-S signatures.
     */
    private val P256_HALF_ORDER = P256_ORDER.shiftRight(1)

    /**
     * Converts DER-encoded ECDSA signature to IEEE P1363 raw format (r || s).
     *
     * Android's Signature.sign() returns DER-encoded signatures, but Sigil protocol
     * requires IEEE P1363 format (64 bytes: 32-byte r || 32-byte s).
     *
     * DER format: 0x30 [total-length] 0x02 [r-length] [r-bytes] 0x02 [s-length] [s-bytes]
     *
     * @param derSignature DER-encoded signature from Android Signature.sign()
     * @return 64-byte signature in IEEE P1363 format (r || s)
     */
    fun derToRawSignature(derSignature: ByteArray): ByteArray {
        require(derSignature.isNotEmpty() && derSignature[0] == 0x30.toByte()) {
            "Invalid DER signature format"
        }

        var offset = 2 // Skip 0x30 and total length

        // Parse r
        require(derSignature[offset] == 0x02.toByte()) { "Invalid DER: r marker missing" }
        offset++
        val rLength = derSignature[offset].toInt() and 0xFF
        offset++
        val r = BigInteger(1, derSignature.sliceArray(offset until offset + rLength))
        offset += rLength

        // Parse s
        require(derSignature[offset] == 0x02.toByte()) { "Invalid DER: s marker missing" }
        offset++
        val sLength = derSignature[offset].toInt() and 0xFF
        offset++
        val s = BigInteger(1, derSignature.sliceArray(offset until offset + sLength))

        // Convert to 32-byte arrays (left-padded if needed)
        val rBytes = r.toByteArray().let { bytes ->
            when {
                bytes.size == 32 -> bytes
                bytes.size < 32 -> ByteArray(32 - bytes.size) + bytes
                bytes.size == 33 && bytes[0] == 0.toByte() -> bytes.sliceArray(1..32)
                else -> throw IllegalStateException("Invalid r length: ${bytes.size}")
            }
        }

        val sBytes = s.toByteArray().let { bytes ->
            when {
                bytes.size == 32 -> bytes
                bytes.size < 32 -> ByteArray(32 - bytes.size) + bytes
                bytes.size == 33 && bytes[0] == 0.toByte() -> bytes.sliceArray(1..32)
                else -> throw IllegalStateException("Invalid s length: ${bytes.size}")
            }
        }

        return rBytes + sBytes
    }

    /**
     * Normalizes ECDSA signature to low-S form per BIP-62.
     *
     * Per protocol-spec §3.3: signatures MUST have S ≤ N/2. If S > N/2,
     * it is normalized to N - S.
     *
     * Reference: Ridge's Sigil.Windows.Core/Crypto/LowS.cs and Kai's
     * relay/internal/verify/verify.go (SIG-2026-001 fix).
     *
     * @param signature 64-byte IEEE P1363 signature (r || s)
     * @return 64-byte low-S normalized signature
     */
    fun normalizeLowS(signature: ByteArray): ByteArray {
        require(signature.size == 64) {
            "P-256 IEEE P1363 signature must be 64 bytes (r || s); got ${signature.size}"
        }

        // Extract r (unchanged) and s
        val rBytes = signature.sliceArray(0 until 32)
        val s = BigInteger(1, signature.sliceArray(32 until 64))

        // Check if s is already low-S
        if (s <= P256_HALF_ORDER) {
            return signature // Already normalized
        }

        // Normalize: s' = n - s
        val normalizedS = P256_ORDER.subtract(s)
        val normalizedSBytes = normalizedS.toByteArray().let { bytes ->
            when {
                bytes.size == 32 -> bytes
                bytes.size < 32 -> ByteArray(32 - bytes.size) + bytes
                bytes.size == 33 && bytes[0] == 0.toByte() -> bytes.sliceArray(1..32)
                else -> throw IllegalStateException("Invalid normalized s length: ${bytes.size}")
            }
        }

        return rBytes + normalizedSBytes
    }

    /**
     * Checks if a signature is in canonical low-S form.
     *
     * Useful for verification paths that want to reject high-S signatures
     * rather than silently normalize.
     *
     * @param signature 64-byte IEEE P1363 signature (r || s)
     * @return true if S ≤ N/2, false otherwise
     */
    fun isLowS(signature: ByteArray): Boolean {
        if (signature.size != 64) {
            return false
        }

        val s = BigInteger(1, signature.sliceArray(32 until 64))
        return s <= P256_HALF_ORDER
    }
}
