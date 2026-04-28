import Foundation

/// Pairing payload returned from POST /pairing/redeem
/// Per product-spec §3.7 and openapi.yaml
public struct PairingPayload: Codable, Equatable, Sendable {
    public let serverUrl: String
    public let serverPublicKey: String
    public let serverName: String
    public let serverPictogram: [String]
    public let serverPictogramSpeakable: String
    public let callbackUrl: String
    public let sessionToken: String

    enum CodingKeys: String, CodingKey {
        case serverUrl = "server_url"
        case serverPublicKey = "server_public_key"
        case serverName = "server_name"
        case serverPictogram = "server_pictogram"
        case serverPictogramSpeakable = "server_pictogram_speakable"
        case callbackUrl = "callback_url"
        case sessionToken = "session_token"
    }
}

/// Request to redeem pairing code
public struct PairingCodeRedeemRequest: Codable, Sendable {
    public let pairingCode: String

    public init(pairingCode: String) {
        self.pairingCode = pairingCode
    }

    enum CodingKeys: String, CodingKey {
        case pairingCode = "pairing_code"
    }
}
