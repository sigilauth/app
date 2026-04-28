import SwiftUI
import LocalAuthentication

#if canImport(UIKit)
import UIKit
#endif

/// Challenge approval screen per Aria §2.2
/// REQ: Action context display before biometric gate
/// REQ: 44×44pt touch targets, VoiceOver announcements
struct ApprovalView: View {
    let challenge: ChallengePayload  // TODO: Define when B0 complete
    @State private var biometricType: String = "Face ID"

    var body: some View {
        VStack(spacing: .s6) {
            // Badge
            Text.localized("challenge-title")
                .font(.system(size: 12, weight: .medium))
                .foregroundColor(.white)
                .padding(.horizontal, .s3)
                .padding(.vertical, .s2)
                .background(Color.sigilPrimary)
                .cornerRadius(6)

            // Title
            Text.localized("challenge-approve-login-title")
                .font(.title2)
                .foregroundColor(.sigilText)
                .accessibilityAddTraits(.isHeader)

            // Timestamp
            Text(formatTimestamp(Date()))
                .font(.system(.body, design: .monospaced))
                .foregroundColor(.sigilTextMuted)

            // Server card
            VStack(alignment: .leading, spacing: .s2) {
                Text.localized("challenge-service-label")
                    .font(.caption)
                    .foregroundColor(.sigilTextDim)
                Text(challenge.serverName)
                    .font(.body)
                    .foregroundColor(.sigilText)
            }
            .accessibilityElement(children: .combine)
            .accessibilityLabel("Service: \(challenge.serverName)")
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .background(Color.sigilSurface)
            .cornerRadius(.rMd)
            .overlay(
                RoundedRectangle(cornerRadius: .rMd)
                    .stroke(Color.sigilBorder, lineWidth: 1)
            )

            // Action card
            VStack(alignment: .leading, spacing: .s2) {
                Text.localized("challenge-action-label")
                    .font(.caption)
                    .foregroundColor(.sigilTextDim)
                Text(challenge.actionDescription)
                    .font(.body)
                    .foregroundColor(.sigilText)
                    .accessibilityLabel("Requested action: \(challenge.actionDescription)")

                // Parameters display
                if let params = challenge.actionParams {
                    ForEach(params.keys.sorted(), id: \.self) { key in
                        HStack {
                            Text("\(key):")
                                .foregroundColor(.sigilTextDim)
                            Text("\(params[key] ?? "")")
                                .foregroundColor(.sigilText)
                        }
                        .font(.body)
                        .accessibilityElement(children: .combine)
                        .accessibilityLabel("\(key): \(params[key] ?? "")")
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .background(Color.sigilSurface)
            .cornerRadius(.rMd)
            .overlay(
                RoundedRectangle(cornerRadius: .rMd)
                    .stroke(Color.sigilBorder, lineWidth: 1)
            )

            // View Details button - shown when detailsUrl is present
            if let detailsUrl = challenge.detailsUrl,
               let url = URL(string: detailsUrl) {
                Button {
                    openDetailsURL(url)
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "info.circle")
                        Text.localized("challenge-view-details-button")
                    }
                    .font(.body)
                    .frame(maxWidth: .infinity, minHeight: 44)
                }
                .buttonStyle(.bordered)
                .accessibilityLabel("View details in browser")
            }

            TimeRemainingView(expiresAt: challenge.expiresAt)

            Spacer()

            // Action buttons per Aria §2.2: 44×44pt minimum
            HStack(spacing: .s4) {
                Button {
                    handleDeny()
                } label: {
                    Text.localized("btn-reject")
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .background(Color.sigilSurface)
                .foregroundColor(.sigilDanger)
                .cornerRadius(.rMd)
                .overlay(
                    RoundedRectangle(cornerRadius: .rMd)
                        .stroke(Color.sigilBorder, lineWidth: 1)
                )
                .accessibilityLabel("Reject request")

                Button {
                    handleApprove()
                } label: {
                    Text.localized("btn-approve")
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .background(Color.sigilPrimary)
                .foregroundColor(.white)
                .cornerRadius(.rMd)
                .accessibilityLabel("Approve with \(biometricType)")
            }
        }
        .padding()
        .onAppear {
            detectBiometricType()
        }
    }

    private func detectBiometricType() {
        let context = LAContext()
        var error: NSError?

        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
            biometricType = "biometrics"
            return
        }

        switch context.biometryType {
        case .faceID:
            biometricType = "Face ID"
        case .touchID:
            biometricType = "Touch ID"
        default:
            biometricType = "biometrics"
        }
    }

    func handleApprove() {
        // TODO: Trigger biometric, then sign challenge
    }

    func handleDeny() {
        // TODO: Send denial response
    }

    private func openDetailsURL(_ url: URL) {
        #if canImport(UIKit)
        UIApplication.shared.open(url)
        #endif
    }

    private func formatTimestamp(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss 'UTC'"
        formatter.timeZone = TimeZone(abbreviation: "UTC")
        return formatter.string(from: date)
    }
}

/// Time remaining countdown with live region updates per Aria §2.2
struct TimeRemainingView: View {
    let expiresAt: Date
    @State private var timeRemaining: String = ""
    @State private var timer: Timer?

    var body: some View {
        Text(LocalizationService.shared.string("challenge-expires-label", arguments: ["timeRemaining": .string(timeRemaining)]))
            .font(.caption)
            .foregroundColor(.secondary)
            .accessibilityLabel("Challenge expires in \(timeRemaining)")
            .onAppear {
                updateTimeRemaining()
                startTimer()
            }
            .onDisappear {
                timer?.invalidate()
            }
            .onChange(of: timeRemaining) { newValue in
                #if canImport(UIKit)
                UIAccessibility.post(notification: .announcement, argument: newValue)
                #endif
            }
    }

    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 60, repeats: true) { _ in
            updateTimeRemaining()
        }
    }

    private func updateTimeRemaining() {
        let remaining = expiresAt.timeIntervalSinceNow

        guard remaining > 0 else {
            timeRemaining = "expired"
            timer?.invalidate()
            return
        }

        let minutes = Int(remaining / 60)
        let seconds = Int(remaining.truncatingRemainder(dividingBy: 60))

        if minutes > 0 {
            timeRemaining = "\(minutes) minute\(minutes == 1 ? "" : "s")"
        } else {
            timeRemaining = "\(seconds) second\(seconds == 1 ? "" : "s")"
        }
    }
}

// Placeholder - replace with real type from B0
struct ChallengePayload {
    let serverName: String
    let actionDescription: String
    let actionParams: [String: String]?
    let expiresAt: Date
    let detailsUrl: String?  // Optional URL to view action details in external browser
}
