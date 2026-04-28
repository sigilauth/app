package com.wagmilabs.sigil.core.crypto

import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

/**
 * Domain separation test vectors per api/domain-separation.md
 * Tests verify that tagged hashing produces canonical digests
 *
 * AGPL-3.0 License
 */
class DomainSeparationTest {

    // MARK: - Test Vectors from api/test-vectors/domain-separation/*-v1.json

    @Test
    fun testAuthDomainTag() {
        val domain = DomainTag.AUTH
        assertEquals("5349474c2d415554482d56312d00", domain.toHex())
        assertEquals(15, domain.bytes.size)
    }

    @Test
    fun testMPADomainTag() {
        val domain = DomainTag.MPA
        assertEquals("5349474c2d4d50412d56312d00", domain.toHex())
        assertEquals(14, domain.bytes.size)
    }

    @Test
    fun testDecryptDomainTag() {
        val domain = DomainTag.DECRYPT
        assertEquals("5349474c2d444543525950542d56312d00", domain.toHex())
        assertEquals(18, domain.bytes.size)
    }

    // MARK: - Signature Generation Tests (Byte-for-Byte Canonical Match)

    @Test
    fun testAuthSignatureGenerationMatchesCanonical() {
        // Test vector from auth-v1.json (with action_context binding per spec be85208)
        val privateKeyHex = "c9afa9d845ba75166b5c215767b1d6934e50c3db36e89b127b8a622b120f6721"
        val challengeHex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        val actionContextJSON = "{\"description\":\"Sign in to Engine Management\",\"type\":\"engine_login\",\"user_id\":\"alice\"}"
        val actionHashHex = "ae4c8d04ee09905ce0fbf5dd11c8733e92508ab00301b19d8e9da8f28b49b620"
        val expectedSignatureHex = "1500ac510b6b2cd7ed7400542d10ecf500a36e6f402b2f122afc15b8988e27c03a10c9d47aebcda59893507d3ad5bb75dd0fe5b94cecbbbb27c2522847cb1f02"

        val privateKeyBytes = hexStringToByteArray(privateKeyHex)
        val challenge = hexStringToByteArray(challengeHex)
        val actionHash = hexStringToByteArray(actionHashHex)
        val expectedSignature = hexStringToByteArray(expectedSignatureHex)

        // Verify action_context hash matches expected
        val computedActionHash = JSONCanonicalizer.hash(actionContextJSON)
        assertArrayEquals("Computed action_context hash must match test vector", actionHash, computedActionHash)

        // Build auth payload: challenge_bytes || action_hash
        val authPayload = challenge + actionHash

        // Load private key (software key for testing)
        val privateKey = loadPrivateKey(privateKeyBytes)

        // Sign with domain separation
        val signature = signWithDomain(privateKey, authPayload, DomainTag.AUTH)

        // Compare byte-for-byte
        assertArrayEquals(
            "Generated AUTH signature must match canonical test vector byte-for-byte (with action_context)",
            expectedSignature,
            signature
        )
    }

    @Test
    fun testAuthSignatureEmptyActionContext() {
        // Test vector from auth-v1.json (empty action_context)
        val privateKeyHex = "c9afa9d845ba75166b5c215767b1d6934e50c3db36e89b127b8a622b120f6721"
        val challengeHex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        val emptyActionHashHex = "44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a"
        val expectedSignatureHex = "c8c48552aea9b31d6e1b14228598fc5db9c2ef23e31573c8957d96fc966347e4491183144065153ca605f0572fb655b54da99a92f7b7468ab22276e4ad36a6ea"

        val privateKeyBytes = hexStringToByteArray(privateKeyHex)
        val challenge = hexStringToByteArray(challengeHex)
        val emptyActionHash = hexStringToByteArray(emptyActionHashHex)
        val expectedSignature = hexStringToByteArray(expectedSignatureHex)

        // Verify empty action_context hash (canonical "{}")
        val computedEmptyHash = JSONCanonicalizer.hash("{}")
        assertArrayEquals("Empty action_context hash must be fixed canonical value", emptyActionHash, computedEmptyHash)

        // Build auth payload: challenge_bytes || empty_action_hash
        val authPayload = challenge + emptyActionHash

        // Load private key (software key for testing)
        val privateKey = loadPrivateKey(privateKeyBytes)

        // Sign with domain separation
        val signature = signWithDomain(privateKey, authPayload, DomainTag.AUTH)

        // Compare byte-for-byte
        assertArrayEquals(
            "Generated AUTH signature must match canonical test vector byte-for-byte (empty action_context)",
            expectedSignature,
            signature
        )
    }

    @Test
    fun testMPASignatureGenerationMatchesCanonical() {
        // Load test private key and generate signature - must match canonical bytes exactly
        val privateKeyHex = "c9afa9d845ba75166b5c215767b1d6934e50c3db36e89b127b8a622b120f6721"
        val messageJSON = "{\"type\":\"delete_account\",\"description\":\"Permanently delete user account\",\"params\":{\"user_id\":\"123\"}}"
        val expectedSignatureHex = "0fcbec9dbcfbb571dc11f16577c8ae487d9feade9c357a70354f45130e2a3a4650b46362efa7130e4a84ffaf3729bf0fb5db38def3b75096c2341ad52b085574"

        val privateKeyBytes = hexStringToByteArray(privateKeyHex)
        val message = messageJSON.toByteArray(Charsets.UTF_8)
        val expectedSignature = hexStringToByteArray(expectedSignatureHex)

        // Load private key (software key for testing)
        val privateKey = loadPrivateKey(privateKeyBytes)

        // Sign with domain separation
        val signature = signWithDomain(privateKey, message, DomainTag.MPA)

        // Compare byte-for-byte
        assertArrayEquals(
            "Generated MPA signature must match canonical test vector byte-for-byte",
            expectedSignature,
            signature
        )
    }

    // MARK: - Tagged Hash Tests

    @Test
    fun testAuthTaggedHashProducesCorrectDigest() {
        // Verify that taggedHash produces the expected SHA256(domain || message)
        val domain = DomainTag.AUTH
        val messageHex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        val expectedTaggedInputHex = "534947494c2d415554482d5631000123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

        val message = hexStringToByteArray(messageHex)
        val expectedTaggedInput = hexStringToByteArray(expectedTaggedInputHex)

        // Verify tagged input construction
        val taggedInput = domain.bytes + message
        assertArrayEquals("Tagged input should match expected concatenation", expectedTaggedInput, taggedInput)

        // Verify hash
        val digest = taggedHash(domain, message)
        val expectedDigest = MessageDigest.getInstance("SHA-256").digest(expectedTaggedInput)
        assertArrayEquals("Tagged hash should match SHA256(domain || message)", expectedDigest, digest)
    }

    @Test
    fun testMPATaggedHashProducesCorrectDigest() {
        val domain = DomainTag.MPA
        val messageJSON = "{\"type\":\"delete_account\",\"description\":\"Permanently delete user account\",\"params\":{\"user_id\":\"123\"}}"
        val expectedTaggedInputHex = "534947494c2d4d50412d5631007b2274797065223a2264656c6574655f6163636f756e74222c226465736372697074696f6e223a225065726d616e656e746c792064656c6574652075736572206163636f756e74222c22706172616d73223a7b22757365725f6964223a22313233227d7d"

        val message = messageJSON.toByteArray(Charsets.UTF_8)
        val expectedTaggedInput = hexStringToByteArray(expectedTaggedInputHex)

        val taggedInput = domain.bytes + message
        assertArrayEquals("Tagged input should match expected concatenation", expectedTaggedInput, taggedInput)

        val digest = taggedHash(domain, message)
        val expectedDigest = MessageDigest.getInstance("SHA-256").digest(expectedTaggedInput)
        assertArrayEquals("Tagged hash should match SHA256(domain || message)", expectedDigest, digest)
    }

    @Test
    fun testAuthTaggedInputConstruction() {
        // Test vector: challenge_bytes (32 random)
        val messageHex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        val message = hexStringToByteArray(messageHex)

        // Expected: SIGIL-AUTH-V1\0 || message
        val domain = DomainTag.AUTH
        val tagged = domain.bytes + message

        // Verify length
        assertEquals(15 + 32, tagged.size)

        // Verify domain prefix
        assertArrayEquals(domain.bytes, tagged.sliceArray(0 until 15))

        // Verify message suffix
        assertArrayEquals(message, tagged.sliceArray(15 until tagged.size))
    }

    @Test
    fun testMPATaggedInputConstruction() {
        val messageJSON = "{\"type\":\"delete_account\",\"description\":\"Permanently delete user account\",\"params\":{\"user_id\":\"123\"}}"
        val message = messageJSON.toByteArray(Charsets.UTF_8)

        val domain = DomainTag.MPA
        val tagged = domain.bytes + message

        // Verify length
        assertEquals(14 + message.size, tagged.size)

        // Verify domain prefix
        assertArrayEquals(domain.bytes, tagged.sliceArray(0 until 14))

        // Verify message suffix
        assertArrayEquals(message, tagged.sliceArray(14 until tagged.size))
    }

    // MARK: - Utility Functions

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    // MARK: - Signing Helpers (for byte-match tests)

    private fun loadPrivateKey(rawKey: ByteArray): java.security.PrivateKey {
        val d = java.math.BigInteger(1, rawKey)
        val ecSpec = java.security.spec.ECGenParameterSpec("secp256r1")
        val keyFactory = java.security.KeyFactory.getInstance("EC")
        val params = java.security.AlgorithmParameters.getInstance("EC").apply { init(ecSpec) }
            .getParameterSpec(java.security.spec.ECParameterSpec::class.java)
        val privateKeySpec = java.security.spec.ECPrivateKeySpec(d, params)
        return keyFactory.generatePrivate(privateKeySpec)
    }

    private fun signWithDomain(privateKey: java.security.PrivateKey, message: ByteArray, domain: DomainTag): ByteArray {
        // Compute tagged hash
        val digest = taggedHash(domain, message)

        // Sign digest with NONEwithECDSA (to avoid double-hashing)
        val signature = java.security.Signature.getInstance("NONEwithECDSA")
        signature.initSign(privateKey)
        signature.update(digest)
        val derSig = signature.sign()

        // Convert DER to raw R || S
        return derToRaw(derSig)
    }

    private fun derToRaw(derSig: ByteArray): ByteArray {
        // P-256 parameters
        val p256Order = java.math.BigInteger("FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551", 16)
        val p256HalfOrder = p256Order.shiftRight(1)

        // Simple DER parsing: 0x30 [len] 0x02 [r-len] [r] 0x02 [s-len] [s]
        var idx = 0
        require(derSig[idx++] == 0x30.toByte()) { "Invalid DER signature" }
        idx++ // skip total length

        require(derSig[idx++] == 0x02.toByte()) { "Invalid DER signature" }
        val rLen = derSig[idx++].toInt()
        val r = derSig.sliceArray(idx until idx + rLen)
        idx += rLen

        require(derSig[idx++] == 0x02.toByte()) { "Invalid DER signature" }
        val sLen = derSig[idx++].toInt()
        val s = derSig.sliceArray(idx until idx + sLen)

        // Pad to 32 bytes and normalize low-S
        val rPadded = r.takeLast(32).toByteArray().let {
            if (it.size < 32) ByteArray(32 - it.size) + it else it
        }
        var sBig = java.math.BigInteger(1, s)

        // BIP-62 low-S normalization
        if (sBig > p256HalfOrder) {
            sBig = p256Order - sBig
        }

        val sPadded = sBig.toByteArray().let {
            when {
                it.size > 32 -> it.takeLast(32).toByteArray()
                it.size < 32 -> ByteArray(32 - it.size) + it
                else -> it
            }
        }

        return rPadded + sPadded
    }
}
