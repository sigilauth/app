import SwiftUI

/// OOB approval waiting screen - displayed while polling /pair/complete
/// Shows when server returns NOT_APPROVED error
struct AwaitingAdminApprovalView: View {
    let serverId: String
    let pollAttempts: Int

    private var timeElapsed: String {
        let seconds = pollAttempts * 2
        let minutes = seconds / 60
        let remainingSeconds = seconds % 60

        if minutes > 0 {
            return "\(minutes)m \(remainingSeconds)s"
        } else {
            return "\(seconds)s"
        }
    }

    private var pollProgress: Double {
        let maxAttempts = 150.0 // 5 minutes at 2s intervals
        return Double(pollAttempts) / maxAttempts
    }

    var body: some View {
        VStack(spacing: 24) {
            // Spinner
            ProgressView()
                .scaleEffect(1.5)
                .padding(.bottom, 8)

            // Title
            Text.localized("pair-oob-title")
                .font(.title2)
                .fontWeight(.semibold)
                .multilineTextAlignment(.center)

            // Subtitle
            Text.localized("pair-oob-subtitle")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            // Server info card
            VStack(alignment: .leading, spacing: 8) {
                Text.localized("pair-oob-server-label")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text(serverId)
                    .font(.body)
                    .fontWeight(.medium)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(8)
            .padding(.horizontal)

            // Status info
            VStack(spacing: 8) {
                HStack {
                    Image(systemName: "clock")
                        .foregroundColor(.secondary)
                    Text(LocalizationService.shared.string("pair-oob-time-elapsed", arguments: ["timeElapsed": .string(timeElapsed)]))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                ProgressView(value: pollProgress)
                    .progressViewStyle(.linear)
                    .padding(.horizontal)

                Text.localized("pair-oob-poll-status")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal)

            Spacer()

            // Info text
            Text.localized("pair-oob-wait-message")
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
                .padding(.bottom)
        }
        .padding()
    }
}

#Preview {
    AwaitingAdminApprovalView(
        serverId: "sigil-prod-01",
        pollAttempts: 15
    )
}
