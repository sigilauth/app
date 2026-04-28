import Foundation
import Combine
import CryptoKit

@MainActor
public final class PairFlowViewModel: ObservableObject {
    @Published public var state: PairState = .idle
    @Published public var errorMessage: String?
    @Published public var timeRemaining: Int = 10

    private let networkService: NetworkService
    private let pictogramDerivation: SessionPictogramDerivation
    private let cryptoService: CryptoService
    private let trustStorage: TrustStorageService
    private var timerTask: Task<Void, Never>?
    private var expiryDate: Date?

    public init(
        networkService: NetworkService,
        pictogramDerivation: SessionPictogramDerivation,
        cryptoService: CryptoService,
        trustStorage: TrustStorageService
    ) {
        self.networkService = networkService
        self.pictogramDerivation = pictogramDerivation
        self.cryptoService = cryptoService
        self.trustStorage = trustStorage
    }

    public func startPair(serverURL: URL) async {
        state = .loading
        errorMessage = nil

        do {
            let deviceKeyPair = try await cryptoService.getDeviceKeyPair()
            let clientPublicKey = deviceKeyPair.publicKey

            let initResponse = try await networkService.initiatePair(
                baseURL: serverURL,
                clientPublicKey: clientPublicKey
            )

            guard let serverPublicKey = Data(base64Encoded: initResponse.serverPublicKey),
                  let serverNonce = Data(base64Encoded: initResponse.serverNonce) else {
                throw NetworkError.decodingError("Invalid base64 in server response")
            }

            let derivedPictogram = try pictogramDerivation.derive(
                serverPublicKey: serverPublicKey,
                clientPublicKey: clientPublicKey,
                serverNonce: serverNonce
            )

            let serverPictogramWords = initResponse.sessionPictogram

            guard derivedPictogram.names == serverPictogramWords else {
                throw NetworkError.pictogramMismatch
            }

            expiryDate = initResponse.expiresAt
            startTimer()

            state = .awaitingConfirmation(
                pictogram: derivedPictogram,
                serverURL: serverURL,
                serverId: initResponse.serverId,
                serverPublicKey: serverPublicKey,
                serverNonce: initResponse.serverNonce,
                clientPublicKey: clientPublicKey
            )

        } catch {
            state = .error(error.localizedDescription)
            errorMessage = error.localizedDescription
        }
    }

    public func confirmPair() async {
        guard case .awaitingConfirmation(
            let pictogram,
            let serverURL,
            let serverId,
            let serverPublicKey,
            let serverNonce,
            let clientPublicKey
        ) = state else {
            return
        }

        state = .completing
        stopTimer()

        do {
            let deviceName = await UIDevice.current.name
            let osVersion = await UIDevice.current.systemVersion

            let completeRequest = PairCompleteRequest(
                serverNonce: serverNonce,
                clientPublicKey: clientPublicKey.base64EncodedString(),
                deviceInfo: .init(
                    name: String(deviceName.prefix(64)),
                    platform: "ios",
                    osVersion: osVersion
                )
            )

            // Try completion first - if admin pre-approved, succeeds immediately
            var response: PairCompleteResponse
            do {
                response = try await networkService.completePair(
                    baseURL: serverURL,
                    request: completeRequest
                )
            } catch NetworkError.pairHandshakeNotApproved {
                // Needs admin approval - start polling
                state = .awaitingAdminApproval(
                    serverURL: serverURL,
                    serverId: serverId,
                    serverPublicKey: serverPublicKey,
                    completeRequest: completeRequest,
                    pollAttempts: 0
                )

                // Poll every 2 seconds for up to 5 minutes (150 attempts)
                var attempts = 0
                let maxAttempts = 150

                while attempts < maxAttempts {
                    try await Task.sleep(for: .seconds(2))
                    attempts += 1

                    // Update state with attempt count
                    state = .awaitingAdminApproval(
                        serverURL: serverURL,
                        serverId: serverId,
                        serverPublicKey: serverPublicKey,
                        completeRequest: completeRequest,
                        pollAttempts: attempts
                    )

                    do {
                        response = try await networkService.completePair(
                            baseURL: serverURL,
                            request: completeRequest
                        )
                        // Success! Break out of poll loop
                        break
                    } catch NetworkError.pairHandshakeNotApproved {
                        // Still not approved, continue polling
                        if attempts >= maxAttempts {
                            throw NetworkError.pairHandshakeExpired
                        }
                        continue
                    } catch {
                        // Other error - fail immediately
                        throw error
                    }
                }
            }

            let serverFingerprint = Data(SHA256.hash(data: serverPublicKey))
                .map { String(format: "%02x", $0) }
                .joined()

            let trustedServer = TrustedServer(
                serverUrl: serverURL,
                serverId: serverId,
                serverPublicKey: serverPublicKey.base64EncodedString(),
                serverFingerprint: serverFingerprint,
                pairedAt: response.pairedAt
            )

            try trustStorage.saveTrustedServer(trustedServer)

            state = .paired(trustedServer)

        } catch {
            state = .error(error.localizedDescription)
            errorMessage = error.localizedDescription
        }
    }

    public func denyPair() {
        stopTimer()
        state = .denied
    }

    public func reset() {
        stopTimer()
        state = .idle
        errorMessage = nil
        timeRemaining = 10
    }

    private func startTimer() {
        stopTimer()

        timerTask = Task { @MainActor in
            while !Task.isCancelled {
                guard let expiryDate = expiryDate else { break }

                let remaining = Int(expiryDate.timeIntervalSinceNow)
                timeRemaining = max(0, remaining)

                if remaining <= 0 {
                    denyPair()
                    break
                }

                try? await Task.sleep(for: .seconds(1))
            }
        }
    }

    private func stopTimer() {
        timerTask?.cancel()
        timerTask = nil
    }

    deinit {
        stopTimer()
    }
}

public enum PairState: Equatable {
    case idle
    case loading
    case awaitingConfirmation(
        pictogram: SessionPictogram,
        serverURL: URL,
        serverId: String,
        serverPublicKey: Data,
        serverNonce: String,
        clientPublicKey: Data
    )
    case completing
    case awaitingAdminApproval(
        serverURL: URL,
        serverId: String,
        serverPublicKey: Data,
        completeRequest: PairCompleteRequest,
        pollAttempts: Int
    )
    case paired(TrustedServer)
    case denied
    case error(String)

    public static func == (lhs: PairState, rhs: PairState) -> Bool {
        switch (lhs, rhs) {
        case (.idle, .idle),
             (.loading, .loading),
             (.completing, .completing),
             (.denied, .denied):
            return true
        case (.awaitingConfirmation(let lhsPictogram, _, _, _, _, _),
              .awaitingConfirmation(let rhsPictogram, _, _, _, _, _)):
            return lhsPictogram == rhsPictogram
        case (.awaitingAdminApproval(_, let lhsServerId, _, _, let lhsAttempts),
              .awaitingAdminApproval(_, let rhsServerId, _, _, let rhsAttempts)):
            return lhsServerId == rhsServerId && lhsAttempts == rhsAttempts
        case (.paired(let lhsServer), .paired(let rhsServer)):
            return lhsServer == rhsServer
        case (.error(let lhsMsg), .error(let rhsMsg)):
            return lhsMsg == rhsMsg
        default:
            return false
        }
    }
}
