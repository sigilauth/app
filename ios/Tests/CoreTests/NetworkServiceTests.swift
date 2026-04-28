import XCTest
@testable import SigilAuthCore

/// TDD: Tests for network operations
/// Note: Implementation written before tests (not ideal TDD flow)
/// Future features should follow: tests first, then implementation
final class NetworkServiceTests: XCTestCase {

    var service: DefaultNetworkService!
    var mockSession: URLSession!

    override func setUp() {
        super.setUp()
        // TODO: Create mock URLSession for testing
        // For now, skip tests that require network mocking
    }

    override func tearDown() {
        service = nil
        mockSession = nil
        super.tearDown()
    }

    func testFetchServerInfo_ValidResponse() async throws {
        throw XCTSkip("Requires URLSession mocking - implement with URLProtocol stub")
    }

    func testFetchServerInfo_NetworkError() async throws {
        throw XCTSkip("Requires URLSession mocking")
    }

    func testRespondToChallenge_Approved() async throws {
        throw XCTSkip("Requires URLSession mocking")
    }

    func testRespondToChallenge_InvalidSignature() async throws {
        throw XCTSkip("Requires URLSession mocking")
    }

    func testRespondToChallenge_FingerprintMismatch() async throws {
        throw XCTSkip("Requires URLSession mocking")
    }

    func testRespondToChallenge_ChallengeNotFound() async throws {
        throw XCTSkip("Requires URLSession mocking")
    }

    func testModelDecoding_ServerInfo() throws {
        let json = """
        {
          "server_id": "test-001",
          "server_name": "Test Server",
          "server_public_key": "Ag8xYzI3ZWRkNDUzYmNlYzVmMTJjNmI5MzA4OGY0",
          "server_pictogram": ["apple", "banana", "car", "dog", "key"],
          "server_pictogram_speakable": "apple banana car dog key",
          "version": "1.0.0",
          "mode": "operational",
          "relay_url": "https://relay.sigilauth.com",
          "features": {
            "mpa": true,
            "secure_decrypt": true,
            "mnemonic_generation": true,
            "webhooks": true
          }
        }
        """.data(using: .utf8)!

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601

        let serverInfo = try decoder.decode(ServerInfo.self, from: json)

        XCTAssertEqual(serverInfo.serverId, "test-001")
        XCTAssertEqual(serverInfo.serverName, "Test Server")
        XCTAssertEqual(serverInfo.serverPictogram.count, 5)
        XCTAssertEqual(serverInfo.serverPictogramSpeakable, "apple banana car dog key")
        XCTAssertEqual(serverInfo.version, "1.0.0")
        XCTAssertTrue(serverInfo.features.mpa)
    }

    func testModelDecoding_ChallengeVerified() throws {
        let json = """
        {
          "verified": true,
          "fingerprint": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2",
          "pictogram": ["apple", "banana", "car", "dog", "key"],
          "pictogram_speakable": "apple banana car dog key",
          "action": {
            "type": "step_up",
            "description": "Add WebAuthn key",
            "params": {
              "key_name": "YubiKey 5C"
            }
          }
        }
        """.data(using: .utf8)!

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601

        let verified = try decoder.decode(ChallengeVerified.self, from: json)

        XCTAssertTrue(verified.verified)
        XCTAssertEqual(verified.fingerprint.count, 64)
        XCTAssertEqual(verified.pictogram.count, 5)
        XCTAssertEqual(verified.action?.type, "step_up")
        XCTAssertEqual(verified.action?.params?["key_name"], "YubiKey 5C")
    }

    func testModelEncoding_ChallengeResponse() throws {
        let response = ChallengeResponse(
            challengeId: "550e8400-e29b-41d4-a716-446655440000",
            devicePublicKey: "Ag8xYzI3ZWRkNDUzYmNlYzVmMTJjNmI5MzA4OGY0",
            decision: .approved,
            signature: "MEUCIQDfz8K7rN==",
            timestamp: Date(timeIntervalSince1970: 1714737600)
        )

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601

        let data = try encoder.encode(response)
        let json = try JSONSerialization.jsonObject(with: data) as! [String: Any]

        XCTAssertEqual(json["challenge_id"] as? String, "550e8400-e29b-41d4-a716-446655440000")
        XCTAssertEqual(json["device_public_key"] as? String, "Ag8xYzI3ZWRkNDUzYmNlYzVmMTJjNmI5MzA4OGY0")
        XCTAssertEqual(json["decision"] as? String, "approved")
        XCTAssertEqual(json["signature"] as? String, "MEUCIQDfz8K7rN==")
        XCTAssertNotNil(json["timestamp"])
    }

    func testModelDecoding_APIError() throws {
        let json = """
        {
          "error": {
            "code": "INVALID_SIGNATURE",
            "message": "Signature verification failed"
          }
        }
        """.data(using: .utf8)!

        let decoder = JSONDecoder()
        let apiError = try decoder.decode(APIError.self, from: json)

        XCTAssertEqual(apiError.error.code, "INVALID_SIGNATURE")
        XCTAssertEqual(apiError.error.message, "Signature verification failed")
    }
}
