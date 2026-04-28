import Foundation
import CryptoKit

/// Production implementation of CryptoService
/// Per protocol-spec §3.6 (pictogram), §1.2 (key formats), §1.3 (signatures)
struct DefaultCryptoService: CryptoService {

    // MARK: - Public Key Compression

    func compressPublicKey(_ uncompressed: Data) throws -> Data {
        guard uncompressed.count == 65, uncompressed.first == 0x04 else {
            throw CryptoError.invalidPublicKeyFormat
        }

        let x = uncompressed[1..<33]
        let y = uncompressed[33..<65]

        let prefix: UInt8 = y.last! & 1 == 0 ? 0x02 : 0x03

        return Data([prefix]) + x
    }

    // MARK: - Pictogram Derivation

    func derivePictogram(from fingerprint: Data) -> Pictogram {
        precondition(fingerprint.count == 32, "Fingerprint must be 32 bytes (SHA-256)")

        let first4Bytes = fingerprint.prefix(4)
        let bits = UInt32(bigEndian: first4Bytes.withUnsafeBytes { $0.load(as: UInt32.self) })

        let indices = [
            Int((bits >> 26) & 0x3F),
            Int((bits >> 20) & 0x3F),
            Int((bits >> 14) & 0x3F),
            Int((bits >> 8) & 0x3F),
            Int((bits >> 2) & 0x3F)
        ]

        let emojis = indices.map { ProtocolConstants.EMOJI_LIST[$0] }
        let names = indices.map { ProtocolConstants.EMOJI_NAMES[$0] }
        let speakable = names.joined(separator: " ")

        return Pictogram(emojis: emojis, speakable: speakable)
    }

    // MARK: - Fingerprint Computation

    func computeFingerprint(of publicKey: Data) -> Data {
        return Data(SHA256.hash(data: publicKey))
    }

    // MARK: - Signature Verification

    func verifySignature(_ signature: Data, for payload: Data, domain: DomainTag, publicKey: Data) throws -> Bool {
        guard signature.count == 64 else {
            throw CryptoError.signatureVerificationFailed
        }

        guard publicKey.count == 33, [0x02, 0x03].contains(publicKey.first!) else {
            throw CryptoError.invalidPublicKeyFormat
        }

        let sData = signature[32..<64]
        guard isLowS(sData) else {
            throw CryptoError.signatureVerificationFailed
        }

        // Domain-separated hash per api/domain-separation.md
        let digest = taggedHash(domain: domain, message: payload)

        let publicKeyObj = try P256.Signing.PublicKey(compressedRepresentation: publicKey)

        let ecdsaSignature = try P256.Signing.ECDSASignature(rawRepresentation: signature)

        return publicKeyObj.isValidSignature(ecdsaSignature, for: digest)
    }

    private func isLowS(_ sBytes: Data) -> Bool {
        let halfOrder = P256_HALF_ORDER_BYTES

        for i in 0..<32 {
            if sBytes[i] < halfOrder[i] {
                return true
            } else if sBytes[i] > halfOrder[i] {
                return false
            }
        }
        return true
    }
}

// MARK: - P-256 Constants

private let P256_HALF_ORDER_BYTES: [UInt8] = [
    0x7F, 0xFF, 0xFF, 0xFF, 0x80, 0x00, 0x00, 0x00,
    0x7F, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
    0xDE, 0x73, 0x7D, 0x56, 0xD3, 0x8B, 0xCF, 0x42,
    0x79, 0xDC, 0xE5, 0x61, 0x7E, 0x31, 0x92, 0xA8
]

// MARK: - Emoji List (Protocol-Spec §3.6)
// Moved to ProtocolConstants.swift for shared access across crypto + UI

// MARK: - Utilities

extension Data {
    init?(hexString: String) {
        let len = hexString.count / 2
        var data = Data(capacity: len)
        for i in 0..<len {
            let start = hexString.index(hexString.startIndex, offsetBy: i*2)
            let end = hexString.index(start, offsetBy: 2)
            let bytes = hexString[start..<end]
            if let num = UInt8(bytes, radix: 16) {
                data.append(num)
            } else {
                return nil
            }
        }
        self = data
    }
}
