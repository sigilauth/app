import SwiftUI

public struct MnemonicDisplayView: View {
    let words: [String]
    @Binding var confirmedWrittenDown: Bool
    let onContinue: () -> Void
    let onCopy: () -> Void

    public init(
        words: [String],
        confirmedWrittenDown: Binding<Bool>,
        onContinue: @escaping () -> Void,
        onCopy: @escaping () -> Void
    ) {
        self.words = words
        self._confirmedWrittenDown = confirmedWrittenDown
        self.onContinue = onContinue
        self.onCopy = onCopy
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                HStack {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundColor(.orange)
                    Text("Write this down — you cannot recover it later")
                        .fontWeight(.semibold)
                        .foregroundColor(.orange)
                }
                .padding()
                .background(Color.orange.opacity(0.1))
                .cornerRadius(8)

                LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: 12) {
                    ForEach(Array(words.enumerated()), id: \.offset) { index, word in
                        HStack {
                            Text("\(index + 1).")
                                .foregroundColor(.secondary)
                                .font(.caption)
                                .frame(width: 24, alignment: .trailing)
                            Text(word)
                                .font(.body)
                                .fontWeight(.medium)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .padding(8)
                        .background(Color(.systemGray6))
                        .cornerRadius(4)
                    }
                }

                Button(action: onCopy) {
                    HStack {
                        Image(systemName: "doc.on.doc")
                        Text("Copy to Clipboard")
                    }
                }
                .buttonStyle(.bordered)

                Toggle(isOn: $confirmedWrittenDown) {
                    Text("I have written down all 24 words")
                        .font(.body)
                }

                Button("Continue") {
                    onContinue()
                }
                .buttonStyle(.borderedProminent)
                .disabled(!confirmedWrittenDown)
            }
            .padding()
        }
    }
}
