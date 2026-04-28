package com.wagmilabs.sigil.core.crypto

/**
 * Android KeyStore management for device keypair
 * Hardware-backed key storage (StrongBox on supported devices)
 */
interface KeyStoreService {
    /**
     * Generate P-256 keypair in Android KeyStore with biometric access control
     * @param alias Unique key identifier
     * @return Compressed public key (33 bytes)
     * @throws KeyStoreException if generation fails or StrongBox unavailable
     */
    suspend fun generateDeviceKeypair(alias: String): ByteArray

    /**
     * Sign payload with device private key (biometric gate triggered)
     * Domain separation per api/domain-separation.md
     * @param payload Data to sign (will be tagged with domain)
     * @param domain Domain tag (auth, mpa, decrypt, conv, pair)
     * @param keyAlias Key identifier
     * @return ECDSA signature (64 bytes, fixed format)
     * @throws KeyStoreException if signing fails or biometric cancelled
     */
    suspend fun sign(payload: ByteArray, domain: DomainTag, keyAlias: String): ByteArray

    /**
     * Delete device keypair from Android KeyStore
     */
    fun deleteKey(alias: String)

    /**
     * Check if StrongBox is available on this device
     */
    val isStrongBoxAvailable: Boolean
}

sealed class KeyStoreException : Exception() {
    object StrongBoxUnavailable : KeyStoreException()
    data class KeyGenerationFailed(override val cause: Throwable?) : KeyStoreException()
    data class KeyNotFound(val alias: String) : KeyStoreException()
    object BiometricCancelled : KeyStoreException()
    data class SigningFailed(override val cause: Throwable?) : KeyStoreException()
    data class DeletionFailed(val status: Int) : KeyStoreException()
}
