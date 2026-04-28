import Foundation
import CryptoKit

/// Cryptographic operations for Sigil Auth
/// Implements: P-256 keypair generation, ECDSA signing, ECDH key agreement, HKDF derivation
protocol CryptoService {
    /// Compress uncompressed P-256 public key (65 bytes) to compressed format (33 bytes)
    func compressPublicKey(_ uncompressed: Data) throws -> Data

    /// Derive 5-emoji pictogram from device fingerprint (SHA256 of public key)
    func derivePictogram(from fingerprint: Data) -> Pictogram

    /// Compute fingerprint (SHA256) of compressed public key
    func computeFingerprint(of publicKey: Data) -> Data

    /// Verify ECDSA signature against public key with domain separation
    /// Per api/domain-separation.md: verifier MUST use same domain tag as signer
    func verifySignature(_ signature: Data, for payload: Data, domain: DomainTag, publicKey: Data) throws -> Bool
}

enum CryptoError: Error, Equatable {
    case invalidPublicKeyFormat
    case signatureVerificationFailed
    case keyCompressionFailed
    case highSSignature
}
