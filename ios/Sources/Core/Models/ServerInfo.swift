import Foundation

/// Server information response from /info endpoint
public struct ServerInfo: Codable, Equatable, Sendable {
    public let serverId: String
    public let serverName: String
    public let serverPublicKey: String
    public let serverPictogram: [String]
    public let serverPictogramSpeakable: String
    public let version: String
    public let mode: String
    public let relayUrl: String?
    public let features: Features

    public struct Features: Codable, Equatable, Sendable {
        public let mpa: Bool
        public let secureDecrypt: Bool
        public let mnemonicGeneration: Bool
        public let webhooks: Bool

        enum CodingKeys: String, CodingKey {
            case mpa
            case secureDecrypt = "secure_decrypt"
            case mnemonicGeneration = "mnemonic_generation"
            case webhooks
        }
    }

    enum CodingKeys: String, CodingKey {
        case serverId = "server_id"
        case serverName = "server_name"
        case serverPublicKey = "server_public_key"
        case serverPictogram = "server_pictogram"
        case serverPictogramSpeakable = "server_pictogram_speakable"
        case version
        case mode
        case relayUrl = "relay_url"
        case features
    }
}

/// API error response
public struct APIError: Codable, Equatable, Sendable {
    public let error: ErrorDetail

    public struct ErrorDetail: Codable, Equatable, Sendable {
        public let code: String
        public let message: String
    }
}

/// Network errors
public enum NetworkError: Error, Equatable {
    case invalidURL
    case noInternetConnection
    case timeout
    case serverError(statusCode: Int, message: String?)
    case decodingError(String)
    case signatureVerificationFailed
    case fingerprintMismatch
    case challengeNotFound
    case invalidResponse
    case pairingCodeInvalid
    case pairingCodeNotFound
    case pairingCodeTooManyAttempts
    case pairHandshakeExpired
    case pairHandshakeNotApproved
    case pairNonceConsumed
    case pictogramMismatch
    case initClaimInvalidCode
    case initRequestNotFound
    case initAlreadyClaimed
    case initCodeExpired
    case initRateLimited
    case initInvalidMnemonic
    case initAlreadyResponded
    case initAlreadyApproved
    case initRequestExpired
}
