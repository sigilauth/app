import Foundation
import Security
import CryptoKit
import LocalAuthentication

/// Production implementation of KeychainService
/// Uses Secure Enclave for key generation per Knox Top 5 requirements
final class DefaultKeychainService: KeychainService {

    var isSecureEnclaveAvailable: Bool {
        #if targetEnvironment(simulator)
        return false
        #else
        // Secure Enclave available on A7+ devices (iPhone 5s and later)
        return true
        #endif
    }

    /// Generate ECDSA P-256 keypair in Secure Enclave
    /// Per Knox Top 5 #1: Private key is non-exportable
    /// Per Aria §4.3: Supports both biometric and device passcode
    func generateDeviceKeypair(label: String) async throws -> Data {
        // Access control: biometric OR device passcode (WCAG 3.3.8 compliance)
        var accessControlError: Unmanaged<CFError>?
        guard let accessControl = SecAccessControlCreateWithFlags(
            kCFAllocatorDefault,
            kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            [.privateKeyUsage, .biometryAny, .or, .devicePasscode],
            &accessControlError
        ) else {
            if let error = accessControlError?.takeRetainedValue() {
                throw KeychainError.accessControlCreationFailed(error as Error)
            }
            throw KeychainError.accessControlCreationFailed(nil)
        }

        // Key attributes for Secure Enclave
        let attributes: [String: Any] = [
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeySizeInBits as String: 256,
            kSecAttrTokenID as String: kSecAttrTokenIDSecureEnclave,
            kSecPrivateKeyAttrs as String: [
                kSecAttrIsPermanent as String: true,
                kSecAttrApplicationLabel as String: label.data(using: .utf8)!,
                kSecAttrAccessControl as String: accessControl,
                // Knox Top 5 #1: Prevent key extraction
                kSecAttrIsExtractable as String: false
            ] as [String: Any]
        ]

        var error: Unmanaged<CFError>?
        guard let privateKey = SecKeyCreateRandomKey(attributes as CFDictionary, &error) else {
            if let err = error?.takeRetainedValue() {
                throw KeychainError.keyGenerationFailed(err as Error)
            }
            throw KeychainError.keyGenerationFailed(nil)
        }

        // Extract public key
        guard let publicKey = SecKeyCopyPublicKey(privateKey) else {
            throw KeychainError.publicKeyExtractionFailed
        }

        // Export public key as uncompressed (65 bytes)
        var exportError: Unmanaged<CFError>?
        guard let publicKeyData = SecKeyCopyExternalRepresentation(publicKey, &exportError) as Data? else {
            if let err = exportError?.takeRetainedValue() {
                throw KeychainError.publicKeyExportFailed(err as Error)
            }
            throw KeychainError.publicKeyExportFailed(nil)
        }

        // Compress to 33 bytes
        let crypto = DefaultCryptoService()
        return try crypto.compressPublicKey(publicKeyData)
    }

    /// Sign payload with domain separation and biometric gate
    /// Per api/domain-separation.md: tagged_input = domain || payload, hash = SHA256(tagged_input)
    /// Per Knox Top 5 #2: Biometric required on every signing operation
    func sign(_ payload: Data, domain: DomainTag, with keyLabel: String) async throws -> Data {
        // Domain-separated hash per api/domain-separation.md
        let digest = taggedHash(domain: domain, message: payload)

        // Retrieve private key from keychain
        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationLabel as String: keyLabel.data(using: .utf8)!,
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecReturnRef as String: true,
            kSecUseOperationPrompt as String: "Sign authentication challenge"
        ]

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)

        guard status == errSecSuccess else {
            throw KeychainError.keyNotFound(label: keyLabel)
        }

        guard let privateKey = item as! SecKey? else {
            throw KeychainError.keyNotFound(label: keyLabel)
        }

        // Sign the digest (already hashed with domain tag)
        // Use .ecdsaSignatureDigestX962SHA256 to sign digest directly (not hash again)
        var signError: Unmanaged<CFError>?
        guard let signature = SecKeyCreateSignature(
            privateKey,
            .ecdsaSignatureDigestX962SHA256,
            digest as CFData,
            &signError
        ) as Data? else {
            if let err = signError?.takeRetainedValue() {
                let nsError = err as Error as NSError
                if nsError.code == errSecAuthFailed {
                    throw KeychainError.biometricAuthenticationFailed
                }
                throw KeychainError.signingFailed(err as Error)
            }
            throw KeychainError.signingFailed(nil)
        }

        // Convert DER signature to raw r||s (64 bytes)
        return try convertDERToRaw(signature)
    }

    /// Delete key from keychain
    func deleteKey(label: String) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationLabel as String: label.data(using: .utf8)!
        ]

        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError.deletionFailed(status: status)
        }
    }

    // MARK: - Helpers

    /// Convert DER-encoded ECDSA signature to raw r||s format
    /// DER format: 0x30 [length] 0x02 [r-length] [r] 0x02 [s-length] [s]
    /// Raw format: [r (32 bytes)] [s (32 bytes)]
    private func convertDERToRaw(_ derSignature: Data) throws -> Data {
        // Simple DER parsing for ECDSA signatures
        // Format: SEQUENCE { r INTEGER, s INTEGER }

        var index = 0
        guard derSignature[index] == 0x30 else {
            throw KeychainError.invalidSignatureFormat
        }
        index += 1

        // Skip total length
        let totalLength = Int(derSignature[index])
        index += 1

        // Parse r
        guard derSignature[index] == 0x02 else {
            throw KeychainError.invalidSignatureFormat
        }
        index += 1

        var rLength = Int(derSignature[index])
        index += 1

        // Skip leading zero byte if present (for positive numbers)
        if derSignature[index] == 0x00 && rLength == 33 {
            index += 1
            rLength = 32
        }

        let r = derSignature[index..<(index + rLength)]
        index += rLength

        // Parse s
        guard derSignature[index] == 0x02 else {
            throw KeychainError.invalidSignatureFormat
        }
        index += 1

        var sLength = Int(derSignature[index])
        index += 1

        // Skip leading zero byte if present
        if derSignature[index] == 0x00 && sLength == 33 {
            index += 1
            sLength = 32
        }

        let s = derSignature[index..<(index + sLength)]

        // Pad to 32 bytes if needed
        var rPadded = Data(count: 32 - r.count)
        rPadded.append(r)

        var sPadded = Data(count: 32 - s.count)
        sPadded.append(s)

        return rPadded + sPadded
    }
}
