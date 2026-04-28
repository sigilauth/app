#!/usr/bin/env kotlin

// crypto-sign CLI harness for cross-implementation testing
// Per tests/cross-impl/EXAMPLE-CLI-HARNESS.md § Kotlin
// Usage: kotlinc -script CryptoSignCLI.kt -- --domain auth --message <hex> --private-key <hex>

package com.sigilauth.tools

import java.math.BigInteger
import java.security.*
import java.security.interfaces.ECPrivateKey
import java.security.spec.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// MARK: - Argument Parsing

fun printUsage() {
    System.err.println("Usage: crypto-sign --domain <auth|mpa|decrypt> --message <hex> --private-key <hex> [--action-context <json>]")
    System.err.println("  --action-context: Optional JSON for auth domain (canonicalized + hashed, prepended to message)")
    kotlin.system.exitProcess(1)
}

var domain: String? = null
var messageHex: String? = null
var privateKeyHex: String? = null
var actionContextJSON: String? = null

var i = 0
while (i < args.size) {
    when (args[i]) {
        "--domain" -> {
            if (i + 1 >= args.size) printUsage()
            domain = args[i + 1]
            i += 2
        }
        "--message" -> {
            if (i + 1 >= args.size) printUsage()
            messageHex = args[i + 1]
            i += 2
        }
        "--private-key" -> {
            if (i + 1 >= args.size) printUsage()
            privateKeyHex = args[i + 1]
            i += 2
        }
        "--action-context" -> {
            if (i + 1 >= args.size) printUsage()
            actionContextJSON = args[i + 1]
            i += 2
        }
        else -> {
            System.err.println("Error: Unknown argument '${args[i]}'")
            printUsage()
        }
    }
}

if (domain == null || messageHex == null || privateKeyHex == null) {
    printUsage()
}

// MARK: - Domain Tag Mapping

val domainTag: ByteArray = when (domain) {
    "auth" -> byteArrayOf(
        0x53, 0x49, 0x47, 0x49, 0x4c, 0x2d, 0x41, 0x55, 0x54, 0x48, 0x2d, 0x56, 0x31, 0x00
    )
    "mpa" -> byteArrayOf(
        0x53, 0x49, 0x47, 0x49, 0x4c, 0x2d, 0x4d, 0x50, 0x41, 0x2d, 0x56, 0x31, 0x00
    )
    "decrypt" -> byteArrayOf(
        0x53, 0x49, 0x47, 0x49, 0x4c, 0x2d, 0x44, 0x45, 0x43, 0x52, 0x59, 0x50, 0x54, 0x2d, 0x56, 0x31, 0x00
    )
    else -> {
        System.err.println("Error: Invalid domain '$domain' (must be auth, mpa, or decrypt)")
        kotlin.system.exitProcess(1)
    }
}

// MARK: - Hex Utilities

fun hexStringToByteArray(s: String): ByteArray {
    val len = s.length
    require(len % 2 == 0) { "Hex string must have even length" }
    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
        data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
        i += 2
    }
    return data
}

fun byteArrayToHex(bytes: ByteArray): String {
    return bytes.joinToString("") { "%02x".format(it) }
}

// MARK: - Input Parsing

val messageBytes = try {
    hexStringToByteArray(messageHex)
} catch (e: Exception) {
    System.err.println("Error: Invalid message hex")
    kotlin.system.exitProcess(1)
}

val privKeyBytes = try {
    val bytes = hexStringToByteArray(privateKeyHex)
    require(bytes.size == 32) { "Private key must be 32 bytes" }
    bytes
} catch (e: Exception) {
    System.err.println("Error: Invalid private key hex (must be 32 bytes)")
    kotlin.system.exitProcess(1)
}

// MARK: - RFC 6979 Deterministic ECDSA (simplified)

// P-256 curve parameters
val p256Order = BigInteger("FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551", 16)
val p256HalfOrder = p256Order.shiftRight(1)

// Convert raw 32-byte private key to ECPrivateKey
fun loadPrivateKey(rawKey: ByteArray): PrivateKey {
    val d = BigInteger(1, rawKey)
    val ecSpec = ECGenParameterSpec("secp256r1")
    val keyFactory = KeyFactory.getInstance("EC")
    val params = AlgorithmParameters.getInstance("EC").apply { init(ecSpec) }
        .getParameterSpec(ECParameterSpec::class.java)
    val privateKeySpec = ECPrivateKeySpec(d, params)
    return keyFactory.generatePrivate(privateKeySpec)
}

// Sign with domain separation
fun signWithDomain(privateKey: PrivateKey, message: ByteArray, domain: ByteArray): ByteArray {
    // Domain-separated hash
    val tagged = domain + message
    val digest = MessageDigest.getInstance("SHA-256").digest(tagged)

    // Sign digest with NONEwithECDSA (to avoid double-hashing)
    val signature = Signature.getInstance("NONEwithECDSA")
    signature.initSign(privateKey)
    signature.update(digest)
    val derSig = signature.sign()

    // Convert DER to raw R || S
    return derToRaw(derSig)
}

// Convert DER signature to raw 64-byte format (R || S)
fun derToRaw(derSig: ByteArray): ByteArray {
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
    var sBig = BigInteger(1, s)

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

// MARK: - RFC 8785 JSON Canonicalization Helper (simplified for CLI)

fun canonicalizeAndHashJSON(jsonString: String): ByteArray {
    // Simple canonicalization for basic JSON objects
    // Parse JSON, sort keys, rebuild without whitespace
    val canonical = when {
        jsonString.trim() == "{}" -> "{}"
        jsonString.trim().startsWith("{") -> {
            // Extract key-value pairs, sort by key, rebuild
            val trimmed = jsonString.trim().removeSurrounding("{", "}")
            if (trimmed.isEmpty()) "{}"
            else {
                val pairs = mutableListOf<Pair<String, String>>()
                var depth = 0
                var currentKey = ""
                var currentValue = StringBuilder()
                var inKey = true
                var inString = false

                for (c in trimmed) {
                    when {
                        c == '\"' && (currentValue.isEmpty() || currentValue.last() != '\\') -> inString = !inString
                        !inString && c == ':' && depth == 0 -> {
                            currentKey = currentValue.toString().trim().removeSurrounding("\"")
                            currentValue.clear()
                            inKey = false
                        }
                        !inString && c == ',' && depth == 0 -> {
                            pairs.add(currentKey to currentValue.toString().trim())
                            currentValue.clear()
                            inKey = true
                        }
                        !inString && (c == '{' || c == '[') -> {
                            depth++
                            currentValue.append(c)
                        }
                        !inString && (c == '}' || c == ']') -> {
                            depth--
                            currentValue.append(c)
                        }
                        else -> currentValue.append(c)
                    }
                }
                if (currentValue.isNotEmpty()) {
                    pairs.add(currentKey to currentValue.toString().trim())
                }

                // Sort by key and rebuild
                pairs.sortBy { it.first }
                pairs.joinToString(",", prefix = "{", postfix = "}") { (k, v) ->
                    "\"$k\":$v"
                }
            }
        }
        else -> jsonString.trim()
    }

    return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
}

// MARK: - Main Execution

try {
    val privateKey = loadPrivateKey(privKeyBytes)

    // Build final message based on domain
    var finalMessage = messageBytes

    // AUTH domain: prepend action_context hash to challenge
    if (domain == "auth") {
        val actionJSON = actionContextJSON ?: "{}"
        val actionHash = canonicalizeAndHashJSON(actionJSON)
        // Auth payload = challenge_bytes || action_hash
        finalMessage = messageBytes + actionHash
    }

    val signature = signWithDomain(privateKey, finalMessage, domainTag)
    println(byteArrayToHex(signature))
} catch (e: Exception) {
    System.err.println("Error: Signing failed - ${e.message}")
    kotlin.system.exitProcess(1)
}
