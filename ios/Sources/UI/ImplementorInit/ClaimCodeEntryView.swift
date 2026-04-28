import SwiftUI

public struct ClaimCodeEntryView: View {
    @Binding var characters: [String]
    @FocusState private var focusedField: Int?
    let onClaim: () -> Void
    let onScanQR: () -> Void

    public init(
        characters: Binding<[String]>,
        onClaim: @escaping () -> Void,
        onScanQR: @escaping () -> Void
    ) {
        self._characters = characters
        self.onClaim = onClaim
        self.onScanQR = onScanQR
    }

    public var body: some View {
        VStack(spacing: 24) {
            Text("Enter Claim Code")
                .font(.title2)
                .fontWeight(.semibold)

            Text("Enter the 6-character code from your browser")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            HStack(spacing: 8) {
                ForEach(0..<6) { index in
                    TextField("", text: $characters[index])
                        .font(.system(.title, design: .monospaced))
                        .multilineTextAlignment(.center)
                        .frame(width: 50, height: 60)
                        .background(Color(.systemGray6))
                        .cornerRadius(8)
                        .textInputAutocapitalization(.characters)
                        .keyboardType(.asciiCapable)
                        .focused($focusedField, equals: index)
                        .onChange(of: characters[index]) { newValue in
                            handleCharacterInput(index: index, value: newValue)
                        }

                    if index == 2 {
                        Text("-")
                            .foregroundColor(.secondary)
                            .font(.title)
                    }
                }
            }

            Button("Claim") {
                onClaim()
            }
            .buttonStyle(.borderedProminent)
            .disabled(!isValid)

            Divider()

            Button(action: onScanQR) {
                HStack {
                    Image(systemName: "qrcode.viewfinder")
                    Text("Scan QR Code Instead")
                }
            }
            .buttonStyle(.bordered)
        }
        .padding()
        .onAppear {
            focusedField = 0
        }
    }

    private var isValid: Bool {
        characters.allSatisfy { !$0.isEmpty && ClaimCodeValidator.isValid($0) }
    }

    private func handleCharacterInput(index: Int, value: String) {
        let filtered = value.uppercased().filter { char in
            ClaimCodeValidator.isValid(String(char))
        }

        if filtered.isEmpty {
            characters[index] = ""
            return
        }

        characters[index] = String(filtered.prefix(1))

        if !filtered.isEmpty && index < 5 {
            focusedField = index + 1
        }
    }
}
