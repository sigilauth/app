import SwiftUI

/// Sigil Auth design tokens from sigilauth.com
/// Provides consistent color palette across all screens
public extension Color {
    // MARK: - Backgrounds
    static let sigilBg = Color(hex: "07070c")
    static let sigilBgRaised = Color(hex: "0e0e16")
    static let sigilSurface = Color(hex: "141420")

    // MARK: - Borders & Dividers
    static let sigilBorder = Color(hex: "252536")

    // MARK: - Text
    static let sigilText = Color(hex: "f5f5f7")
    static let sigilTextMuted = Color(hex: "9ca0b0")
    static let sigilTextDim = Color(hex: "636879")

    // MARK: - Brand
    static let sigilPrimary = Color(hex: "4d88ff")
    static let sigilAccent = Color(hex: "3dfce8")

    // MARK: - Semantic
    static let sigilSuccess = Color(hex: "00e676")
    static let sigilDanger = Color(hex: "ff5a5a")
    static let sigilWarning = Color(hex: "ffb300")
}

extension Color {
    init(hex: String) {
        let scanner = Scanner(string: hex)
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)

        let r = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = Double(rgbValue & 0x0000FF) / 255.0

        self.init(red: r, green: g, blue: b)
    }
}

/// Sigil Auth spacing tokens
public extension CGFloat {
    static let s2: CGFloat = 8    // 0.5rem
    static let s3: CGFloat = 12   // 0.75rem
    static let s4: CGFloat = 16   // 1rem
    static let s6: CGFloat = 24   // 1.5rem
    static let s8: CGFloat = 32   // 2rem
}

/// Sigil Auth corner radii
public extension CGFloat {
    static let rMd: CGFloat = 10  // --r-md
    static let rLg: CGFloat = 14  // --r-lg
}
