import Foundation
import CryptoKit

/// Domain separation tags for cryptographic operations
/// Per api/domain-separation.md (Apache-2.0)
/// NORMATIVE: These exact byte sequences MUST be prepended to all signed messages
public enum DomainTag {
    case auth
    case mpa
    case decrypt
    case conv
    case pair

    /// UTF-8 bytes including trailing NUL (0x00)
    public var bytes: [UInt8] {
        switch self {
        case .auth:
            // "SIGIL-AUTH-V1\0" - 15 bytes
            return [0x53, 0x49, 0x47, 0x49, 0x4c, 0x2d, 0x41, 0x55, 0x54, 0x48, 0x2d, 0x56, 0x31, 0x00]
        case .mpa:
            // "SIGIL-MPA-V1\0" - 14 bytes
            return [0x53, 0x49, 0x47, 0x49, 0x4c, 0x2d, 0x4d, 0x50, 0x41, 0x2d, 0x56, 0x31, 0x00]
        case .decrypt:
            // "SIGIL-DECRYPT-V1\0" - 18 bytes
            return [0x53, 0x49, 0x47, 0x49, 0x4c, 0x2d, 0x44, 0x45, 0x43, 0x52, 0x59, 0x50, 0x54, 0x2d, 0x56, 0x31, 0x00]
        case .conv:
            // "SIGIL-CONV-V1\0" - 14 bytes
            return [0x53, 0x49, 0x47, 0x49, 0x4c, 0x2d, 0x43, 0x4f, 0x4e, 0x56, 0x2d, 0x56, 0x31, 0x00]
        case .pair:
            // "SIGIL-PAIR-V1\0\0\0" - 16 bytes (zero-padded for Argon2id salt requirement)
            return [0x53, 0x49, 0x47, 0x49, 0x4c, 0x2d, 0x50, 0x41, 0x49, 0x52, 0x2d, 0x56, 0x31, 0x00, 0x00, 0x00]
        }
    }

    /// UTF-8 string representation (for debugging)
    public var string: String {
        switch self {
        case .auth: return "SIGIL-AUTH-V1\0"
        case .mpa: return "SIGIL-MPA-V1\0"
        case .decrypt: return "SIGIL-DECRYPT-V1\0"
        case .conv: return "SIGIL-CONV-V1\0"
        case .pair: return "SIGIL-PAIR-V1\0\0\0"
        }
    }

    /// Hex representation (for test vectors)
    public var hex: String {
        return bytes.map { String(format: "%02x", $0) }.joined()
    }
}

/// Domain-separated signing operation
/// Implements: tagged_input = domain_tag || message, hash = SHA256(tagged_input), signature = ECDSA-P256-Sign(hash)
public func taggedHash(domain: DomainTag, message: Data) -> Data {
    var tagged = Data(domain.bytes)
    tagged.append(message)
    return Data(SHA256.hash(data: tagged))
}
