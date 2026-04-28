#if DEBUG
import SwiftUI
import SigilAuthCore

/// Debug-only view for simulating push notifications without relay
/// Per task: "write a test mode that lets QA simulate a push without needing the real relay"
@available(iOS 16.0, *)
struct PushTestView: View {
    @State private var jsonPayload: String = """
{
  "challenge_id": "ch_test_\(UUID().uuidString.prefix(8))",
  "server_name": "test.sigilauth.com",
  "server_fingerprint": "fp_\(UUID().uuidString.prefix(12))",
  "action": "login",
  "action_description": "Sign in to Dashboard",
  "metadata": {
    "ip": "192.168.1.100",
    "user_agent": "Mozilla/5.0",
    "location": "Brisbane, AU"
  },
  "expires_at": "\(ISO8601DateFormatter().string(from: Date().addingTimeInterval(300)))"
}
"""
    @State private var showApproval = false
    @State private var parsedChallenge: TestChallenge?
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            VStack(spacing: .s4) {
                Text("Paste JSON challenge payload below")
                    .font(.caption)
                    .foregroundColor(.sigilTextMuted)
                    .padding(.top)

                TextEditor(text: $jsonPayload)
                    .font(.system(.body, design: .monospaced))
                    .frame(minHeight: 300)
                    .padding(8)
                    .background(Color.sigilSurface)
                    .cornerRadius(.rMd)
                    .overlay(
                        RoundedRectangle(cornerRadius: .rMd)
                            .stroke(Color.sigilBorder, lineWidth: 1)
                    )
                    .padding(.horizontal)

                if let error = errorMessage {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.sigilDanger)
                        .padding(.horizontal)
                }

                Button(action: simulatePush) {
                    Text("Simulate Push")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.sigilPrimary)
                        .cornerRadius(.rMd)
                }
                .padding(.horizontal)

                Spacer()
            }
            .navigationTitle("Push Test")
            .navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $showApproval) {
                if let challenge = parsedChallenge {
                    TestApprovalView(challenge: challenge)
                }
            }
        }
    }

    private func simulatePush() {
        errorMessage = nil

        guard let data = jsonPayload.data(using: .utf8) else {
            errorMessage = "Invalid UTF-8"
            return
        }

        do {
            let decoded = try JSONDecoder().decode(TestChallenge.self, from: data)
            parsedChallenge = decoded
            showApproval = true
        } catch {
            errorMessage = "JSON parse error: \(error.localizedDescription)"
        }
    }
}

/// Test-only challenge structure
struct TestChallenge: Codable {
    let challenge_id: String
    let server_name: String
    let server_fingerprint: String
    let action: String
    let action_description: String
    let metadata: [String: String]
    let expires_at: String
}

/// Test approval screen (simplified version of ApprovalView)
@available(iOS 16.0, *)
struct TestApprovalView: View {
    let challenge: TestChallenge
    @Environment(\.dismiss) var dismiss
    @State private var result: String = ""

    var body: some View {
        VStack(spacing: .s6) {
            Text("Test Approval")
                .font(.title2)
                .foregroundColor(.sigilText)

            VStack(alignment: .leading, spacing: .s2) {
                Text("Server")
                    .font(.caption)
                    .foregroundColor(.sigilTextDim)
                Text(challenge.server_name)
                    .font(.body)
                    .foregroundColor(.sigilText)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .background(Color.sigilSurface)
            .cornerRadius(.rMd)

            VStack(alignment: .leading, spacing: .s2) {
                Text("Action")
                    .font(.caption)
                    .foregroundColor(.sigilTextDim)
                Text(challenge.action_description)
                    .font(.body)
                    .foregroundColor(.sigilText)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .background(Color.sigilSurface)
            .cornerRadius(.rMd)

            if !challenge.metadata.isEmpty {
                VStack(alignment: .leading, spacing: .s2) {
                    Text("Metadata")
                        .font(.caption)
                        .foregroundColor(.sigilTextDim)
                    ForEach(Array(challenge.metadata.sorted(by: { $0.key < $1.key })), id: \.key) { key, value in
                        HStack {
                            Text(key)
                                .font(.caption)
                                .foregroundColor(.sigilTextMuted)
                            Spacer()
                            Text(value)
                                .font(.caption)
                                .foregroundColor(.sigilText)
                        }
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
                .background(Color.sigilSurface)
                .cornerRadius(.rMd)
            }

            if !result.isEmpty {
                Text(result)
                    .font(.caption)
                    .foregroundColor(.sigilSuccess)
                    .padding()
            }

            Spacer()

            HStack(spacing: .s4) {
                Button(action: {
                    result = "❌ Challenge denied"
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                        dismiss()
                    }
                }) {
                    Text("Deny")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.sigilDanger)
                        .cornerRadius(.rMd)
                }

                Button(action: {
                    result = "✅ Challenge approved (test mode - no crypto)"
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                        dismiss()
                    }
                }) {
                    Text("Approve")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.sigilSuccess)
                        .cornerRadius(.rMd)
                }
            }
        }
        .padding()
    }
}

#if DEBUG
#Preview {
    PushTestView()
}
#endif
#endif
