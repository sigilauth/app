import Foundation

/// Challenge action context per challenge.json schema
public struct Action: Codable, Equatable, Sendable {
    public let type: String
    public let description: String
    public let params: [String: String]?

    public init(type: String, description: String, params: [String: String]? = nil) {
        self.type = type
        self.description = description
        self.params = params
    }
}

/// Challenge creation request per challenge.json schema
public struct ChallengeRequest: Codable, Equatable, Sendable {
    public let fingerprint: String
    public let devicePublicKey: String
    public let action: Action

    enum CodingKeys: String, CodingKey {
        case fingerprint
        case devicePublicKey = "device_public_key"
        case action
    }

    public init(fingerprint: String, devicePublicKey: String, action: Action) {
        self.fingerprint = fingerprint
        self.devicePublicKey = devicePublicKey
        self.action = action
    }
}

/// Challenge creation response per challenge.json schema
public struct ChallengeCreated: Codable, Equatable, Sendable {
    public let challengeId: String
    public let fingerprint: String
    public let pictogram: [String]
    public let pictogramSpeakable: String
    public let expiresAt: Date

    enum CodingKeys: String, CodingKey {
        case challengeId = "challenge_id"
        case fingerprint
        case pictogram
        case pictogramSpeakable = "pictogram_speakable"
        case expiresAt = "expires_at"
    }
}

/// Challenge push notification payload per challenge.json schema
public struct ChallengeNotification: Codable, Equatable, Sendable {
    public let type: String
    public let challengeId: String
    public let serverId: String
    public let challengeBytes: String
    public let action: Action
    public let serverSignature: String
    public let expiresAt: Date
    public let respondTo: String

    enum CodingKeys: String, CodingKey {
        case type
        case challengeId = "challenge_id"
        case serverId = "server_id"
        case challengeBytes = "challenge_bytes"
        case action
        case serverSignature = "server_signature"
        case expiresAt = "expires_at"
        case respondTo = "respond_to"
    }
}

/// Challenge response submission per challenge.json schema
public struct ChallengeResponse: Codable, Equatable, Sendable {
    public let challengeId: String
    public let devicePublicKey: String
    public let decision: Decision
    public let signature: String
    public let timestamp: Date

    public enum Decision: String, Codable, Sendable {
        case approved
        case rejected
    }

    enum CodingKeys: String, CodingKey {
        case challengeId = "challenge_id"
        case devicePublicKey = "device_public_key"
        case decision
        case signature
        case timestamp
    }

    public init(
        challengeId: String,
        devicePublicKey: String,
        decision: Decision,
        signature: String,
        timestamp: Date
    ) {
        self.challengeId = challengeId
        self.devicePublicKey = devicePublicKey
        self.decision = decision
        self.signature = signature
        self.timestamp = timestamp
    }
}

/// Challenge verification response per challenge.json schema
public struct ChallengeVerified: Codable, Equatable, Sendable {
    public let verified: Bool
    public let fingerprint: String
    public let pictogram: [String]
    public let pictogramSpeakable: String
    public let action: Action?

    enum CodingKeys: String, CodingKey {
        case verified
        case fingerprint
        case pictogram
        case pictogramSpeakable = "pictogram_speakable"
        case action
    }
}
