import Foundation

/// Protocol-level constants per protocol-spec §3.6
/// Shared across crypto, UI, and accessibility implementations
public enum ProtocolConstants {

    /// 64 emojis indexed 0-63 per protocol-spec §3.6
    public static let EMOJI_LIST: [String] = [
        "🍎", "🍌", "🍇", "🍊", "🍋", "🍒", "🍓", "🥝",   // 0-7:   fruits
        "🥕", "🌽", "🥦", "🍄", "🌶️", "🥑", "🧅", "🥜",   // 8-15:  vegetables/nuts
        "🍕", "🍔", "🌮", "🍩", "🍪", "🎂", "🧁", "🍿",   // 16-23: food
        "🚗", "🚕", "🚌", "🚀", "✈️", "🚁", "⛵", "🚲",   // 24-31: transport
        "🐕", "🐈", "🐟", "🦋", "🐝", "🦊", "🦁", "🐘",   // 32-39: animals
        "🌲", "🌻", "🌵", "🍀", "🌸", "🌈", "⭐", "🌙",   // 40-47: nature
        "🏠", "🏔️", "⛰️", "🌋", "🏝️", "🗿", "⛺", "🏰",   // 48-55: places
        "🔑", "🔔", "📚", "🎸", "⚓", "👑", "💎", "🔥"    // 56-63: objects
    ]

    /// Canonical speakable names per protocol-spec §3.6
    public static let EMOJI_NAMES: [String] = [
        "apple", "banana", "grapes", "orange", "lemon", "cherry", "strawberry", "kiwi",
        "carrot", "corn", "broccoli", "mushroom", "pepper", "avocado", "onion", "peanut",
        "pizza", "burger", "taco", "donut", "cookie", "cake", "cupcake", "popcorn",
        "car", "taxi", "bus", "rocket", "plane", "helicopter", "sailboat", "bicycle",
        "dog", "cat", "fish", "butterfly", "bee", "fox", "lion", "elephant",
        "tree", "sunflower", "cactus", "clover", "blossom", "rainbow", "star", "moon",
        "house", "mountain", "peak", "volcano", "island", "moai", "tent", "castle",
        "key", "bell", "books", "guitar", "anchor", "crown", "diamond", "fire"
    ]

    /// Map emoji to canonical name for VoiceOver announcements
    /// - Parameter emoji: The emoji character (e.g., "🍎")
    /// - Returns: Canonical name (e.g., "apple") or the emoji itself if not found
    public static func name(for emoji: String) -> String {
        guard let index = EMOJI_LIST.firstIndex(of: emoji) else {
            return emoji
        }
        return EMOJI_NAMES[index]
    }
}
