package com.wagmilabs.sigil.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.nio.ByteBuffer

/**
 * Production implementation of KeyStoreService
 * Uses Android KeyStore with StrongBox on supported devices
 */
class DefaultKeyStoreService : KeyStoreService {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    override val isStrongBoxAvailable: Boolean
        get() {
            // StrongBox available on Android 9+ (API 28+) on supported hardware
            return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P
        }

    override suspend fun generateDeviceKeypair(alias: String): ByteArray {
        try {
            // Build KeyGenParameterSpec with biometric authentication
            val parameterSpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            ).apply {
                setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1")) // P-256
                setDigests(KeyProperties.DIGEST_SHA256)
                setUserAuthenticationRequired(true)

                // Use StrongBox if available (hardware security module)
                if (isStrongBoxAvailable) {
                    setIsStrongBoxBacked(true)
                }

                // Require biometric or device credential on every use
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    setUserAuthenticationParameters(
                        0, // 0 = auth required for every use
                        KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
                    )
                } else {
                    @Suppress("DEPRECATION")
                    setUserAuthenticationValidityDurationSeconds(-1) // Auth required every time
                }

                // Non-exportable key (stays in hardware)
                setInvalidatedByBiometricEnrollment(true)
            }.build()

            // Generate keypair in Android KeyStore
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                "AndroidKeyStore"
            )
            keyPairGenerator.initialize(parameterSpec)
            val keyPair = keyPairGenerator.generateKeyPair()

            // Extract public key and compress to 33 bytes
            val publicKey = keyPair.public
            val uncompressed = publicKey.encoded // X.509 DER format

            // Parse X.509 to extract raw public key bytes (65 bytes: 0x04 || x || y)
            // Then compress to 33 bytes (0x02/0x03 || x)
            return compressPublicKey(uncompressed)

        } catch (e: Exception) {
            throw KeyStoreException.KeyGenerationFailed(e)
        }
    }

    override suspend fun sign(payload: ByteArray, domain: DomainTag, keyAlias: String): ByteArray {
        try {
            // Domain-separated hash
            val digest = taggedHash(domain, payload)

            // Retrieve private key from KeyStore
            val entry = keyStore.getEntry(keyAlias, null)
            if (entry !is KeyStore.PrivateKeyEntry) {
                throw KeyStoreException.KeyNotFound(keyAlias)
            }

            val privateKey = entry.privateKey

            // Sign the digest
            // IMPORTANT: Use NONEwithECDSA to sign the digest directly (already hashed)
            val signature = Signature.getInstance("NONEwithECDSA")
            signature.initSign(privateKey)
            signature.update(digest)
            val derSignature = signature.sign()

            // Convert DER signature to raw r||s (64 bytes)
            return convertDERToRaw(derSignature)

        } catch (e: android.security.keystore.UserNotAuthenticatedException) {
            throw KeyStoreException.BiometricCancelled
        } catch (e: KeyStoreException) {
            throw e
        } catch (e: Exception) {
            throw KeyStoreException.SigningFailed(e)
        }
    }

    override fun deleteKey(alias: String) {
        try {
            keyStore.deleteEntry(alias)
        } catch (e: Exception) {
            throw KeyStoreException.DeletionFailed(-1)
        }
    }

    // MARK: - Helpers

    /**
     * Compress uncompressed P-256 public key (65 bytes) to compressed format (33 bytes)
     * Uncompressed: 0x04 || x (32 bytes) || y (32 bytes)
     * Compressed: (0x02 if y even, 0x03 if y odd) || x (32 bytes)
     */
    private fun compressPublicKey(x509Encoded: ByteArray): ByteArray {
        // X.509 DER public key structure for EC:
        // SEQUENCE { algorithm, BIT STRING { 0x04 || x || y } }
        // We need to extract the raw 65-byte uncompressed key

        // Find the 0x04 marker (uncompressed point)
        val index = x509Encoded.indexOfFirst { it == 0x04.toByte() }
        if (index == -1 || x509Encoded.size < index + 65) {
            throw IllegalArgumentException("Invalid uncompressed public key format")
        }

        val uncompressed = x509Encoded.sliceArray(index until index + 65)
        val x = uncompressed.sliceArray(1..32)
        val y = uncompressed.sliceArray(33..64)

        // Compressed format: prefix byte (0x02 if y is even, 0x03 if y is odd) + x
        val prefix = if (y.last().toInt() and 1 == 0) 0x02.toByte() else 0x03.toByte()
        return byteArrayOf(prefix) + x
    }

    /**
     * Convert DER-encoded ECDSA signature to raw r||s format
     * DER format: 0x30 [length] 0x02 [r-length] [r] 0x02 [s-length] [s]
     * Raw format: [r (32 bytes)] [s (32 bytes)]
     */
    private fun convertDERToRaw(derSignature: ByteArray): ByteArray {
        var index = 0

        // Check SEQUENCE tag
        if (derSignature[index] != 0x30.toByte()) {
            throw IllegalArgumentException("Invalid DER signature format")
        }
        index++

        // Skip total length
        index++

        // Parse r
        if (derSignature[index] != 0x02.toByte()) {
            throw IllegalArgumentException("Invalid DER signature format")
        }
        index++

        var rLength = derSignature[index].toInt()
        index++

        // Skip leading zero byte if present (for positive numbers)
        var rStart = index
        if (derSignature[index] == 0x00.toByte() && rLength == 33) {
            rStart++
            rLength = 32
        }

        val r = derSignature.sliceArray(rStart until rStart + rLength)
        index = rStart + rLength

        // Parse s
        if (derSignature[index] != 0x02.toByte()) {
            throw IllegalArgumentException("Invalid DER signature format")
        }
        index++

        var sLength = derSignature[index].toInt()
        index++

        // Skip leading zero byte if present
        var sStart = index
        if (derSignature[index] == 0x00.toByte() && sLength == 33) {
            sStart++
            sLength = 32
        }

        val s = derSignature.sliceArray(sStart until sStart + sLength)

        // Pad to 32 bytes if needed
        val rPadded = ByteArray(32 - r.size) + r
        val sPadded = ByteArray(32 - s.size) + s

        return rPadded + sPadded
    }
}
