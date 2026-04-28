package com.wagmilabs.sigil.core.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

class Argon2Service {

    fun deriveKey(
        password: ByteArray,
        salt: ByteArray,
        memoryKiB: Int,
        iterations: Int,
        parallelism: Int,
        outputLength: Int
    ): ByteArray {
        require(salt.size == 16) { "Salt must be 16 bytes, got ${salt.size}" }
        require(memoryKiB > 0) { "Memory must be positive" }
        require(iterations > 0) { "Iterations must be positive" }
        require(parallelism > 0) { "Parallelism must be positive" }
        require(outputLength > 0) { "Output length must be positive" }

        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(salt)
            .withIterations(iterations)
            .withMemoryAsKB(memoryKiB)
            .withParallelism(parallelism)
            .build()

        val generator = Argon2BytesGenerator()
        generator.init(params)

        val output = ByteArray(outputLength)
        generator.generateBytes(password, output)

        return output
    }
}

fun deriveSessionPictogramKey(
    serverPublicKey: ByteArray,
    clientPublicKey: ByteArray,
    serverNonce: ByteArray
): ByteArray {
    require(serverPublicKey.size == 33) { "Server public key must be 33 bytes (compressed P-256)" }
    require(clientPublicKey.size == 33) { "Client public key must be 33 bytes (compressed P-256)" }
    require(serverNonce.size == 32) { "Server nonce must be 32 bytes" }

    val password = taggedHash(
        domain = DomainTag.PAIR,
        message = serverPublicKey + clientPublicKey + serverNonce
    )

    val salt = DomainTag.PAIR.bytes

    val argon2 = Argon2Service()
    return argon2.deriveKey(
        password = password,
        salt = salt,
        memoryKiB = 65536,
        iterations = 10,
        parallelism = 1,
        outputLength = 32
    )
}
