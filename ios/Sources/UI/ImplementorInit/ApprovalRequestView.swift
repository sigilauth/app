import SwiftUI

public struct ApprovalRequestView: View {
    let request: ImplementorInitRequest
    let timeRemaining: Int
    let onApprove: () -> Void
    let onReject: () -> Void

    public init(
        request: ImplementorInitRequest,
        timeRemaining: Int,
        onApprove: @escaping () -> Void,
        onReject: @escaping () -> Void
    ) {
        self.request = request
        self.timeRemaining = timeRemaining
        self.onApprove = onApprove
        self.onReject = onReject
    }

    public var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "key.fill")
                .font(.system(size: 64))
                .foregroundColor(.blue)

            Text("Mnemonic Request")
                .font(.title2)
                .fontWeight(.semibold)

            GroupBox {
                VStack(alignment: .leading, spacing: 12) {
                    DetailRow(label: "Implementor:", value: request.implementorName)
                    DetailRow(label: "Identifier:", value: request.implementorId)
                    DetailRow(label: "Requested:", value: formatDate(request.timestamp))
                }
            }

            Text("A new 24-word mnemonic will be generated and sent to the implementor. You will need to write it down before submission.")
                .font(.footnote)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            if timeRemaining < 60 {
                HStack {
                    Image(systemName: "clock")
                    Text("Expires in \(timeRemaining)s")
                }
                .font(.caption)
                .foregroundColor(.orange)
            }

            HStack(spacing: 16) {
                Button("Reject") {
                    onReject()
                }
                .buttonStyle(.bordered)

                Button("Approve") {
                    onApprove()
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .padding()
    }

    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}

struct DetailRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .fontWeight(.medium)
            Spacer()
            Text(value)
                .foregroundColor(.secondary)
        }
    }
}
