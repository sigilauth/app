import Foundation
import Crypto
import CryptoKit

public enum ECIESError: Error {
    case keyGenerationFailed
    case encryptionFailed
    case decryptionFailed
    case invalidCiphertext
    case invalidPublicKey
}

public protocol ECIESService {
    func encrypt(plaintext: Data, recipientPublicKey: Data) throws -> Data
    func decrypt(ciphertext: Data, recipientPrivateKey: SecKey, recipientPublicKey: Data) throws -> Data
}

public struct DefaultECIESService: ECIESService {

    public func encrypt(plaintext: Data, recipientPublicKey: Data) throws -> Data {
        guard recipientPublicKey.count == 33 else {
            throw ECIESError.invalidPublicKey
        }

        let recipientKey = try P256.KeyAgreement.PublicKey(compressedRepresentation: recipientPublicKey)

        let ephemeralPrivateKey = P256.KeyAgreement.PrivateKey()
        let ephemeralPublicKey = ephemeralPrivateKey.publicKey

        let sharedSecret = try ephemeralPrivateKey.sharedSecretFromKeyAgreement(with: recipientKey)

        let fingerprint = Data(SHA256.hash(data: recipientPublicKey))

        let derivedKey = try deriveAESKey(
            sharedSecret: sharedSecret.withUnsafeBytes { Data($0) },
            salt: fingerprint,
            info: "SIGIL-CONV-V1-AES256"
        )

        let nonce = try AES.GCM.Nonce()
        let ephemeralPubCompressed = ephemeralPublicKey.compressedRepresentation

        let sealedBox = try AES.GCM.seal(
            plaintext,
            using: SymmetricKey(data: derivedKey),
            nonce: nonce,
            authenticating: ephemeralPubCompressed
        )

        var envelope = Data()
        envelope.append(ephemeralPubCompressed)
        envelope.append(nonce.withUnsafeBytes { Data($0) })
        envelope.append(sealedBox.ciphertext)
        envelope.append(sealedBox.tag)

        return envelope
    }

    public func decrypt(ciphertext: Data, recipientPrivateKey: SecKey, recipientPublicKey: Data) throws -> Data {
        guard ciphertext.count >= 33 + 12 + 16 else {
            throw ECIESError.invalidCiphertext
        }

        let ephemeralPubCompressed = ciphertext[0..<33]
        let nonceBytes = ciphertext[33..<45]
        let ciphertextBody = ciphertext[45..<(ciphertext.count - 16)]
        let tag = ciphertext[(ciphertext.count - 16)...]

        let ephemeralSecKey = try importPublicKeyAsSecKey(compressedPublicKey: ephemeralPubCompressed)

        var error: Unmanaged<CFError>?
        guard let sharedSecretData = SecKeyCopyKeyExchangeResult(
            recipientPrivateKey,
            .ecdhKeyExchangeStandard,
            ephemeralSecKey,
            [:] as CFDictionary,
            &error
        ) as Data? else {
            if let err = error?.takeRetainedValue() {
                throw err as Error
            }
            throw ECIESError.decryptionFailed
        }

        let fingerprint = Data(SHA256.hash(data: recipientPublicKey))

        let derivedKey = try deriveAESKey(
            sharedSecret: sharedSecretData,
            salt: fingerprint,
            info: "SIGIL-CONV-V1-AES256"
        )

        let nonce = try AES.GCM.Nonce(data: nonceBytes)

        let sealedBox = try AES.GCM.SealedBox(
            nonce: nonce,
            ciphertext: ciphertextBody,
            tag: tag
        )

        let plaintext = try AES.GCM.open(
            sealedBox,
            using: SymmetricKey(data: derivedKey),
            authenticating: ephemeralPubCompressed
        )

        return plaintext
    }

    private func importPublicKeyAsSecKey(compressedPublicKey: Data) throws -> SecKey {
        let keyAttributes: [String: Any] = [
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeyClass as String: kSecAttrKeyClassPublic,
            kSecAttrKeySizeInBits as String: 256
        ]

        var error: Unmanaged<CFError>?
        guard let secKey = SecKeyCreateWithData(
            compressedPublicKey as CFData,
            keyAttributes as CFDictionary,
            &error
        ) else {
            if let err = error?.takeRetainedValue() {
                throw err as Error
            }
            throw ECIESError.invalidPublicKey
        }

        return secKey
    }

    private func deriveAESKey(sharedSecret: Data, salt: Data, info: String) throws -> Data {
        let infoData = info.data(using: .utf8)!

        let derivedKey = HKDF<SHA256>.deriveKey(
            inputKeyMaterial: SymmetricKey(data: sharedSecret),
            salt: salt,
            info: infoData,
            outputByteCount: 32
        )

        return derivedKey.withUnsafeBytes { Data($0) }
    }
}
