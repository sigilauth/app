import SwiftUI

public struct SessionPictogramView: View {
    let pictogram: SessionPictogram
    let timeRemaining: Int
    let onConfirm: () -> Void
    let onDeny: () -> Void

    @State private var isExpired: Bool = false

    public init(
        pictogram: SessionPictogram,
        timeRemaining: Int,
        onConfirm: @escaping () -> Void,
        onDeny: @escaping () -> Void
    ) {
        self.pictogram = pictogram
        self.timeRemaining = timeRemaining
        self.onConfirm = onConfirm
        self.onDeny = onDeny
    }

    public var body: some View {
        VStack(spacing: 24) {
            VStack(alignment: .leading, spacing: 8) {
                Text("Confirm Server Identity")
                    .font(.title2)
                    .fontWeight(.semibold)

                Text("Verify this pictogram matches what the server displays:")
                    .font(.body)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            pictogramGrid
                .padding(16)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.secondary.opacity(0.1))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .strokeBorder(Color.accentColor.opacity(0.2), lineWidth: 1)
                        )
                )
                .accessibilityElement(children: .combine)
                .accessibilityLabel("Session pictogram verification")
                .accessibilityHint("Verify the six emoji-word pairs match what the server displays, then confirm or deny")

            speakableTextSection

            timerSection

            buttonsSection
        }
        .padding(24)
        .frame(maxWidth: 600)
        .onChange(of: timeRemaining) { newValue in
            if newValue <= 0 && !isExpired {
                isExpired = true
                onDeny()
            }
        }
    }

    private var pictogramGrid: some View {
        LazyVGrid(columns: [
            GridItem(.flexible()),
            GridItem(.flexible()),
            GridItem(.flexible())
        ], spacing: 16) {
            ForEach(Array(pictogram.emojis.enumerated()), id: \.offset) { index, emoji in
                VStack(spacing: 8) {
                    Text(emoji)
                        .font(.system(size: 48))

                    Text(pictogram.names[index])
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .accessibilityElement(children: .combine)
                .accessibilityLabel(pictogram.names[index])
            }
        }
    }

    private var speakableTextSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Speakable format:")
                .font(.caption)
                .fontWeight(.medium)
                .foregroundColor(.secondary)

            Text(pictogram.speakable)
                .font(.body)
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.secondary.opacity(0.1))
                .cornerRadius(8)
                .textSelection(.enabled)
        }
    }

    private var timerSection: some View {
        HStack {
            Image(systemName: timeRemaining <= 3 ? "exclamationmark.triangle.fill" : "clock")
                .foregroundColor(timeRemaining <= 3 ? .red : .secondary)

            Text("Expires in \(timeRemaining)s")
                .font(.caption)
                .foregroundColor(timeRemaining <= 3 ? .red : .secondary)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Expires in \(timeRemaining) seconds")
    }

    private var buttonsSection: some View {
        HStack(spacing: 16) {
            Button("Deny") {
                onDeny()
            }
            .buttonStyle(.bordered)
            .controlSize(.large)
            .disabled(isExpired)
            .keyboardShortcut(.cancelAction)

            Button("Confirm") {
                onConfirm()
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .disabled(isExpired)
            .keyboardShortcut(.defaultAction)
        }
    }
}

#if DEBUG
struct SessionPictogramView_Previews: PreviewProvider {
    static var previews: some View {
        SessionPictogramView(
            pictogram: SessionPictogram(
                emojis: ["🍎", "🚀", "🦊", "⚓", "🌙", "🏠"],
                names: ["apple", "rocket", "fox", "anchor", "moon", "house"]
            ),
            timeRemaining: 8,
            onConfirm: {},
            onDeny: {}
        )
        .previewLayout(.sizeThatFits)

        SessionPictogramView(
            pictogram: SessionPictogram(
                emojis: ["🍎", "🚀", "🦊", "⚓", "🌙", "🏠"],
                names: ["apple", "rocket", "fox", "anchor", "moon", "house"]
            ),
            timeRemaining: 2,
            onConfirm: {},
            onDeny: {}
        )
        .previewDisplayName("Warning state (2s)")
        .previewLayout(.sizeThatFits)
    }
}
#endif
