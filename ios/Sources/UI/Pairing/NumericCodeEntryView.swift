import SwiftUI
import SigilAuthCore

#if canImport(UIKit)
import UIKit
#endif

/// 8-digit numeric pairing code entry with auto-advance and paste support
/// Per product-spec §3.7: 8 digits, auto-advance, paste, accessibility
struct NumericCodeEntryView: View {
    @StateObject private var viewModel: NumericCodeEntryViewModel

    init(onCodeComplete: @escaping (String) -> Void) {
        _viewModel = StateObject(wrappedValue: NumericCodeEntryViewModel(onCodeComplete: onCodeComplete))
    }

    var body: some View {
        VStack(spacing: .s6) {
            Text(fluent: "auth.pairing-code-instruction")
                .font(.body)
                .foregroundColor(.sigilTextMuted)
                .multilineTextAlignment(.center)
                .accessibilityAddTraits(.isStaticText)

            HStack(spacing: .s2) {
                ForEach(0..<8, id: \.self) { index in
                    DigitField(
                        digit: $viewModel.digits[index],
                        isFocused: viewModel.focusedIndex == index,
                        onDigitEntered: {
                            viewModel.handleDigitEntered(at: index)
                        },
                        onBackspace: {
                            viewModel.handleBackspace(at: index)
                        }
                    )
                    .accessibilityLabel("Digit \(index + 1) of 8")
                    .accessibilityValue(viewModel.digits[index].isEmpty ? "empty" : viewModel.digits[index])
                    .accessibilityHint(index == 0 ? "Enter 8 digit pairing code. You can paste the entire code." : "")
                    .onTapGesture {
                        viewModel.focusedIndex = index
                    }

                    if index == 3 {
                        Text("-")
                            .font(.title)
                            .foregroundColor(.sigilTextDim)
                            .accessibilityHidden(true)
                    }
                }
            }
            #if os(macOS)
            .onPasteCommand(of: [.plainText]) { providers in
                guard let provider = providers.first else { return }
                provider.loadItem(forTypeIdentifier: "public.plain-text") { data, error in
                    if let data = data as? Data,
                       let text = String(data: data, encoding: .utf8) {
                        DispatchQueue.main.async {
                            viewModel.handlePaste(text)
                        }
                    }
                }
            }
            #endif

            if let error = viewModel.errorMessage {
                Text(fluent: error)
                    .font(.caption)
                    .foregroundColor(.red)
                    .accessibilityLabel("Error: \(LocalizationService.shared.string(for: error))")
                    .accessibilityAddTraits(.isStaticText)
                    .transition(.opacity)
            }

            if viewModel.isSubmitting {
                ProgressView()
                    .accessibilityLabel("Verifying code")
            }
        }
        .padding()
    }
}

/// Single digit input field
struct DigitField: View {
    @Binding var digit: String
    let isFocused: Bool
    let onDigitEntered: () -> Void
    let onBackspace: () -> Void

    @FocusState private var fieldFocused: Bool

    var body: some View {
        TextField("", text: $digit)
            .frame(width: 40, height: 56)
            .multilineTextAlignment(.center)
            .font(.system(.body, design: .monospaced))
            .foregroundColor(.sigilText)
            .focused($fieldFocused)
            #if os(iOS)
            .keyboardType(.numberPad)
            .textContentType(.oneTimeCode)
            #endif
            .background(Color.sigilSurface)
            .cornerRadius(.rMd)
            .overlay(
                RoundedRectangle(cornerRadius: .rMd)
                    .stroke(
                        digit.isEmpty ? Color.sigilBorder : Color.sigilPrimary,
                        lineWidth: digit.isEmpty ? 1 : 2
                    )
            )
            .onChange(of: digit) { newValue in
                if newValue.count > 1 {
                    digit = String(newValue.prefix(1))
                }

                if newValue.count == 1, newValue.allSatisfy({ $0.isNumber }) {
                    onDigitEntered()
                }
            }
            .onChange(of: isFocused) { focused in
                fieldFocused = focused
            }
    }
}

/// ViewModel for numeric code entry with auto-advance and paste logic
@MainActor
final class NumericCodeEntryViewModel: ObservableObject {
    @Published var digits: [String] = Array(repeating: "", count: 8)
    @Published var focusedIndex: Int? = 0
    @Published var errorMessage: String?
    @Published var isSubmitting: Bool = false

    private let onCodeComplete: (String) -> Void

    init(onCodeComplete: @escaping (String) -> Void) {
        self.onCodeComplete = onCodeComplete
    }

    func handleDigitEntered(at index: Int) {
        errorMessage = nil

        if index < 7 {
            focusedIndex = index + 1
        } else {
            submitCode()
        }
    }

    func handleBackspace(at index: Int) {
        if digits[index].isEmpty && index > 0 {
            focusedIndex = index - 1
        }
    }

    func handlePaste(_ text: String) {
        let cleaned = text.filter { $0.isNumber }

        guard cleaned.count == 8 else {
            errorMessage = "auth.pairing-code-error-invalid-format"
            return
        }

        for (index, char) in cleaned.enumerated() {
            digits[index] = String(char)
        }

        focusedIndex = nil
        submitCode()
    }

    private func submitCode() {
        let code = digits.joined()

        guard code.count == 8, code.allSatisfy({ $0.isNumber }) else {
            errorMessage = "auth.pairing-code-error-invalid-format"
            return
        }

        #if os(iOS)
        UIAccessibility.post(notification: .announcement, argument: "Code complete")
        #endif
        isSubmitting = true
        onCodeComplete(code)
    }

    func setError(_ message: String) {
        errorMessage = message
        isSubmitting = false
    }
}
