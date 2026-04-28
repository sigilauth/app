import Foundation
import DeviceCheck

/// App Attest integration per Knox §8.2
/// Per Apple DCAppAttestService API (iOS 14+)
protocol AttestationService {
    /// Check if App Attest is supported on this device (iOS 14+)
    var isSupported: Bool { get }

    /// Generate App Attest key and attestation
    /// - Parameters:
    ///   - keyId: Attestation key identifier (from generateKey())
    ///   - challenge: SHA256 hash of challenge from server
    /// - Returns: Attestation object (CBOR-encoded per Apple spec)
    func generateAttestation(for keyId: String, challenge: Data) async throws -> Data

    /// Generate assertion for existing attestation key
    /// - Parameters:
    ///   - keyId: Previously generated attestation key ID
    ///   - challenge: SHA256 hash of challenge
    /// - Returns: Assertion data
    func verifyAssertion(keyId: String, challenge: Data) async throws -> Data
}

enum AttestationError: Error, LocalizedError {
    case notSupported
    case attestationFailed(Error)
    case assertionFailed(Error)

    var errorDescription: String? {
        switch self {
        case .notSupported:
            return "App Attest is not supported on this device or in simulator"
        case .attestationFailed(let error):
            return "Attestation failed: \(error.localizedDescription)"
        case .assertionFailed(let error):
            return "Assertion failed: \(error.localizedDescription)"
        }
    }
}
