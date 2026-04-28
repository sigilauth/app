package com.wagmilabs.sigil.core.crypto

import android.content.Context
import com.wagmilabs.sigil.core.models.PictogramPool
import java.security.MessageDigest

data class SessionPictogram(
    val emojis: List<String>,
    val names: List<String>,
    val speakable: String
) {
    init {
        require(emojis.size == 6) { "Session pictogram must have exactly 6 emojis" }
        require(names.size == 6) { "Session pictogram must have exactly 6 names" }
    }
}

class SessionPictogramDerivation(
    private val context: Context,
    private val argon2: Argon2Service = Argon2Service()
) {

    fun derive(
        serverPublicKey: ByteArray,
        clientPublicKey: ByteArray,
        serverNonce: ByteArray
    ): SessionPictogram {
        require(serverPublicKey.size == 33) { "Server public key must be 33 bytes (compressed P-256)" }
        require(clientPublicKey.size == 33) { "Client public key must be 33 bytes (compressed P-256)" }
        require(serverNonce.size == 32) { "Server nonce must be 32 bytes" }

        val input = serverPublicKey + clientPublicKey + serverNonce
        val password = MessageDigest.getInstance("SHA-256").digest(input)

        val salt = DomainTag.PAIR.bytes

        val derived = argon2.deriveKey(
            password = password,
            salt = salt,
            memoryKiB = 65536,
            iterations = 10,
            parallelism = 1,
            outputLength = 32
        )

        val pool = PictogramPool.getInstance(context)
        val emojis = mutableListOf<String>()
        val names = mutableListOf<String>()

        for (i in 0 until 6) {
            val offset = i * 2
            val wordIndex = ((derived[offset].toInt() and 0xFF) shl 8) or (derived[offset + 1].toInt() and 0xFF)
            val poolIndex = wordIndex % pool.count

            val entry = pool.entry(poolIndex) ?: throw IllegalStateException("Pool index out of bounds: $poolIndex")

            emojis.add(entry.first)
            names.add(entry.second)
        }

        return SessionPictogram(
            emojis = emojis,
            names = names,
            speakable = names.joinToString(" ")
        )
    }
}
