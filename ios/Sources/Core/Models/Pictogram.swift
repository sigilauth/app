import Foundation

/// 5-emoji pictogram derived from device fingerprint
/// Per protocol-spec §3.6
public struct Pictogram: Equatable, Sendable {
    /// 5 emoji characters
    public let emojis: [String]

    /// Space-separated canonical names (e.g., "tree rocket mushroom orange moai")
    /// Per D10: JSON uses spaces, URLs use hyphens
    public let speakable: String

    public init(emojis: [String], speakable: String) {
        precondition(emojis.count == 5, "Pictogram must have exactly 5 emojis")
        self.emojis = emojis
        self.speakable = speakable
    }

    /// Combined emoji string for display
    public var display: String {
        emojis.joined()
    }
}
