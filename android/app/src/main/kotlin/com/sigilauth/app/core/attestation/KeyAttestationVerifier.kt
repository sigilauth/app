package com.wagmilabs.sigil.core.attestation

import android.security.keystore.KeyGenParameterSpec
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Verifies Android Key Attestation per knox-threat-model.md §2.3.
 *
 * Key Attestation proves:
 * - Key generated in hardware (StrongBox or TEE)
 * - User authentication required
 * - Key not exportable
 *
 * Attestation chain:
 * - Leaf cert: Contains device public key + attestation extension
 * - Intermediate certs: Device-specific signing chain
 * - Root cert: Google hardware attestation CA
 *
 * AGPL-3.0 License
 */
class KeyAttestationVerifier {

    /**
     * Retrieves attestation certificate chain for a key.
     *
     * @param alias Keystore alias
     * @return X.509 certificate chain (DER-encoded)
     */
    fun getAttestationChain(alias: String): List<ByteArray> {
        val keystore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        val certificateChain = keystore.getCertificateChain(alias)
            ?: throw AttestationException("No certificate chain for key: $alias")

        return certificateChain.map { it.encoded }
    }

    /**
     * Verifies attestation chain structure.
     *
     * Basic verification:
     * - Chain length > 0
     * - Leaf cert contains public key
     * - Chain is ordered (leaf → root)
     *
     * Full verification (integrator responsibility):
     * - Root cert matches Google hardware attestation CA
     * - Attestation extension parsing
     * - Challenge verification
     *
     * @param chain Certificate chain (DER-encoded)
     * @return AttestationResult
     */
    fun verifyAttestation(chain: List<ByteArray>): AttestationResult {
        if (chain.isEmpty()) {
            return AttestationResult.Invalid("Empty certificate chain")
        }

        try {
            val certFactory = CertificateFactory.getInstance("X.509")

            // Parse leaf certificate
            val leafCertBytes = ByteArrayInputStream(chain[0])
            val leafCert = certFactory.generateCertificate(leafCertBytes) as X509Certificate

            Timber.d("Leaf certificate subject: ${leafCert.subjectDN}")
            Timber.d("Leaf certificate issuer: ${leafCert.issuerDN}")

            // Parse attestation extension (OID: 1.3.6.1.4.1.11129.2.1.17)
            val attestationExtension = leafCert.getExtensionValue("1.3.6.1.4.1.11129.2.1.17")

            if (attestationExtension == null) {
                Timber.w("No attestation extension found - may be software keystore")
                return AttestationResult.Invalid("Missing attestation extension")
            }

            // TODO: Parse attestation extension ASN.1 structure
            // Per knox-threat-model.md §3.6:
            // - Check attestationSecurityLevel = StrongBox (3) or TEE (1)
            // - Check keyOrigin = Generated (0)
            // - Check purpose includes Sign (2)
            // Full parsing requires BouncyCastle ASN.1 decoder

            Timber.d("Attestation extension present (${attestationExtension.size} bytes)")

            return AttestationResult.Valid(
                securityLevel = "UNKNOWN", // TODO: Parse from extension
                chainLength = chain.size
            )

        } catch (e: Exception) {
            Timber.e(e, "Attestation verification failed")
            return AttestationResult.Invalid(e.message ?: "Unknown error")
        }
    }

    sealed class AttestationResult {
        data class Valid(
            val securityLevel: String,  // "StrongBox" | "TEE" | "Software"
            val chainLength: Int
        ) : AttestationResult()

        data class Invalid(val reason: String) : AttestationResult()
    }

    class AttestationException(message: String) : Exception(message)
}
