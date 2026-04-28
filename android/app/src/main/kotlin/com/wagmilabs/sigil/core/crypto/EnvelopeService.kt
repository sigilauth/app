package com.wagmilabs.sigil.core.crypto

import android.util.Base64
import com.google.gson.Gson
import com.wagmilabs.sigil.core.utilities.CanonicalJSON
import java.security.PrivateKey
import java.security.Signature
import java.security.MessageDigest

data class EnvelopePayload(
    val action: String,
    val body: Map<String, Any>? = null,
    val timestamp: Long,
    val nonce: String,
    val audience: String? = null
)

data class OuterEnvelope(
    val envelope: String
)

class EnvelopeService(
    private val ecies: ECIESService = ECIESService()
) {

    fun createRequest(
        payload: EnvelopePayload,
        clientPrivateKey: PrivateKey,
        clientPublicKey: ByteArray,
        serverPublicKey: ByteArray
    ): String {
        val payloadMap = mapOf(
            "action" to payload.action,
            "body" to (payload.body ?: emptyMap<String, Any>()),
            "timestamp" to payload.timestamp,
            "nonce" to payload.nonce,
            "audience" to (payload.audience ?: "")
        )

        val canonicalPayload = CanonicalJSON.canonicalize(payloadMap)

        val signature = signPayload(
            canonicalPayload.toByteArray(),
            clientPrivateKey,
            DomainTag.CONV
        )

        val innerMap = mapOf(
            "client_public_key" to Base64.encodeToString(clientPublicKey, Base64.NO_WRAP),
            "payload" to canonicalPayload,
            "signature" to Base64.encodeToString(signature, Base64.NO_WRAP)
        )

        val innerJSON = CanonicalJSON.canonicalize(innerMap)

        val outerCiphertext = ecies.encrypt(innerJSON.toByteArray(), serverPublicKey)

        val outerMap = mapOf(
            "envelope" to Base64.encodeToString(outerCiphertext, Base64.NO_WRAP)
        )

        return Gson().toJson(outerMap)
    }

    fun verifyResponse(
        envelopeJSON: String,
        serverPublicKey: ByteArray,
        clientPrivateKey: PrivateKey,
        clientPublicKey: ByteArray
    ): Map<String, Any> {
        val outerMap = Gson().fromJson(envelopeJSON, Map::class.java) as Map<String, Any>
        val envelopeB64 = outerMap["envelope"] as? String
            ?: throw IllegalArgumentException("Missing envelope field")

        val outerCiphertext = Base64.decode(envelopeB64, Base64.NO_WRAP)

        val innerJSON = ecies.decrypt(outerCiphertext, clientPrivateKey, clientPublicKey)

        val innerMap = Gson().fromJson(String(innerJSON), Map::class.java) as Map<String, Any>

        val serverPubB64 = innerMap["server_public_key"] as? String
            ?: throw IllegalArgumentException("Missing server_public_key")
        val payloadStr = innerMap["payload"] as? String
            ?: throw IllegalArgumentException("Missing payload")
        val signatureB64 = innerMap["signature"] as? String
            ?: throw IllegalArgumentException("Missing signature")

        val serverPubDecoded = Base64.decode(serverPubB64, Base64.NO_WRAP)
        val payloadBytes = payloadStr.toByteArray()
        val signature = Base64.decode(signatureB64, Base64.NO_WRAP)

        require(serverPubDecoded.contentEquals(serverPublicKey)) {
            "Server public key mismatch"
        }

        val isValid = verifySignature(
            signature,
            payloadBytes,
            serverPublicKey,
            DomainTag.CONV
        )

        require(isValid) { "Signature verification failed" }

        val payload = Gson().fromJson(payloadStr, Map::class.java) as Map<String, Any>

        val timestamp = (payload["timestamp"] as? Double)?.toLong()
            ?: throw IllegalArgumentException("Missing or invalid timestamp")

        val now = System.currentTimeMillis() / 1000
        require(kotlin.math.abs(now - timestamp) <= 300) {
            "Timestamp expired (now=$now, msg=$timestamp)"
        }

        return payload
    }

    private fun signPayload(payload: ByteArray, privateKey: PrivateKey, domain: DomainTag): ByteArray {
        val tagged = domain.bytes + payload
        val digest = MessageDigest.getInstance("SHA-256").digest(tagged)

        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(digest)

        val rawSig = signature.sign()

        return convertDERtoRaw(rawSig)
    }

    private fun verifySignature(
        signature: ByteArray,
        payload: ByteArray,
        publicKey: ByteArray,
        domain: DomainTag
    ): Boolean {
        val tagged = domain.bytes + payload
        val digest = MessageDigest.getInstance("SHA-256").digest(tagged)

        val curve = org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("secp256r1")
        val point = curve.curve.decodePoint(publicKey)
        val pubKeySpec = org.bouncycastle.jce.spec.ECPublicKeySpec(point, curve)
        val keyFactory = java.security.KeyFactory.getInstance("EC", org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME)
        val pubKey = keyFactory.generatePublic(pubKeySpec)

        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(pubKey)
        verifier.update(digest)

        val derSig = convertRawtoDER(signature)

        return verifier.verify(derSig)
    }

    private fun convertDERtoRaw(derSignature: ByteArray): ByteArray {
        var offset = 2
        if (derSignature[offset].toInt() and 0x80 != 0) {
            offset += (derSignature[offset].toInt() and 0x7F)
        }
        offset++

        val rLength = derSignature[offset++].toInt()
        val r = derSignature.copyOfRange(offset, offset + rLength)
        offset += rLength + 1

        val sLength = derSignature[offset++].toInt()
        val s = derSignature.copyOfRange(offset, offset + sLength)

        val rPadded = r.takeLast(32).toByteArray().let {
            ByteArray(32 - it.size) + it
        }
        val sPadded = s.takeLast(32).toByteArray().let {
            ByteArray(32 - it.size) + it
        }

        return rPadded + sPadded
    }

    private fun convertRawtoDER(rawSignature: ByteArray): ByteArray {
        val r = rawSignature.copyOfRange(0, 32)
        val s = rawSignature.copyOfRange(32, 64)

        fun encodeBigInt(value: ByteArray): ByteArray {
            val stripped = value.dropWhile { it == 0.toByte() }.toByteArray()
            val needsPadding = stripped.isNotEmpty() && (stripped[0].toInt() and 0x80) != 0
            val encoded = if (needsPadding) byteArrayOf(0) + stripped else stripped
            return byteArrayOf(0x02, encoded.size.toByte()) + encoded
        }

        val rEncoded = encodeBigInt(r)
        val sEncoded = encodeBigInt(s)

        val totalLength = rEncoded.size + sEncoded.size
        return byteArrayOf(0x30, totalLength.toByte()) + rEncoded + sEncoded
    }
}
