import SwiftUI
import SigilAuthCore

/// Pictogram display per Aria §3 + protocol-spec §3.6
/// REQ: role="img", speakable text, accessible pattern
struct PictogramView: View {
    let pictogram: Pictogram

    var body: some View {
        VStack(spacing: .s3) {
            // Visual emoji display (aria-hidden equivalent in SwiftUI)
            HStack(spacing: .s2) {
                ForEach(pictogram.emojis, id: \.self) { emoji in
                    Text(emoji)
                        .font(.system(size: 64))  // Min 32px per Aria §3.2.3
                }
            }
            .accessibilityHidden(true)  // Visual only

            // Speakable text (screen reader announces)
            Text(pictogram.speakable)
                .font(.system(.body, design: .monospaced))
                .foregroundColor(.sigilTextMuted)
                .accessibilityLabel("Speakable pictogram: \(pictogramSpokenForm)")

            // Detailed emoji descriptions (collapsible per Aria §3.2.1)
            DisclosureGroup("Emoji descriptions") {
                VStack(alignment: .leading, spacing: .s2) {
                    ForEach(Array(pictogram.emojis.enumerated()), id: \.offset) { index, emoji in
                        Text("\(index + 1). \(emojiName(emoji)) \(emoji)")
                            .font(.caption)
                            .foregroundColor(.sigilTextDim)
                    }
                }
            }
            .accessibilityElement(children: .contain)
        }
        // Container accessibility per Aria §3.2.1
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Device fingerprint: \(pictogramSpokenForm)")
        .accessibilityAddTraits(.isImage)
    }

    private var pictogramSpokenForm: String {
        // Convert hyphens to commas for natural speech
        pictogram.speakable.replacingOccurrences(of: "-", with: ", ")
    }

    private func emojiName(_ emoji: String) -> String {
        ProtocolConstants.name(for: emoji)
    }
}
