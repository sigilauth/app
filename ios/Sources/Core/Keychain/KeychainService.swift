import Foundation
import Security
import LocalAuthentication

/// Secure Enclave key management per Knox threat model
/// REQ: Private key non-exportable, biometric-gated, invalidated on enrollment change
protocol KeychainService {
    /// Generate P-256 keypair in Secure Enclave with biometric access control
    /// - Parameter label: Unique key identifier
    /// - Returns: Compressed public key (33 bytes)
    /// - Throws: KeychainError if generation fails or Secure Enclave unavailable
    func generateDeviceKeypair(label: String) async throws -> Data

    /// Sign payload with device private key (biometric gate triggered)
    /// Domain separation per api/domain-separation.md
    /// - Parameters:
    ///   - payload: Data to sign (will be tagged with domain)
    ///   - domain: Domain tag (auth, mpa, decrypt)
    ///   - keyLabel: Key identifier
    /// - Returns: ECDSA signature (64 bytes, fixed format)
    /// - Throws: KeychainError.biometricCancelled if user denies
    func sign(_ payload: Data, domain: DomainTag, with keyLabel: String) async throws -> Data

    /// Delete device keypair from Secure Enclave
    func deleteKey(label: String) throws

    /// Check if Secure Enclave is available on this device
    var isSecureEnclaveAvailable: Bool { get }
}

enum KeychainError: Error, LocalizedError {
    case secureEnclaveUnavailable
    case accessControlCreationFailed(Error?)
    case keyGenerationFailed(Error?)
    case publicKeyExtractionFailed
    case publicKeyExportFailed(Error?)
    case keyNotFound(label: String)
    case biometricAuthenticationFailed
    case signingFailed(Error?)
    case deletionFailed(status: OSStatus)
    case invalidSignatureFormat

    var errorDescription: String? {
        switch self {
        case .secureEnclaveUnavailable:
            return "Secure Enclave not available on this device"
        case .accessControlCreationFailed(let error):
            return "Failed to create access control: \(error?.localizedDescription ?? "unknown")"
        case .keyGenerationFailed(let error):
            return "Failed to generate key: \(error?.localizedDescription ?? "unknown")"
        case .publicKeyExtractionFailed:
            return "Failed to extract public key from private key"
        case .publicKeyExportFailed(let error):
            return "Failed to export public key: \(error?.localizedDescription ?? "unknown")"
        case .keyNotFound(let label):
            return "Key not found: \(label)"
        case .biometricAuthenticationFailed:
            return "Biometric authentication failed or was cancelled"
        case .signingFailed(let error):
            return "Failed to sign data: \(error?.localizedDescription ?? "unknown")"
        case .deletionFailed(let status):
            return "Failed to delete key: OSStatus \(status)"
        case .invalidSignatureFormat:
            return "Invalid DER signature format"
        }
    }
}
