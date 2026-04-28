import Foundation

public struct ImplementorInitRequest: Codable, Equatable, Sendable {
    public let requestId: String
    public let implementorName: String
    public let implementorId: String
    public let implementorEphemeralPublicKey: String
    public let timestamp: Date
    public let expiresAt: Date

    enum CodingKeys: String, CodingKey {
        case requestId = "request_id"
        case implementorName = "implementor_name"
        case implementorId = "implementor_id"
        case implementorEphemeralPublicKey = "implementor_ephemeral_public_key"
        case timestamp
        case expiresAt = "expires_at"
    }

    public init(
        requestId: String,
        implementorName: String,
        implementorId: String,
        implementorEphemeralPublicKey: String,
        timestamp: Date,
        expiresAt: Date
    ) {
        self.requestId = requestId
        self.implementorName = implementorName
        self.implementorId = implementorId
        self.implementorEphemeralPublicKey = implementorEphemeralPublicKey
        self.timestamp = timestamp
        self.expiresAt = expiresAt
    }
}

public struct ClaimRequest: Codable, Equatable, Sendable {
    public let requestId: String
    public let claimCode: String
    public let deviceFingerprint: String
    public let deviceSignature: String

    enum CodingKeys: String, CodingKey {
        case requestId = "request_id"
        case claimCode = "claim_code"
        case deviceFingerprint = "device_fingerprint"
        case deviceSignature = "device_signature"
    }

    public init(requestId: String, claimCode: String, deviceFingerprint: String, deviceSignature: String) {
        self.requestId = requestId
        self.claimCode = claimCode
        self.deviceFingerprint = deviceFingerprint
        self.deviceSignature = deviceSignature
    }
}

public struct ClaimResponse: Codable, Equatable, Sendable {
    public let status: String
    public let pinnedToFingerprint: String

    enum CodingKeys: String, CodingKey {
        case status
        case pinnedToFingerprint = "pinned_to_fingerprint"
    }
}

public struct InitRespondRequest: Codable, Equatable, Sendable {
    public let requestId: String
    public let deviceFingerprint: String
    public let action: String
    public let encryptedMnemonic: String?
    public let rejectionReason: String?
    public let deviceSignature: String

    enum CodingKeys: String, CodingKey {
        case requestId = "request_id"
        case deviceFingerprint = "device_fingerprint"
        case action
        case encryptedMnemonic = "encrypted_mnemonic"
        case rejectionReason = "rejection_reason"
        case deviceSignature = "device_signature"
    }

    public init(
        requestId: String,
        deviceFingerprint: String,
        action: String,
        encryptedMnemonic: String? = nil,
        rejectionReason: String? = nil,
        deviceSignature: String
    ) {
        self.requestId = requestId
        self.deviceFingerprint = deviceFingerprint
        self.action = action
        self.encryptedMnemonic = encryptedMnemonic
        self.rejectionReason = rejectionReason
        self.deviceSignature = deviceSignature
    }
}

public struct InitRespondResponse: Codable, Equatable, Sendable {
    public let status: String
}
