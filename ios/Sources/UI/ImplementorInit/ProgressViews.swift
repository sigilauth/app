import SwiftUI

// Generating progress
public struct GeneratingView: View {
    public init() {}

    public var body: some View {
        VStack(spacing: 24) {
            ProgressView()
                .controlSize(.large)

            Text("Generating Mnemonic...")
                .font(.title3)
                .fontWeight(.medium)

            Text("Creating a secure 24-word recovery phrase")
                .font(.body)
                .foregroundColor(.secondary)
        }
        .padding()
    }
}

// Encrypting & Submitting
public struct EncryptingSubmittingView: View {
    public init() {}

    public var body: some View {
        VStack(spacing: 24) {
            ProgressView()
                .controlSize(.large)

            Text("Encrypting & Submitting...")
                .font(.title3)
                .fontWeight(.medium)

            Text("Securing mnemonic for implementor")
                .font(.body)
                .foregroundColor(.secondary)
        }
        .padding()
    }
}

// Awaiting Confirmation
public struct AwaitingConfirmationView: View {
    public init() {}

    public var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 64))
                .foregroundColor(.green)

            Text("Submitted")
                .font(.title2)
                .fontWeight(.semibold)

            Text("Waiting for implementor to confirm receipt...")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            ProgressView()
        }
        .padding()
    }
}

// Claiming
public struct ClaimingView: View {
    public init() {}

    public var body: some View {
        VStack(spacing: 24) {
            ProgressView()
                .controlSize(.large)

            Text("Claiming Request...")
                .font(.title3)
                .fontWeight(.medium)

            Text("Verifying claim code")
                .font(.body)
                .foregroundColor(.secondary)
        }
        .padding()
    }
}

// Awaiting Push (after successful claim)
public struct AwaitingPushView: View {
    let timeRemaining: Int

    public init(timeRemaining: Int) {
        self.timeRemaining = timeRemaining
    }

    public var body: some View {
        VStack(spacing: 24) {
            ProgressView()
                .controlSize(.large)

            Text("Waiting for Request...")
                .font(.title3)
                .fontWeight(.medium)

            Text("Your claim was successful. Waiting for the mnemonic generation request.")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            if timeRemaining < 30 {
                HStack {
                    Image(systemName: "clock")
                    Text("Timeout in \(timeRemaining)s")
                }
                .font(.caption)
                .foregroundColor(.secondary)
            }
        }
        .padding()
    }
}
