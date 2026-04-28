import XCTest
@testable import SigilAuthCore

/// TDD: Tests written first per D5 (work-blocks.md)
/// Tests currently FAIL - implementation pending B0 completion
final class CryptoServiceTests: XCTestCase {

    func testCompressPublicKey_65BytesTopubKey() throws {
        // Test vector from protocol-spec §11
        let uncompressed = Data(repeating: 0x04, count: 65) // Placeholder
        // TODO: Real test vector when B0 provides reference impl

        let crypto = DefaultCryptoService() // Not implemented yet
        let compressed = try crypto.compressPublicKey(uncompressed)

        XCTAssertEqual(compressed.count, 33, "Compressed key must be 33 bytes")
        XCTAssertTrue([0x02, 0x03].contains(compressed.first!), "First byte must be 0x02 or 0x03")
    }

    func testDerivePictogram_ProtocolSpecExample() throws {
        // Test vector from /api/test-vectors/pictogram.json
        let fingerprintHex = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2"
        let fingerprint = Data(hexString: fingerprintHex)!

        let crypto = DefaultCryptoService()
        let pictogram = crypto.derivePictogram(from: fingerprint)

        // Expected indices: [40, 27, 11, 3, 53]
        // Expected names: ["tree", "rocket", "mushroom", "orange", "moai"]
        XCTAssertEqual(pictogram.emojis, ["🌲", "🚀", "🍄", "🍊", "🗿"], "Emoji mismatch")
        XCTAssertEqual(pictogram.speakable, "tree rocket mushroom orange moai", "Speakable form uses spaces per D10")
    }

    func testDerivePictogram_AllZeros() throws {
        let fingerprint = Data(repeating: 0, count: 32)

        let crypto = DefaultCryptoService()
        let pictogram = crypto.derivePictogram(from: fingerprint)

        // All zeros -> indices [0,0,0,0,0] -> all "apple"
        XCTAssertEqual(pictogram.emojis, ["🍎", "🍎", "🍎", "🍎", "🍎"])
        XCTAssertEqual(pictogram.speakable, "apple apple apple apple apple")
    }

    func testDerivePictogram_AllOnes() throws {
        let fingerprint = Data(repeating: 0xFF, count: 32)

        let crypto = DefaultCryptoService()
        let pictogram = crypto.derivePictogram(from: fingerprint)

        // All 0xFF -> indices [63,63,63,63,63] -> all "fire"
        XCTAssertEqual(pictogram.emojis, ["🔥", "🔥", "🔥", "🔥", "🔥"])
        XCTAssertEqual(pictogram.speakable, "fire fire fire fire fire")
    }

    func testComputeFingerprint_SHA256() {
        let publicKey = Data(repeating: 0xAB, count: 33)

        let crypto = DefaultCryptoService()
        let fingerprint = crypto.computeFingerprint(of: publicKey)

        XCTAssertEqual(fingerprint.count, 32, "SHA-256 produces 32 bytes")
    }

    func testVerifySignature_ValidSignature() throws {
        let crypto = DefaultCryptoService()
        let message = "test message for signing".data(using: .utf8)!

        let privateKey = try P256.Signing.PrivateKey(rawRepresentation: Data(repeating: 0xC6, count: 32))
        let publicKey = privateKey.publicKey
        let compressedPublicKey = try crypto.compressPublicKey(publicKey.x963Representation)

        let signature = try privateKey.signature(for: message)
        let rawSignature = signature.rawRepresentation

        let result = try crypto.verifySignature(rawSignature, for: message, publicKey: compressedPublicKey)
        XCTAssertTrue(result, "Valid signature must verify")
    }

    func testVerifySignature_InvalidSignatureTooShort() throws {
        let crypto = DefaultCryptoService()
        let message = Data("test".utf8)
        let publicKey = Data(repeating: 0x02, count: 33)
        let signature = Data(repeating: 0x00, count: 32)

        XCTAssertThrowsError(try crypto.verifySignature(signature, for: message, publicKey: publicKey)) { error in
            XCTAssertEqual(error as? CryptoError, .signatureVerificationFailed)
        }
    }

    func testVerifySignature_InvalidPublicKey() throws {
        let crypto = DefaultCryptoService()
        let message = Data("test".utf8)
        let invalidPublicKey = Data(repeating: 0x04, count: 33)
        let signature = Data(repeating: 0x00, count: 64)

        XCTAssertThrowsError(try crypto.verifySignature(signature, for: message, publicKey: invalidPublicKey)) { error in
            XCTAssertEqual(error as? CryptoError, .invalidPublicKeyFormat)
        }
    }

    func testVerifySignature_WrongMessage() throws {
        let crypto = DefaultCryptoService()
        let originalMessage = "original".data(using: .utf8)!
        let wrongMessage = "modified".data(using: .utf8)!

        let privateKey = try P256.Signing.PrivateKey(rawRepresentation: Data(repeating: 0xC6, count: 32))
        let publicKey = privateKey.publicKey
        let compressedPublicKey = try crypto.compressPublicKey(publicKey.x963Representation)

        let signature = try privateKey.signature(for: originalMessage)

        let result = try crypto.verifySignature(signature.rawRepresentation, for: wrongMessage, publicKey: compressedPublicKey)
        XCTAssertFalse(result, "Signature for wrong message must fail")
    }

    func testVerifySignature_HighSRejection() throws {
        let crypto = DefaultCryptoService()
        let message = Data("test".utf8)
        let publicKey = Data(repeating: 0x02, count: 33)

        var highSSignature = Data(repeating: 0x12, count: 32)
        let highS = Data(repeating: 0xFF, count: 31) + Data([0xF0])
        highSSignature.append(highS)

        XCTAssertThrowsError(try crypto.verifySignature(highSSignature, for: message, publicKey: publicKey)) { error in
            XCTAssertEqual(error as? CryptoError, .signatureVerificationFailed)
        }
    }
}

// Implementation now in CryptoServiceImpl.swift
