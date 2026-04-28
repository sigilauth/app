import Foundation
import Crypto

public struct EnvelopePayload: Codable {
    public let action: String
    public let body: [String: Any]?
    public let timestamp: Int64
    public let nonce: String
    public let audience: String?

    enum CodingKeys: String, CodingKey {
        case action, body, timestamp, nonce, audience
    }

    public init(action: String, body: [String: Any]? = nil, timestamp: Int64, nonce: String, audience: String?) {
        self.action = action
        self.body = body
        self.timestamp = timestamp
        self.nonce = nonce
        self.audience = audience
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(action, forKey: .action)
        if let body = body {
            let bodyData = try JSONSerialization.data(withJSONObject: body)
            let bodyDict = try JSONSerialization.jsonObject(with: bodyData) as? [String: Any]
            try container.encode(bodyDict, forKey: .body)
        }
        try container.encode(timestamp, forKey: .timestamp)
        try container.encode(nonce, forKey: .nonce)
        try container.encodeIfPresent(audience, forKey: .audience)
    }
}

struct InnerEnvelope: Codable {
    let client_public_key: String
    let payload: String
    let signature: String
}

public struct OuterEnvelope: Codable {
    public let envelope: String
}

public enum EnvelopeError: Error {
    case canonicalizationFailed
    case signingFailed
    case encryptionFailed
    case decryptionFailed
    case signatureVerificationFailed
    case timestampExpired
    case nonceReused
    case audienceMismatch
}

public protocol EnvelopeService {
    func createRequest(
        payload: EnvelopePayload,
        clientPrivateKey: SecKey,
        clientPublicKey: Data,
        serverPublicKey: Data
    ) throws -> Data

    func verifyResponse(
        envelopeData: Data,
        serverPublicKey: Data,
        clientPrivateKey: SecKey,
        clientPublicKey: Data
    ) throws -> [String: Any]
}

public struct DefaultEnvelopeService: EnvelopeService {

    private let ecies: ECIESService
    private let crypto: CryptoService

    public init(
        ecies: ECIESService = DefaultECIESService(),
        crypto: CryptoService = DefaultCryptoService()
    ) {
        self.ecies = ecies
        self.crypto = crypto
    }

    public func createRequest(
        payload: EnvelopePayload,
        clientPrivateKey: SecKey,
        clientPublicKey: Data,
        serverPublicKey: Data
    ) throws -> Data {
        let payloadDict: [String: Any] = [
            "action": payload.action,
            "body": payload.body ?? [:],
            "timestamp": payload.timestamp,
            "nonce": payload.nonce,
            "audience": payload.audience ?? ""
        ]

        let canonicalPayload = try CanonicalJSON.canonicalize(payloadDict)

        let signature = try signPayload(
            canonicalPayload,
            privateKey: clientPrivateKey,
            domain: .conv
        )

        let innerDict: [String: Any] = [
            "client_public_key": clientPublicKey.base64EncodedString(),
            "payload": String(data: canonicalPayload, encoding: .utf8)!,
            "signature": signature.base64EncodedString()
        ]

        let innerJSON = try CanonicalJSON.canonicalize(innerDict)

        let outerCiphertext = try ecies.encrypt(plaintext: innerJSON, recipientPublicKey: serverPublicKey)

        let outerDict: [String: Any] = [
            "envelope": outerCiphertext.base64EncodedString()
        ]

        return try JSONSerialization.data(withJSONObject: outerDict)
    }

    public func verifyResponse(
        envelopeData: Data,
        serverPublicKey: Data,
        clientPrivateKey: SecKey,
        clientPublicKey: Data
    ) throws -> [String: Any] {
        let outerDict = try JSONSerialization.jsonObject(with: envelopeData) as? [String: Any]
        guard let envelopeB64 = outerDict?["envelope"] as? String,
              let outerCiphertext = Data(base64Encoded: envelopeB64) else {
            throw EnvelopeError.decryptionFailed
        }

        let innerJSON = try ecies.decrypt(ciphertext: outerCiphertext, recipientPrivateKey: clientPrivateKey, recipientPublicKey: clientPublicKey)

        let innerDict = try JSONSerialization.jsonObject(with: innerJSON) as? [String: Any]
        guard let serverPubB64 = innerDict?["server_public_key"] as? String,
              let payloadStr = innerDict?["payload"] as? String,
              let signatureB64 = innerDict?["signature"] as? String,
              let serverPubDecoded = Data(base64Encoded: serverPubB64),
              let payloadData = payloadStr.data(using: .utf8),
              let signature = Data(base64Encoded: signatureB64) else {
            throw EnvelopeError.decryptionFailed
        }

        guard serverPubDecoded == serverPublicKey else {
            throw EnvelopeError.signatureVerificationFailed
        }

        let isValid = try crypto.verifySignature(
            signature,
            for: payloadData,
            domain: .conv,
            publicKey: serverPublicKey
        )

        guard isValid else {
            throw EnvelopeError.signatureVerificationFailed
        }

        let payload = try JSONSerialization.jsonObject(with: payloadData) as? [String: Any]
        guard let timestamp = payload?["timestamp"] as? Int64 else {
            throw EnvelopeError.timestampExpired
        }

        let now = Int64(Date().timeIntervalSince1970)
        guard abs(now - timestamp) <= 300 else {
            throw EnvelopeError.timestampExpired
        }

        return payload ?? [:]
    }

    private func signPayload(_ payload: Data, privateKey: SecKey, domain: DomainTag) throws -> Data {
        var tagged = domain.bytes
        tagged.append(payload)

        let digest = Data(SHA256.hash(data: tagged))

        var error: Unmanaged<CFError>?
        guard let signature = SecKeyCreateSignature(
            privateKey,
            .ecdsaSignatureMessageX962SHA256,
            digest as CFData,
            &error
        ) as Data? else {
            throw EnvelopeError.signingFailed
        }

        return signature
    }
}
