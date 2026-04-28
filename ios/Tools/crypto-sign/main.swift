#!/usr/bin/env swift

// crypto-sign CLI harness for cross-implementation testing
// Per tests/cross-impl/EXAMPLE-CLI-HARNESS.md § Swift
// Usage: swift main.swift --domain auth --message <hex> --private-key <hex>

import Foundation
import CryptoKit

// MARK: - Argument Parsing

func printUsage() {
    print("Usage: crypto-sign --domain <auth|mpa|decrypt> --message <hex> --private-key <hex> [--action-context <json>]")
    print("  --action-context: Optional JSON for auth domain (canonicalized + hashed, prepended to message)")
    exit(1)
}

var domain: String?
var messageHex: String?
var privateKeyHex: String?
var actionContextJSON: String?

var i = 1
while i < CommandLine.arguments.count {
    switch CommandLine.arguments[i] {
    case "--domain":
        guard i + 1 < CommandLine.arguments.count else { printUsage() }
        domain = CommandLine.arguments[i + 1]
        i += 2
    case "--message":
        guard i + 1 < CommandLine.arguments.count else { printUsage() }
        messageHex = CommandLine.arguments[i + 1]
        i += 2
    case "--private-key":
        guard i + 1 < CommandLine.arguments.count else { printUsage() }
        privateKeyHex = CommandLine.arguments[i + 1]
        i += 2
    case "--action-context":
        guard i + 1 < CommandLine.arguments.count else { printUsage() }
        actionContextJSON = CommandLine.arguments[i + 1]
        i += 2
    default:
        fputs("Error: Unknown argument '\(CommandLine.arguments[i])'\n", stderr)
        printUsage()
    }
}

guard let domain = domain,
      let messageHex = messageHex,
      let privateKeyHex = privateKeyHex else {
    printUsage()
}

// MARK: - Domain Tag Mapping

let domainTag: Data
switch domain {
case "auth":
    // SIGIL-AUTH-V1\0 - 15 bytes
    domainTag = Data([0x53, 0x49, 0x47, 0x49, 0x4c, 0x2d, 0x41, 0x55, 0x54, 0x48, 0x2d, 0x56, 0x31, 0x00])
case "mpa":
    // SIGIL-MPA-V1\0 - 14 bytes
    domainTag = Data([0x53, 0x49, 0x47, 0x49, 0x4c, 0x2d, 0x4d, 0x50, 0x41, 0x2d, 0x56, 0x31, 0x00])
case "decrypt":
    // SIGIL-DECRYPT-V1\0 - 18 bytes
    domainTag = Data([0x53, 0x49, 0x47, 0x49, 0x4c, 0x2d, 0x44, 0x45, 0x43, 0x52, 0x59, 0x50, 0x54, 0x2d, 0x56, 0x31, 0x00])
default:
    fputs("Error: Invalid domain '\(domain)' (must be auth, mpa, or decrypt)\n", stderr)
    exit(1)
}

// MARK: - Hex Decoding

extension Data {
    init?(hexString: String) {
        let len = hexString.count / 2
        var data = Data(capacity: len)
        var i = hexString.startIndex
        for _ in 0..<len {
            let j = hexString.index(i, offsetBy: 2)
            let bytes = hexString[i..<j]
            if let num = UInt8(bytes, radix: 16) {
                data.append(num)
            } else {
                return nil
            }
            i = j
        }
        self = data
    }

    var hexString: String {
        map { String(format: "%02x", $0) }.joined()
    }
}

guard let messageBytes = Data(hexString: messageHex) else {
    fputs("Error: Invalid message hex\n", stderr)
    exit(1)
}

guard let privKeyBytes = Data(hexString: privateKeyHex), privKeyBytes.count == 32 else {
    fputs("Error: Invalid private key hex (must be 32 bytes)\n", stderr)
    exit(1)
}

// MARK: - RFC 8785 JSON Canonicalization Helper

func canonicalizeAndHashJSON(_ jsonString: String) -> Data? {
    guard let jsonData = jsonString.data(using: .utf8) else { return nil }
    guard let jsonObject = try? JSONSerialization.jsonObject(with: jsonData) else { return nil }
    guard let canonical = try? JSONSerialization.data(withJSONObject: jsonObject, options: [.sortedKeys, .withoutEscapingSlashes]) else { return nil }
    return Data(SHA256.hash(data: canonical))
}

// MARK: - Signing with Domain Separation

do {
    // Load private key
    let privKey = try P256.Signing.PrivateKey(rawRepresentation: privKeyBytes)

    // Build final message based on domain
    var finalMessage = messageBytes

    // AUTH domain: prepend action_context hash to challenge
    if domain == "auth" {
        let actionJSON = actionContextJSON ?? "{}"
        guard let actionHash = canonicalizeAndHashJSON(actionJSON) else {
            fputs("Error: Failed to canonicalize action_context JSON\n", stderr)
            exit(1)
        }
        // Auth payload = challenge_bytes || action_hash
        var authPayload = messageBytes
        authPayload.append(actionHash)
        finalMessage = authPayload
    }

    // Domain-separated hash: tagged = domain || message, hash = SHA256(tagged)
    var tagged = domainTag
    tagged.append(finalMessage)
    let hash = SHA256.hash(data: tagged)

    // Sign the digest
    let signature = try privKey.signature(for: Data(hash))

    // Output R || S (64 bytes) as hex
    let rawSig = signature.rawRepresentation
    print(rawSig.hexString)

} catch {
    fputs("Error: Signing failed - \(error)\n", stderr)
    exit(1)
}
