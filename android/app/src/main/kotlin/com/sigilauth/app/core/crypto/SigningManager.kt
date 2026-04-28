package com.wagmilabs.sigil.core.crypto

import com.wagmilabs.sigil.core.biometric.BiometricAuthenticator
import timber.log.Timber
import java.security.PrivateKey
import java.security.Signature
import java.time.Instant

/**
 * Manages challenge response signing with biometric gate.
 *
 * Per knox-threat-model.md:
 * - Biometric gate on every signing operation
 * - ECDSA P-256 with SHA-256
 * - Low-S normalization (BIP-62)
 *
 * AGPL-3.0 License
 */
class SigningManager(
    private val keystoreManager: KeystoreManager,
    private val biometricAuthenticator: BiometricAuthenticator
) {

    /**
     * Signs challenge response with device private key and domain separation.
     *
     * Biometric prompt appears before signing.
     *
     * Per api/domain-separation.md:
     * - Signature payload = SHA256(SIGIL-AUTH-V1\0 || challenge_bytes)
     * - Signature format: 64 bytes (r || s), low-S normalized
     *
     * @param challengeBytes Challenge bytes from server
     * @param domain Domain tag (AUTH, MPA, or DECRYPT)
     * @param keyAlias Device key alias
     * @return Pair of (Base64 signature, ISO8601 timestamp)
     */
    suspend fun sign(
        challengeBytes: ByteArray,
        domain: DomainTag,
        keyAlias: String
    ): SignatureResult {

        // Authenticate user first
        val biometricResult = biometricAuthenticator.authenticate(
            title = "Approve Request",
            subtitle = "Tap to approve"
        )

        when (biometricResult) {
            is BiometricAuthenticator.BiometricResult.Success -> {
                // Proceed with signing
            }
            is BiometricAuthenticator.BiometricResult.Canceled -> {
                throw SigningException("User canceled biometric authentication")
            }
            is BiometricAuthenticator.BiometricResult.Lockout -> {
                throw SigningException("Biometric lockout - too many failed attempts")
            }
            is BiometricAuthenticator.BiometricResult.Error -> {
                throw SigningException("Biometric error: ${biometricResult.message}")
            }
        }

        // Retrieve private key
        val privateKey = keystoreManager.getPrivateKey(keyAlias)
            ?: throw SigningException("Private key not found: $keyAlias")

        // Generate timestamp
        val timestamp = Instant.now().toString()

        // Sign with domain separation per api/domain-separation.md
        val signatureBytes = signWithDomain(privateKey, challengeBytes, domain)

        // Encode to Base64
        val signatureB64 = android.util.Base64.encodeToString(
            signatureBytes,
            android.util.Base64.NO_WRAP
        )

        Timber.d("Challenge signed successfully with domain ${domain.name}")
        return SignatureResult(signatureB64, timestamp)
    }

    /**
     * Sign message with domain separation.
     * Per api/domain-separation.md: hash(domain_tag || message) then sign the digest.
     */
    private fun signWithDomain(
        privateKey: PrivateKey,
        message: ByteArray,
        domain: DomainTag
    ): ByteArray {
        // Domain-separated hash
        val digest = taggedHash(domain, message)

        // Sign the digest (not the message)
        // Use NONEwithECDSA to sign the digest directly without double-hashing
        val signature = Signature.getInstance("NONEwithECDSA")
        signature.initSign(privateKey)
        signature.update(digest)

        // Android returns DER-encoded signatures, convert to IEEE P1363 raw format (r || s)
        val derSignature = signature.sign()
        val rawSignature = CryptoUtils.derToRawSignature(derSignature)

        // BIP-62 low-S normalization per protocol-spec §3.3
        val normalizedSignature = CryptoUtils.normalizeLowS(rawSignature)

        Timber.d("Signature normalized: low-S=${CryptoUtils.isLowS(normalizedSignature)}")
        return normalizedSignature
    }

    data class SignatureResult(
        val signatureBase64: String,
        val timestamp: String
    )

    class SigningException(message: String) : Exception(message)
}
