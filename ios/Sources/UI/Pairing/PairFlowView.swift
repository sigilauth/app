import SwiftUI

public struct PairFlowView: View {
    @StateObject private var viewModel: PairFlowViewModel
    @State private var serverURLText: String = ""
    @FocusState private var isServerURLFocused: Bool

    public init(
        networkService: NetworkService,
        pictogramDerivation: SessionPictogramDerivation,
        cryptoService: CryptoService,
        trustStorage: TrustStorageService
    ) {
        _viewModel = StateObject(wrappedValue: PairFlowViewModel(
            networkService: networkService,
            pictogramDerivation: pictogramDerivation,
            cryptoService: cryptoService,
            trustStorage: trustStorage
        ))
    }

    public var body: some View {
        NavigationView {
            ZStack {
                switch viewModel.state {
                case .idle:
                    serverURLInputView
                case .loading:
                    loadingView
                case .awaitingConfirmation(let pictogram, _, _, _, _, _):
                    SessionPictogramView(
                        pictogram: pictogram,
                        timeRemaining: viewModel.timeRemaining,
                        onConfirm: {
                            Task { await viewModel.confirmPair() }
                        },
                        onDeny: {
                            viewModel.denyPair()
                        }
                    )
                case .completing:
                    completingView
                case .awaitingAdminApproval(_, let serverId, _, _, let pollAttempts):
                    AwaitingAdminApprovalView(
                        serverId: serverId,
                        pollAttempts: pollAttempts
                    )
                case .paired(let server):
                    pairedView(server: server)
                case .denied:
                    deniedView
                case .error(let message):
                    errorView(message: message)
                }
            }
            .navigationTitle("Pair Device")
            .navigationBarTitleDisplayMode(.large)
        }
    }

    private var serverURLInputView: some View {
        VStack(spacing: 24) {
            VStack(alignment: .leading, spacing: 8) {
                Text("Enter Server URL")
                    .font(.title2)
                    .fontWeight(.semibold)

                Text("Enter the Sigil Auth server URL you want to pair with:")
                    .font(.body)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            VStack(spacing: 16) {
                TextField("https://sigil.example.com", text: $serverURLText)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
                    .textFieldStyle(.roundedBorder)
                    .focused($isServerURLFocused)

                Button("Start Pair") {
                    startPair()
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .disabled(serverURLText.isEmpty)
            }

            if let errorMessage = viewModel.errorMessage {
                Text(errorMessage)
                    .font(.caption)
                    .foregroundColor(.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding(24)
        .frame(maxWidth: 600)
    }

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
            Text("Initiating handshake...")
                .font(.body)
                .foregroundColor(.secondary)
        }
    }

    private var completingView: some View {
        VStack(spacing: 16) {
            ProgressView()
            Text("Completing pair...")
                .font(.body)
                .foregroundColor(.secondary)
        }
    }

    private func pairedView(server: TrustedServer) -> some View {
        VStack(spacing: 24) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 64))
                .foregroundColor(.green)

            VStack(spacing: 8) {
                Text("Pairing Successful")
                    .font(.title2)
                    .fontWeight(.semibold)

                Text("Your device is now paired with \(server.serverId)")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }

            Button("Done") {
                viewModel.reset()
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
        }
        .padding(24)
        .frame(maxWidth: 600)
    }

    private var deniedView: some View {
        VStack(spacing: 24) {
            Image(systemName: "xmark.circle.fill")
                .font(.system(size: 64))
                .foregroundColor(.orange)

            VStack(spacing: 8) {
                Text("Pairing Denied")
                    .font(.title2)
                    .fontWeight(.semibold)

                Text("You denied the pairing request or it expired.")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }

            Button("Try Again") {
                viewModel.reset()
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
        }
        .padding(24)
        .frame(maxWidth: 600)
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: 24) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 64))
                .foregroundColor(.red)

            VStack(spacing: 8) {
                Text("Error")
                    .font(.title2)
                    .fontWeight(.semibold)

                Text(message)
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }

            Button("Try Again") {
                viewModel.reset()
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
        }
        .padding(24)
        .frame(maxWidth: 600)
    }

    private func startPair() {
        isServerURLFocused = false

        guard var urlString = serverURLText.trimmingCharacters(in: .whitespacesAndNewlines),
              !urlString.isEmpty else {
            return
        }

        if !urlString.hasPrefix("http://") && !urlString.hasPrefix("https://") {
            urlString = "https://" + urlString
        }

        guard let url = URL(string: urlString) else {
            viewModel.errorMessage = "Invalid URL format"
            return
        }

        Task {
            await viewModel.startPair(serverURL: url)
        }
    }
}

#if DEBUG
struct PairFlowView_Previews: PreviewProvider {
    static var previews: some View {
        PairFlowView(
            networkService: DefaultNetworkService(),
            pictogramDerivation: DefaultSessionPictogramDerivation(
                argon2: DefaultArgon2Service(),
                pictogramPool: PictogramPool.shared
            ),
            cryptoService: CryptoServiceImpl(),
            trustStorage: KeychainTrustStorage()
        )
        .previewLayout(.sizeThatFits)
    }
}
#endif
