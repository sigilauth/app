import Foundation

/// RFC 8785 JSON Canonicalization (minimal implementation)
/// Produces canonical JSON with sorted keys, no whitespace, minimal escaping
///
/// Apache-2.0 License (compatible with AGPL-3.0 app code)
public struct JSONCanonicalizer {

    /// Canonicalize a JSON-encodable value per RFC 8785
    /// - Parameter value: Any Codable value
    /// - Returns: Canonical JSON bytes
    /// - Throws: EncodingError if value cannot be encoded
    public static func canonicalize<T: Encodable>(_ value: T) throws -> Data {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys, .withoutEscapingSlashes]

        // Encode to canonical form (sorted keys, no whitespace)
        return try encoder.encode(value)
    }

    /// Canonicalize JSON from string representation
    /// - Parameter jsonString: JSON string (any formatting)
    /// - Returns: Canonical JSON bytes
    /// - Throws: DecodingError if invalid JSON
    public static func canonicalize(jsonString: String) throws -> Data {
        guard let data = jsonString.data(using: .utf8) else {
            throw JSONError.invalidUTF8
        }

        // Decode to generic structure
        let decoded = try JSONSerialization.jsonObject(with: data)

        // Re-encode canonically
        return try JSONSerialization.data(
            withJSONObject: decoded,
            options: [.sortedKeys, .withoutEscapingSlashes]
        )
    }

    /// Compute SHA256 hash of canonical JSON from Codable value
    /// - Parameter value: Any Codable value
    /// - Returns: SHA256 digest (32 bytes)
    public static func hash<T: Encodable>(_ value: T) throws -> Data {
        let canonical = try canonicalize(value)
        return Data(SHA256.hash(data: canonical))
    }

    /// Compute SHA256 hash of canonical JSON from string
    /// - Parameter jsonString: JSON string (any formatting)
    /// - Returns: SHA256 digest (32 bytes)
    public static func hash(jsonString: String) throws -> Data {
        let canonical = try canonicalize(jsonString: jsonString)
        return Data(SHA256.hash(data: canonical))
    }

    public enum JSONError: Error {
        case invalidUTF8
    }
}

import CryptoKit
