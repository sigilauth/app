package com.wagmilabs.sigil.core.crypto

import java.security.MessageDigest

/**
 * Domain separation tags for cryptographic operations.
 * Per api/domain-separation.md (Apache-2.0)
 *
 * NORMATIVE: These exact byte sequences MUST be prepended to all signed messages.
 *
 * AGPL-3.0 License
 */
enum class DomainTag(val bytes: ByteArray) {
    /**
     * Authentication challenge/response - 15 bytes
     * "SIGIL-AUTH-V1\0"
     */
    AUTH(byteArrayOf(
        0x53, 0x49, 0x47, 0x49, 0x4c, 0x2d, 0x41, 0x55, 0x54, 0x48, 0x2d, 0x56, 0x31, 0x00
    )),

    /**
     * Multi-party authorisation approval - 14 bytes
     * "SIGIL-MPA-V1\0"
     */
    MPA(byteArrayOf(
        0x53, 0x49, 0x47, 0x49, 0x4c, 0x2d, 0x4d, 0x50, 0x41, 0x2d, 0x56, 0x31, 0x00
    )),

    /**
     * Secure decrypt envelope - 18 bytes
     * "SIGIL-DECRYPT-V1\0"
     */
    DECRYPT(byteArrayOf(
        0x53, 0x49, 0x47, 0x49, 0x4c, 0x2d, 0x44, 0x45, 0x43, 0x52, 0x59, 0x50, 0x54, 0x2d, 0x56, 0x31, 0x00
    )),

    /**
     * Conversation envelope signature - 15 bytes
     * "SIGIL-CONV-V1\0"
     */
    CONV(byteArrayOf(
        0x53, 0x49, 0x47, 0x49, 0x4c, 0x2d, 0x43, 0x4f, 0x4e, 0x56, 0x2d, 0x56, 0x31, 0x00
    )),

    /**
     * Session pictogram Argon2id salt - 16 bytes (zero-padded)
     * "SIGIL-PAIR-V1\0\0\0"
     */
    PAIR(byteArrayOf(
        0x53, 0x49, 0x47, 0x49, 0x4c, 0x2d, 0x50, 0x41, 0x49, 0x52, 0x2d, 0x56, 0x31, 0x00, 0x00, 0x00
    ));

    /**
     * Hex representation (for test vectors)
     */
    fun toHex(): String = bytes.joinToString("") { "%02x".format(it) }

    /**
     * String representation (for debugging)
     */
    override fun toString(): String = bytes.decodeToString()
}

/**
 * Domain-separated hash operation.
 *
 * Implements: tagged_input = domain_tag || message, hash = SHA256(tagged_input)
 *
 * Per api/domain-separation.md §Algorithm
 */
fun taggedHash(domain: DomainTag, message: ByteArray): ByteArray {
    val tagged = domain.bytes + message
    return MessageDigest.getInstance("SHA-256").digest(tagged)
}
