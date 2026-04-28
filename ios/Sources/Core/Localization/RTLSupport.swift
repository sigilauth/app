import SwiftUI

/// RTL layout support per AC #11
/// SwiftUI automatically mirrors layouts for RTL languages (ar, he, fa, ur)
/// This file provides utilities for RTL-aware layout

extension View {
    /// Apply RTL-aware leading alignment
    /// Automatically becomes trailing in RTL locales
    func leadingAlignment() -> some View {
        self.frame(maxWidth: .infinity, alignment: .leading)
    }

    /// Apply RTL-aware trailing alignment
    /// Automatically becomes leading in RTL locales
    func trailingAlignment() -> some View {
        self.frame(maxWidth: .infinity, alignment: .trailing)
    }
}

/// Environment value for current layout direction
extension EnvironmentValues {
    var isRTL: Bool {
        layoutDirection == .rightToLeft
    }
}

/// RTL-aware spacing and padding
extension EdgeInsets {
    /// Create EdgeInsets with RTL-aware horizontal values
    /// Leading and trailing automatically flip in RTL
    static func horizontal(leading: CGFloat = 0, trailing: CGFloat = 0, vertical: CGFloat = 0) -> EdgeInsets {
        EdgeInsets(top: vertical, leading: leading, bottom: vertical, trailing: trailing)
    }
}

// MARK: - RTL Testing Support

/// Preview helper for testing RTL layouts
struct RTLPreview<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        VStack {
            Text("LTR (Left-to-Right)")
                .font(.caption)
                .foregroundColor(.secondary)

            content
                .environment(\.layoutDirection, .leftToRight)
                .border(Color.blue, width: 1)

            Divider().padding(.vertical)

            Text("RTL (Right-to-Left) - Arabic/Hebrew")
                .font(.caption)
                .foregroundColor(.secondary)

            content
                .environment(\.layoutDirection, .rightToLeft)
                .border(Color.green, width: 1)
        }
        .padding()
    }
}

// MARK: - RTL-Safe Components

/// RTL-aware horizontal stack
/// Use this instead of HStack for RTL-sensitive layouts
struct RTLAwareHStack<Content: View>: View {
    @Environment(\.layoutDirection) var layoutDirection

    let alignment: VerticalAlignment
    let spacing: CGFloat?
    let content: Content

    init(
        alignment: VerticalAlignment = .center,
        spacing: CGFloat? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.alignment = alignment
        self.spacing = spacing
        self.content = content()
    }

    var body: some View {
        HStack(alignment: alignment, spacing: spacing) {
            content
        }
        .environment(\.layoutDirection, layoutDirection)
    }
}
