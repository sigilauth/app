package com.wagmilabs.sigil.core.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import timber.log.Timber
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec

/**
 * Manages Android Keystore operations with StrongBox backing.
 *
 * Per knox-threat-model.md:
 * - Hardware key extraction must be infeasible
 * - Biometric gate on every signing operation
 * - Private keys never leave hardware
 *
 * AGPL-3.0 License
 */
class KeystoreManager {

    private val keystore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    /**
     * Detects if device has StrongBox hardware security module.
     *
     * Per nova-mobile-platform-spec.md §2.2:
     * - StrongBox preferred (Pixel 6+, Samsung S21+, OnePlus 9+)
     * - Falls back to TEE if unavailable
     * - Displays warning to user on TEE fallback
     */
    fun isStrongBoxAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Check for StrongBox feature
            // Note: This requires actual hardware to test properly
            try {
                val keyGen = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC,
                    "AndroidKeyStore"
                )
                val spec = KeyGenParameterSpec.Builder(
                    "strongbox_test",
                    KeyProperties.PURPOSE_SIGN
                ).apply {
                    setIsStrongBoxBacked(true)
                    setDigests(KeyProperties.DIGEST_SHA256)
                    setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                }.build()

                keyGen.initialize(spec)
                keyGen.generateKeyPair()

                // If we got here, StrongBox is available
                keystore.deleteEntry("strongbox_test")
                true
            } catch (e: Exception) {
                Timber.w("StrongBox not available: ${e.message}")
                false
            }
        } else {
            false
        }
    }

    /**
     * Generates P-256 keypair in hardware keystore.
     *
     * Per knox-threat-model.md §2.3:
     * - setUserAuthenticationRequired(true)
     * - setUserAuthenticationValidityDurationSeconds(-1) — every use
     * - setInvalidatedByBiometricEnrollment(true)
     * - setIsStrongBoxBacked(true) if available
     *
     * @param alias Unique identifier for this key
     * @return Pair of (PrivateKey handle, PublicKey)
     */
    fun generateDeviceKeypair(alias: String): Pair<PrivateKey, PublicKey> {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            "AndroidKeyStore"
        )

        val strongBoxBacked = isStrongBoxAvailable()

        val parameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN
        ).apply {
            setDigests(KeyProperties.DIGEST_SHA256)
            setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))

            // Knox Top 5 #2: Biometric gate on every signing operation
            setUserAuthenticationRequired(true)
            setUserAuthenticationValidityDurationSeconds(-1) // Every use

            // Invalidate key if biometric enrollment changes
            setInvalidatedByBiometricEnrollment(true)

            // Use StrongBox if available
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && strongBoxBacked) {
                setIsStrongBoxBacked(true)
                Timber.d("Keypair will be StrongBox-backed")
            } else {
                Timber.w("Keypair will use TEE (StrongBox unavailable)")
            }
        }.build()

        keyPairGenerator.initialize(parameterSpec)
        val keyPair = keyPairGenerator.generateKeyPair()

        Timber.d("Generated P-256 keypair with alias: $alias")
        return Pair(keyPair.private, keyPair.public)
    }

    /**
     * Retrieves existing private key from keystore.
     *
     * @param alias Key alias
     * @return PrivateKey handle or null if not found
     */
    fun getPrivateKey(alias: String): PrivateKey? {
        return try {
            keystore.getKey(alias, null) as? PrivateKey
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve private key: $alias")
            null
        }
    }

    /**
     * Retrieves public key from keystore.
     *
     * @param alias Key alias
     * @return PublicKey or null if not found
     */
    fun getPublicKey(alias: String): PublicKey? {
        return try {
            keystore.getCertificate(alias)?.publicKey
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve public key: $alias")
            null
        }
    }

    /**
     * Deletes keypair from keystore.
     *
     * @param alias Key alias
     */
    fun deleteKey(alias: String) {
        try {
            keystore.deleteEntry(alias)
            Timber.d("Deleted key: $alias")
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete key: $alias")
        }
    }

    /**
     * Lists all key aliases in keystore.
     *
     * @return List of key aliases
     */
    fun listKeys(): List<String> {
        return keystore.aliases().toList()
    }
}
