import Foundation

public struct ClaimCodeValidator {
    private static let validCharacters = Set("ABCDEFGHJKLMNPQRSTUVWXYZ23456789")

    public static func isValid(_ code: String) -> Bool {
        guard code.count == 6 else {
            return false
        }

        let uppercase = code.uppercased()
        return uppercase.allSatisfy { validCharacters.contains($0) }
    }

    public static func normalize(_ code: String) -> String {
        return code.uppercased()
    }

    public static func formatForDisplay(_ code: String) -> String {
        guard code.count == 6 else {
            return code
        }

        let index = code.index(code.startIndex, offsetBy: 3)
        return "\(code[..<index])-\(code[index...])"
    }
}
