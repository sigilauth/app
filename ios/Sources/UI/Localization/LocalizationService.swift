import Foundation
import SwiftUI

/// Localization service using NSLocalizedString
/// Wraps standard iOS localization with Fluent-compatible key names
public final class LocalizationService {
    public static let shared = LocalizationService()

    private init() {}

    /// Get localized string for key
    /// Falls back to key if translation not found
    public func string(_ key: String, arguments: [String: String] = [:]) -> String {
        var localizedString = NSLocalizedString(key, bundle: .module, comment: "")

        // If translation not found, NSLocalizedString returns the key
        // Replace placeholders in Fluent format { $variable } with values
        for (argKey, argValue) in arguments {
            localizedString = localizedString.replacingOccurrences(
                of: "{ $\(argKey) }",
                with: argValue
            )
        }

        return localizedString
    }
}

// MARK: - SwiftUI Helper

public extension Text {
    /// Create localized Text from key
    static func localized(_ key: String, arguments: [String: String] = [:]) -> Text {
        Text(LocalizationService.shared.string(key, arguments: arguments))
    }
}

/// Environment key for localization service
struct LocalizationServiceKey: EnvironmentKey {
    static let defaultValue: LocalizationService = .shared
}

public extension EnvironmentValues {
    var localization: LocalizationService {
        get { self[LocalizationServiceKey.self] }
        set { self[LocalizationServiceKey.self] = newValue }
    }
}
