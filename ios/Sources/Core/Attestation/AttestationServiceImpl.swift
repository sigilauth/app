import Foundation
import DeviceCheck

/// Production implementation of AttestationService
/// Uses Apple App Attest API (iOS 14+)
/// Per Knox Top 5 #3: Device integrity verification
@available(iOS 14.0, *)
final class DefaultAttestationService: AttestationService {

    private let service: DCAppAttestService

    init(service: DCAppAttestService = .shared) {
        self.service = service
    }

    var isSupported: Bool {
        #if targetEnvironment(simulator)
        return false  // App Attest not available in simulator
        #else
        return service.isSupported
        #endif
    }

    /// Generate attestation key and return attestation object
    /// Per protocol-spec §2.4: Server verifies this attestation
    func generateAttestation(for keyId: String, challenge: Data) async throws -> Data {
        guard isSupported else {
            throw AttestationError.notSupported
        }

        // Generate key if it doesn't exist
        do {
            let keyId = try await service.generateKey()

            // Attest the key with challenge
            let attestation = try await service.attestKey(keyId, clientDataHash: challenge)

            return attestation
        } catch {
            throw AttestationError.attestationFailed(error)
        }
    }

    /// Verify a previously generated attestation key
    func verifyAssertion(keyId: String, challenge: Data) async throws -> Data {
        guard isSupported else {
            throw AttestationError.notSupported
        }

        do {
            let assertion = try await service.generateAssertion(keyId, clientDataHash: challenge)
            return assertion
        } catch {
            throw AttestationError.assertionFailed(error)
        }
    }
}
