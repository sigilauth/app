#if canImport(UIKit)
import SwiftUI
import UIKit

/// Mnemonic display with screenshot prevention per AC #8
/// Uses UIScreen.isCaptured monitoring (iOS 11+)
@available(iOS 16.0, macOS 14.0, *)
struct MnemonicDisplayView: View {
    let mnemonic: [String]  // 12 or 24 words

    @State private var isScreenCaptured = false
    @State private var showWarning = false

    var body: some View {
        VStack(spacing: .s6) {
            // Title and subtitle
            VStack(spacing: .s2) {
                Text("Backup Phrase")
                    .font(.title2)
                    .foregroundColor(.sigilText)
                    .accessibilityAddTraits(.isHeader)

                Text("Write down these 12 words in order. You'll need them to recover your device identity.")
                    .font(.body)
                    .multilineTextAlignment(.center)
                    .foregroundColor(.sigilTextMuted)
            }

            // Screenshot warning banner
            HStack(spacing: .s3) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundColor(.sigilWarning)
                Text("Screenshot Protection Active")
                    .font(.callout)
                    .foregroundColor(.sigilText)
            }
            .frame(maxWidth: .infinity)
            .padding(.s3)
            .background(Color.sigilWarning.opacity(0.15))
            .cornerRadius(.rMd)
            .overlay(
                RoundedRectangle(cornerRadius: .rMd)
                    .stroke(Color.sigilWarning.opacity(0.3), lineWidth: 1)
            )

            // Mnemonic display OR screenshot warning
            if isScreenCaptured {
                // Screenshot prevention per AC #8
                VStack(spacing: 16) {
                    Image(systemName: "eye.slash.fill")
                        .font(.system(size: 64))
                        .foregroundColor(.sigilDanger)

                    Text("Screen Recording Detected")
                        .font(.headline)
                        .foregroundColor(.sigilDanger)

                    Text("For your security, the recovery phrase is hidden when screen recording or mirroring is active.")
                        .font(.body)
                        .multilineTextAlignment(.center)
                        .foregroundColor(.sigilTextMuted)
                }
                .padding()
                .background(Color.sigilDanger.opacity(0.1))
                .cornerRadius(.rMd)
                .accessibilityElement(children: .combine)
                .accessibilityLabel("Recovery phrase hidden. Screen recording detected. For your security, the recovery phrase is hidden when screen recording or mirroring is active.")
            } else {
                // Display mnemonic words
                LazyVGrid(columns: [
                    GridItem(.flexible()),
                    GridItem(.flexible())
                ], spacing: .s3) {
                    ForEach(Array(mnemonic.enumerated()), id: \.offset) { index, word in
                        HStack(spacing: .s2) {
                            Text("\(index + 1)")
                                .font(.caption)
                                .foregroundColor(.sigilTextDim)
                                .frame(width: 24, alignment: .trailing)
                            Text(word)
                                .font(.system(.body, design: .monospaced))
                                .foregroundColor(.sigilText)
                            Spacer()
                        }
                        .padding(.horizontal, .s3)
                        .padding(.vertical, .s2)
                        .background(Color.sigilSurface)
                        .cornerRadius(.rMd)
                        .overlay(
                            RoundedRectangle(cornerRadius: .rMd)
                                .stroke(Color.sigilBorder, lineWidth: 1)
                        )
                        .accessibilityElement(children: .combine)
                        .accessibilityLabel("Word \(index + 1): \(word)")
                    }
                }
                .accessibilityElement(children: .contain)
                .accessibilityLabel("Recovery phrase words")
            }

            Spacer()

            // Security warning
            VStack(spacing: 8) {
                HStack(spacing: 8) {
                    Image(systemName: "lock.shield.fill")
                        .foregroundColor(.orange)
                    Text("Never share your recovery phrase")
                        .font(.caption)
                        .fontWeight(.medium)
                }

                Text("Anyone with these words can access your account. Keep them safe and private.")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding()
            .background(Color.orange.opacity(0.1))
            .cornerRadius(8)

            // Confirm button
            Button("I've Written It Down") {
                // Continue to next step
            }
            .buttonStyle(.borderedProminent)
            .frame(minHeight: 44)
            .disabled(isScreenCaptured)
            .accessibilityHint(isScreenCaptured ? "Disabled while screen recording is active" : "Confirm you have safely written down your recovery phrase")
        }
        .padding()
        .navigationTitle("Recovery Phrase")
        .onAppear {
            startMonitoringScreenCapture()
        }
        .onDisappear {
            stopMonitoringScreenCapture()
        }
    }

    // MARK: - Screenshot Prevention (AC #8)

    private func startMonitoringScreenCapture() {
        // Monitor UIScreen.isCaptured (iOS 11+)
        isScreenCaptured = UIScreen.main.isCaptured

        NotificationCenter.default.addObserver(
            forName: UIScreen.capturedDidChangeNotification,
            object: nil,
            queue: .main
        ) { _ in
            self.isScreenCaptured = UIScreen.main.isCaptured

            if self.isScreenCaptured {
                // Announce to VoiceOver (iOS 16 compatible)
                UIAccessibility.post(
                    notification: .announcement,
                    argument: "Screen recording detected. Recovery phrase hidden for security."
                )
            } else {
                UIAccessibility.post(
                    notification: .announcement,
                    argument: "Screen recording stopped. Recovery phrase visible."
                )
            }
        }
    }

    private func stopMonitoringScreenCapture() {
        NotificationCenter.default.removeObserver(
            self,
            name: UIScreen.capturedDidChangeNotification,
            object: nil
        )
    }
}
#endif
