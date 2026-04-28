import XCTest
import CryptoKit
@testable import SigilAuthCore

/// Domain separation test vectors per api/domain-separation.md
/// Tests verify that tagged hashing + signing produces canonical signatures
final class DomainSeparationTests: XCTestCase {

    // MARK: - Test Vectors from api/test-vectors/domain-separation/auth-v1.json

    func testAuthDomainTag() throws {
        let domain = DomainTag.auth
        XCTAssertEqual(domain.hex, "5349474c2d415554482d56312d00")
        XCTAssertEqual(domain.bytes.count, 15)
    }

    func testMPADomainTag() throws {
        let domain = DomainTag.mpa
        XCTAssertEqual(domain.hex, "5349474c2d4d50412d56312d00")
        XCTAssertEqual(domain.bytes.count, 14)
    }

    func testDecryptDomainTag() throws {
        let domain = DomainTag.decrypt
        XCTAssertEqual(domain.hex, "5349474c2d444543525950542d56312d00")
        XCTAssertEqual(domain.bytes.count, 18)
    }

    // MARK: - Auth Challenge Signature Verification

    func testAuthChallengeSignatureVerification() throws {
        // Test vector from auth-v1.json
        let privateKeyHex = "c9afa9d845ba75166b5c215767b1d6934e50c3db36e89b127b8a622b120f6721"
        let publicKeyCompressedHex = "0360fed4ba255a9d31c961eb74c6356d68c049b8923b61fa6ce669622e60f29fb6"
        let messageHex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        let expectedSignatureHex = "a68b65130bda2a2ce3cd5242f3a0a4976d4496c2ffa6f6a6917f0f85e8226a536ba5f356a7ef8ca87e9b08ff8fbb028794ea7d6c9000d5468daba65495d297d1"

        guard let publicKey = Data(hexString: publicKeyCompressedHex),
              let message = Data(hexString: messageHex),
              let expectedSignature = Data(hexString: expectedSignatureHex) else {
            XCTFail("Failed to parse hex strings")
            return
        }

        let crypto = DefaultCryptoService()
        let isValid = try crypto.verifySignature(expectedSignature, for: message, domain: .auth, publicKey: publicKey)

        XCTAssertTrue(isValid, "Expected signature should verify with AUTH domain tag")
    }

    // MARK: - MPA Approval Signature Verification

    func testMPAApprovalSignatureVerification() throws {
        // Test vector from mpa-v1.json
        let publicKeyCompressedHex = "0360fed4ba255a9d31c961eb74c6356d68c049b8923b61fa6ce669622e60f29fb6"
        let messageJSON = "{\"type\":\"delete_account\",\"description\":\"Permanently delete user account\",\"params\":{\"user_id\":\"123\"}}"
        let expectedSignatureHex = "0fcbec9dbcfbb571dc11f16577c8ae487d9feade9c357a70354f45130e2a3a4650b46362efa7130e4a84ffaf3729bf0fb5db38def3b75096c2341ad52b085574"

        guard let publicKey = Data(hexString: publicKeyCompressedHex),
              let message = messageJSON.data(using: .utf8),
              let expectedSignature = Data(hexString: expectedSignatureHex) else {
            XCTFail("Failed to parse test data")
            return
        }

        let crypto = DefaultCryptoService()
        let isValid = try crypto.verifySignature(expectedSignature, for: message, domain: .mpa, publicKey: publicKey)

        XCTAssertTrue(isValid, "Expected signature should verify with MPA domain tag")
    }

    // MARK: - Cross-Domain Rejection Tests

    func testAuthSignatureRejectsWithMPADomain() throws {
        // Signature produced with AUTH tag should fail verification with MPA tag
        let publicKeyCompressedHex = "0360fed4ba255a9d31c961eb74c6356d68c049b8923b61fa6ce669622e60f29fb6"
        let messageHex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        let authSignatureHex = "a68b65130bda2a2ce3cd5242f3a0a4976d4496c2ffa6f6a6917f0f85e8226a536ba5f356a7ef8ca87e9b08ff8fbb028794ea7d6c9000d5468daba65495d297d1"

        guard let publicKey = Data(hexString: publicKeyCompressedHex),
              let message = Data(hexString: messageHex),
              let authSignature = Data(hexString: authSignatureHex) else {
            XCTFail("Failed to parse hex strings")
            return
        }

        let crypto = DefaultCryptoService()
        let isValid = try crypto.verifySignature(authSignature, for: message, domain: .mpa, publicKey: publicKey)

        XCTAssertFalse(isValid, "AUTH signature should NOT verify with MPA domain tag (cross-protocol attack prevented)")
    }

    func testMPASignatureRejectsWithAuthDomain() throws {
        // Signature produced with MPA tag should fail verification with AUTH tag
        let publicKeyCompressedHex = "0360fed4ba255a9d31c961eb74c6356d68c049b8923b61fa6ce669622e60f29fb6"
        let messageJSON = "{\"type\":\"delete_account\",\"description\":\"Permanently delete user account\",\"params\":{\"user_id\":\"123\"}}"
        let mpaSignatureHex = "0fcbec9dbcfbb571dc11f16577c8ae487d9feade9c357a70354f45130e2a3a4650b46362efa7130e4a84ffaf3729bf0fb5db38def3b75096c2341ad52b085574"

        guard let publicKey = Data(hexString: publicKeyCompressedHex),
              let message = messageJSON.data(using: .utf8),
              let mpaSignature = Data(hexString: mpaSignatureHex) else {
            XCTFail("Failed to parse test data")
            return
        }

        let crypto = DefaultCryptoService()
        let isValid = try crypto.verifySignature(mpaSignature, for: message, domain: .auth, publicKey: publicKey)

        XCTAssertFalse(isValid, "MPA signature should NOT verify with AUTH domain tag (cross-protocol attack prevented)")
    }

    // MARK: - Signature Generation Tests (Byte-for-Byte Canonical Match)

    func testAuthSignatureGenerationMatchesCanonical() throws {
        // Test vector from auth-v1.json (with action_context binding per spec be85208)
        let privateKeyHex = "c9afa9d845ba75166b5c215767b1d6934e50c3db36e89b127b8a622b120f6721"
        let challengeHex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        let actionContextJSON = "{\"description\":\"Sign in to Engine Management\",\"type\":\"engine_login\",\"user_id\":\"alice\"}"
        let actionHashHex = "ae4c8d04ee09905ce0fbf5dd11c8733e92508ab00301b19d8e9da8f28b49b620"
        let expectedSignatureHex = "1500ac510b6b2cd7ed7400542d10ecf500a36e6f402b2f122afc15b8988e27c03a10c9d47aebcda59893507d3ad5bb75dd0fe5b94cecbbbb27c2522847cb1f02"

        guard let privateKeyData = Data(hexString: privateKeyHex),
              let challenge = Data(hexString: challengeHex),
              let actionHash = Data(hexString: actionHashHex),
              let expectedSignature = Data(hexString: expectedSignatureHex) else {
            XCTFail("Failed to parse hex strings")
            return
        }

        // Verify action_context hash matches expected
        let computedActionHash = try JSONCanonicalizer.hash(jsonString: actionContextJSON)
        XCTAssertEqual(computedActionHash, actionHash, "Computed action_context hash must match test vector")

        // Build auth payload: challenge_bytes || action_hash
        var authPayload = challenge
        authPayload.append(actionHash)

        // Load private key (software key for testing)
        let privateKey = try P256.Signing.PrivateKey(rawRepresentation: privateKeyData)

        // Compute tagged hash
        let digest = taggedHash(domain: .auth, message: authPayload)

        // Sign digest
        let signature = try privateKey.signature(for: digest)

        // Compare raw representation byte-for-byte
        XCTAssertEqual(signature.rawRepresentation, expectedSignature,
                       "Generated AUTH signature must match canonical test vector byte-for-byte (with action_context)")
    }

    func testAuthSignatureEmptyActionContext() throws {
        // Test vector from auth-v1.json (empty action_context)
        let privateKeyHex = "c9afa9d845ba75166b5c215767b1d6934e50c3db36e89b127b8a622b120f6721"
        let challengeHex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        let emptyActionHashHex = "44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a"
        let expectedSignatureHex = "c8c48552aea9b31d6e1b14228598fc5db9c2ef23e31573c8957d96fc966347e4491183144065153ca605f0572fb655b54da99a92f7b7468ab22276e4ad36a6ea"

        guard let privateKeyData = Data(hexString: privateKeyHex),
              let challenge = Data(hexString: challengeHex),
              let emptyActionHash = Data(hexString: emptyActionHashHex),
              let expectedSignature = Data(hexString: expectedSignatureHex) else {
            XCTFail("Failed to parse hex strings")
            return
        }

        // Verify empty action_context hash (canonical "{}")
        let computedEmptyHash = try JSONCanonicalizer.hash(jsonString: "{}")
        XCTAssertEqual(computedEmptyHash, emptyActionHash, "Empty action_context hash must be fixed canonical value")

        // Build auth payload: challenge_bytes || empty_action_hash
        var authPayload = challenge
        authPayload.append(emptyActionHash)

        // Load private key (software key for testing)
        let privateKey = try P256.Signing.PrivateKey(rawRepresentation: privateKeyData)

        // Compute tagged hash
        let digest = taggedHash(domain: .auth, message: authPayload)

        // Sign digest
        let signature = try privateKey.signature(for: digest)

        // Compare raw representation byte-for-byte
        XCTAssertEqual(signature.rawRepresentation, expectedSignature,
                       "Generated AUTH signature must match canonical test vector byte-for-byte (empty action_context)")
    }

    func testMPASignatureGenerationMatchesCanonical() throws {
        // Load test private key and generate signature - must match canonical bytes exactly
        let privateKeyHex = "c9afa9d845ba75166b5c215767b1d6934e50c3db36e89b127b8a622b120f6721"
        let messageJSON = "{\"type\":\"delete_account\",\"description\":\"Permanently delete user account\",\"params\":{\"user_id\":\"123\"}}"
        let expectedSignatureHex = "0fcbec9dbcfbb571dc11f16577c8ae487d9feade9c357a70354f45130e2a3a4650b46362efa7130e4a84ffaf3729bf0fb5db38def3b75096c2341ad52b085574"

        guard let privateKeyData = Data(hexString: privateKeyHex),
              let message = messageJSON.data(using: .utf8),
              let expectedSignature = Data(hexString: expectedSignatureHex) else {
            XCTFail("Failed to parse test data")
            return
        }

        // Load private key (software key for testing)
        let privateKey = try P256.Signing.PrivateKey(rawRepresentation: privateKeyData)

        // Compute tagged hash
        let digest = taggedHash(domain: .mpa, message: message)

        // Sign digest
        let signature = try privateKey.signature(for: digest)

        // Compare raw representation byte-for-byte
        XCTAssertEqual(signature.rawRepresentation, expectedSignature,
                       "Generated MPA signature must match canonical test vector byte-for-byte")
    }

    // MARK: - Tagged Hash Tests

    func testAuthTaggedHashProducesCorrectDigest() throws {
        // Verify that taggedHash produces the expected SHA256(domain || message)
        let domain = DomainTag.auth
        let messageHex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        let expectedTaggedInputHex = "534947494c2d415554482d5631000123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

        guard let message = Data(hexString: messageHex),
              let expectedTaggedInput = Data(hexString: expectedTaggedInputHex) else {
            XCTFail("Failed to parse hex strings")
            return
        }

        // Verify tagged input construction
        var taggedInput = Data(domain.bytes)
        taggedInput.append(message)
        XCTAssertEqual(taggedInput, expectedTaggedInput, "Tagged input should match expected concatenation")

        // Verify hash
        let digest = taggedHash(domain: domain, message: message)
        let expectedDigest = Data(SHA256.hash(data: expectedTaggedInput))
        XCTAssertEqual(digest, expectedDigest, "Tagged hash should match SHA256(domain || message)")
    }

    func testMPATaggedHashProducesCorrectDigest() throws {
        let domain = DomainTag.mpa
        let messageJSON = "{\"type\":\"delete_account\",\"description\":\"Permanently delete user account\",\"params\":{\"user_id\":\"123\"}}"
        let expectedTaggedInputHex = "534947494c2d4d50412d5631007b2274797065223a2264656c6574655f6163636f756e74222c226465736372697074696f6e223a225065726d616e656e746c792064656c6574652075736572206163636f756e74222c22706172616d73223a7b22757365725f6964223a22313233227d7d"

        guard let message = messageJSON.data(using: .utf8),
              let expectedTaggedInput = Data(hexString: expectedTaggedInputHex) else {
            XCTFail("Failed to parse test data")
            return
        }

        var taggedInput = Data(domain.bytes)
        taggedInput.append(message)
        XCTAssertEqual(taggedInput, expectedTaggedInput, "Tagged input should match expected concatenation")

        let digest = taggedHash(domain: domain, message: message)
        let expectedDigest = Data(SHA256.hash(data: expectedTaggedInput))
        XCTAssertEqual(digest, expectedDigest, "Tagged hash should match SHA256(domain || message)")
    }
}
