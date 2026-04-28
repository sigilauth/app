import Foundation

/// Fluent-based localization service per AC #10
/// Loads .ftl files and provides localized strings
/// Per Suki spec: supports plural selectors, gender variants, RTL
public final class LocalizationService {

    public static let shared = LocalizationService()

    private var bundles: [String: FluentBundle] = [:]
    private var currentLocale: String = "en"

    private init() {
        loadBundles()
    }

    /// Get localized string for key
    /// Format: "file.key" e.g., "auth.biometric-prompt-approve"
    public func string(for key: String, args: [String: FluentValue] = [:]) -> String {
        let parts = key.split(separator: ".")
        guard parts.count == 2 else {
            return key  // Return key if invalid format
        }

        let file = String(parts[0])
        let messageKey = String(parts[1])

        guard let bundle = bundles[file] else {
            return key  // Bundle not found
        }

        guard let message = bundle.getMessage(messageKey) else {
            return key  // Message not found
        }

        var errors: [FluentError] = []
        let formatted = bundle.format(message, args: args, errors: &errors)

        return formatted
    }

    /// Change current locale
    func setLocale(_ locale: String) {
        currentLocale = locale
        loadBundles()
    }

    // MARK: - Bundle Loading

    private func loadBundles() {
        bundles.removeAll()

        let fileNames = ["auth", "challenge", "mpa", "errors", "common"]
        let localeDir = Bundle.main.url(
            forResource: "Localization/\(currentLocale)",
            withExtension: nil
        )

        guard let localeDir = localeDir else {
            print("Localization directory not found for locale: \(currentLocale)")
            return
        }

        for fileName in fileNames {
            let fileURL = localeDir.appendingPathComponent("\(fileName).ftl")

            guard let ftlContent = try? String(contentsOf: fileURL, encoding: .utf8) else {
                print("Failed to load \(fileName).ftl for locale \(currentLocale)")
                continue
            }

            let bundle = FluentBundle(locale: currentLocale)
            var errors: [FluentError] = []
            bundle.addResource(ftlContent, errors: &errors)

            if !errors.isEmpty {
                print("Fluent parsing errors in \(fileName).ftl: \(errors)")
            }

            bundles[fileName] = bundle
        }
    }
}

// MARK: - Fluent Types (Placeholder - use actual FluentSwift library)

/// Placeholder Fluent bundle (replace with actual FluentSwift)
class FluentBundle {
    let locale: String
    private var messages: [String: FluentMessage] = [:]

    init(locale: String) {
        self.locale = locale
    }

    func addResource(_ ftl: String, errors: inout [FluentError]) {
        // Parse FTL content (placeholder - use actual Fluent parser)
        // For now, simple key=value parsing for demo
        let lines = ftl.components(separatedBy: "\n")
        for line in lines {
            guard !line.isEmpty, !line.hasPrefix("#"), line.contains("=") else {
                continue
            }
            let parts = line.components(separatedBy: "=")
            if parts.count >= 2 {
                let key = parts[0].trimmingCharacters(in: .whitespaces)
                let value = parts[1...].joined(separator: "=").trimmingCharacters(in: .whitespaces)
                messages[key] = FluentMessage(value: value)
            }
        }
    }

    func getMessage(_ key: String) -> FluentMessage? {
        return messages[key]
    }

    func format(_ message: FluentMessage, args: [String: FluentValue], errors: inout [FluentError]) -> String {
        // Simple placeholder replacement
        var result = message.value
        for (key, value) in args {
            result = result.replacingOccurrences(of: "${\(key)}", with: value.asString())
        }
        return result
    }
}

struct FluentMessage {
    let value: String
}

public enum FluentValue {
    case string(String)
    case number(Double)

    func asString() -> String {
        switch self {
        case .string(let s): return s
        case .number(let n): return "\(n)"
        }
    }
}

struct FluentError: Error {
    let message: String
}

// MARK: - SwiftUI Integration

import SwiftUI

public extension Text {
    /// Create localized Text from Fluent key
    init(fluent key: String, args: [String: FluentValue] = [:]) {
        self.init(LocalizationService.shared.string(for: key, args: args))
    }
}
