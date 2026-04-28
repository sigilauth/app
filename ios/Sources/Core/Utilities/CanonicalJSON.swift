import Foundation

public enum CanonicalJSONError: Error {
    case serializationFailed
    case invalidJSON
}

public struct CanonicalJSON {

    public static func canonicalize(_ object: Any) throws -> Data {
        guard JSONSerialization.isValidJSONObject(object) else {
            throw CanonicalJSONError.invalidJSON
        }

        let sortedObject = try sortKeys(object)

        let data = try JSONSerialization.data(
            withJSONObject: sortedObject,
            options: [.sortedKeys, .withoutEscapingSlashes]
        )

        let jsonString = String(data: data, encoding: .utf8) ?? ""
        let compacted = jsonString
            .replacingOccurrences(of: " ", with: "")
            .replacingOccurrences(of: "\n", with: "")
            .replacingOccurrences(of: "\r", with: "")
            .replacingOccurrences(of: "\t", with: "")

        guard let canonical = compacted.data(using: .utf8) else {
            throw CanonicalJSONError.serializationFailed
        }

        return canonical
    }

    private static func sortKeys(_ object: Any) throws -> Any {
        if let dict = object as? [String: Any] {
            var sorted = [String: Any]()
            for (key, value) in dict {
                sorted[key] = try sortKeys(value)
            }
            return sorted
        } else if let array = object as? [Any] {
            return try array.map { try sortKeys($0) }
        } else {
            return object
        }
    }
}
