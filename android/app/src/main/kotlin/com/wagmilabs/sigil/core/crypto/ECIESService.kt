package com.wagmilabs.sigil.core.crypto

import org.bouncycastle.crypto.agreement.ECDHBasicAgreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.bouncycastle.math.ec.ECPoint
import java.security.*
import java.security.spec.ECGenParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class ECIESService {

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun encrypt(plaintext: ByteArray, recipientPublicKey: ByteArray): ByteArray {
        require(recipientPublicKey.size == 33) { "Recipient public key must be 33 bytes (compressed P-256)" }

        val curve = ECNamedCurveTable.getParameterSpec("secp256r1")
        val point = curve.curve.decodePoint(recipientPublicKey)
        val recipientPubKeySpec = ECPublicKeySpec(point, curve)
        val keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        val recipientKey = keyFactory.generatePublic(recipientPubKeySpec)

        val ephemeralKeyPair = generateEphemeralKeyPair()
        val ephemeralPublicCompressed = compressPublicKey(ephemeralKeyPair.public)

        val keyAgreement = KeyAgreement.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
        keyAgreement.init(ephemeralKeyPair.private)
        keyAgreement.doPhase(recipientKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        val fingerprint = MessageDigest.getInstance("SHA-256").digest(recipientPublicKey)

        val aesKey = deriveAESKey(sharedSecret, fingerprint, "SIGIL-CONV-V1-AES256")

        val nonce = ByteArray(12)
        SecureRandom().nextBytes(nonce)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, nonce)
        val secretKey = SecretKeySpec(aesKey, "AES")

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        cipher.updateAAD(ephemeralPublicCompressed)
        val ciphertextWithTag = cipher.doFinal(plaintext)

        val ciphertextBody = ciphertextWithTag.copyOfRange(0, ciphertextWithTag.size - 16)
        val tag = ciphertextWithTag.copyOfRange(ciphertextWithTag.size - 16, ciphertextWithTag.size)

        return ephemeralPublicCompressed + nonce + ciphertextBody + tag
    }

    fun decrypt(ciphertext: ByteArray, recipientPrivateKey: PrivateKey, recipientPublicKey: ByteArray): ByteArray {
        require(ciphertext.size >= 33 + 12 + 16) { "Ciphertext too short" }
        require(recipientPublicKey.size == 33) { "Recipient public key must be 33 bytes (compressed P-256)" }

        val ephemeralPublicCompressed = ciphertext.copyOfRange(0, 33)
        val nonce = ciphertext.copyOfRange(33, 45)
        val ciphertextBody = ciphertext.copyOfRange(45, ciphertext.size - 16)
        val tag = ciphertext.copyOfRange(ciphertext.size - 16, ciphertext.size)

        val isHardwareBacked = recipientPrivateKey.javaClass.name.contains("android.security.keystore") ||
                                recipientPrivateKey.javaClass.name.contains("AndroidKeyStore")

        val sharedSecret = if (isHardwareBacked) {
            val keyFactory = KeyFactory.getInstance("EC", "AndroidKeyStore")
            val x509EncodedKey = convertCompressedToX509(ephemeralPublicCompressed)
            val ephemeralPublicKey = keyFactory.generatePublic(java.security.spec.X509EncodedKeySpec(x509EncodedKey))

            val keyAgreement = KeyAgreement.getInstance("ECDH", "AndroidKeyStore")
            keyAgreement.init(recipientPrivateKey)
            keyAgreement.doPhase(ephemeralPublicKey, true)
            keyAgreement.generateSecret()
        } else {
            val curve = ECNamedCurveTable.getParameterSpec("secp256r1")
            val ephemeralPoint = curve.curve.decodePoint(ephemeralPublicCompressed)
            val ephemeralPubKeySpec = ECPublicKeySpec(ephemeralPoint, curve)
            val keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
            val ephemeralPublicKey = keyFactory.generatePublic(ephemeralPubKeySpec)

            val keyAgreement = KeyAgreement.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
            keyAgreement.init(recipientPrivateKey)
            keyAgreement.doPhase(ephemeralPublicKey, true)
            keyAgreement.generateSecret()
        }

        val fingerprint = MessageDigest.getInstance("SHA-256").digest(recipientPublicKey)

        val aesKey = deriveAESKey(sharedSecret, fingerprint, "SIGIL-CONV-V1-AES256")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, nonce)
        val secretKey = SecretKeySpec(aesKey, "AES")

        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        cipher.updateAAD(ephemeralPublicCompressed)

        val ciphertextWithTag = ciphertextBody + tag

        return cipher.doFinal(ciphertextWithTag)
    }

    private fun generateEphemeralKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
        return keyPairGenerator.generateKeyPair()
    }

    private fun compressPublicKey(publicKey: PublicKey): ByteArray {
        val ecPublicKey = publicKey as org.bouncycastle.jce.interfaces.ECPublicKey
        val point = ecPublicKey.q
        return point.getEncoded(true)
    }

    private fun deriveAESKey(sharedSecret: ByteArray, salt: ByteArray, info: String): ByteArray {
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        val params = HKDFParameters(sharedSecret, salt, info.toByteArray())
        hkdf.init(params)

        val output = ByteArray(32)
        hkdf.generateBytes(output, 0, output.size)
        return output
    }

    private fun convertCompressedToX509(compressedKey: ByteArray): ByteArray {
        val curve = ECNamedCurveTable.getParameterSpec("secp256r1")
        val point = curve.curve.decodePoint(compressedKey)
        val pubKeySpec = ECPublicKeySpec(point, curve)
        val keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        val publicKey = keyFactory.generatePublic(pubKeySpec)
        return publicKey.encoded
    }
}
