import Foundation
import Combine

@MainActor
public final class ImplementorInitViewModel: ObservableObject {
    @Published public var state: ImplementorInitState = .idle
    @Published public var errorMessage: String?
    @Published public var timeRemaining: Int = 0

    private let mnemonicGenerator: BIP39MnemonicGenerator
    private let eciesService: ECIESService
    private let cryptoService: CryptoService
    private let networkService: NetworkService
    private let keychainService: KeychainService
    private var timerTask: Task<Void, Never>?
    private var generatedMnemonic: [String]?

    // Configuration
    private var baseURL: URL?
    private var deviceFingerprint: String?
    private let deviceKeyLabel: String = "device-key"

    public init(
        mnemonicGenerator: BIP39MnemonicGenerator,
        eciesService: ECIESService,
        cryptoService: CryptoService,
        networkService: NetworkService,
        keychainService: KeychainService
    ) {
        self.mnemonicGenerator = mnemonicGenerator
        self.eciesService = eciesService
        self.cryptoService = cryptoService
        self.networkService = networkService
        self.keychainService = keychainService
    }

    public func configure(baseURL: URL, deviceFingerprint: String) {
        self.baseURL = baseURL
        self.deviceFingerprint = deviceFingerprint
    }

    // MARK: - Flow B: Claim Gate

    public func startClaimFlow() {
        state = .claimCodeEntry
    }

    public func submitClaimCode(_ code: String) async {
        state = .claiming
        errorMessage = nil

        do {
            guard let baseURL = baseURL else {
                throw NSError(domain: "ImplementorInit", code: 1, userInfo: [NSLocalizedDescriptionKey: "No server configured"])
            }

            guard let deviceFingerprint = deviceFingerprint else {
                throw NSError(domain: "ImplementorInit", code: 2, userInfo: [NSLocalizedDescriptionKey: "No device fingerprint available"])
            }

            // Parse input: could be full URL or just code
            let (requestId, normalizedCode) = try parseClaimInput(code)

            // Generate device signature: request_id || claim_code || timestamp
            let timestamp = Date().timeIntervalSince1970
            var signaturePayload = Data()
            signaturePayload.append(requestId.data(using: .utf8)!)
            signaturePayload.append(normalizedCode.data(using: .utf8)!)
            signaturePayload.append(withUnsafeBytes(of: timestamp.bitPattern) { Data($0) })

            let signature = try await keychainService.sign(
                signaturePayload,
                domain: .auth,
                with: deviceKeyLabel
            )

            let claimRequest = ClaimRequest(
                requestId: requestId,
                claimCode: normalizedCode,
                deviceFingerprint: deviceFingerprint,
                deviceSignature: signature.base64EncodedString()
            )

            let response = try await networkService.claimInitRequest(
                baseURL: baseURL,
                request: claimRequest
            )

            // Success - now wait for push
            state = .awaitingPush
            startAwaitingPushTimeout(duration: 60)

        } catch let error as NetworkError {
            let message = formatNetworkError(error)
            state = .claimError(message)
            errorMessage = error.localizedDescription
        } catch {
            state = .claimError("Failed to claim request. Please try again.")
            errorMessage = error.localizedDescription
        }
    }

    private func parseClaimInput(_ input: String) throws -> (requestId: String, code: String) {
        // Check if input is a URL
        if input.hasPrefix("http://") || input.hasPrefix("https://") {
            guard let url = URL(string: input),
                  let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
                  let requestId = components.queryItems?.first(where: { $0.name == "r" })?.value,
                  let code = components.queryItems?.first(where: { $0.name == "c" })?.value else {
                throw NSError(domain: "ImplementorInit", code: 3, userInfo: [NSLocalizedDescriptionKey: "Invalid claim URL format"])
            }
            return (requestId, ClaimCodeValidator.normalize(code))
        }

        // Just a code - need to error for now (UI should provide request_id separately)
        throw NSError(domain: "ImplementorInit", code: 4, userInfo: [NSLocalizedDescriptionKey: "Request ID required - please scan QR code"])
    }

    private func formatNetworkError(_ error: NetworkError) -> String {
        switch error {
        case .initClaimInvalidCode:
            return "Invalid claim code format"
        case .initRequestNotFound:
            return "Request not found or expired"
        case .initAlreadyClaimed:
            return "This request has already been claimed by another device"
        case .initCodeExpired:
            return "Claim code has expired (60 second limit)"
        case .initRateLimited:
            return "Too many attempts - request invalidated"
        case .noInternetConnection:
            return "No internet connection"
        case .timeout:
            return "Request timed out"
        default:
            return "Network error occurred"
        }
    }

    // MARK: - Flow A: Approval (Push-Triggered or Post-Claim)

    public func receiveInitRequest(_ request: ImplementorInitRequest) {
        state = .approvalRequested(request)
        startExpiryTimer(expiresAt: request.expiresAt)
    }

    public func approveRequest() async {
        guard case .approvalRequested(let request) = state else {
            return
        }

        // Biometric prompt happens in UI layer
        state = .generating

        do {
            let words = try mnemonicGenerator.generate()
            generatedMnemonic = words

            state = .displayingMnemonic(words: words, request: request)

        } catch {
            state = .error("Failed to generate mnemonic: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }

    public func rejectRequest() {
        state = .rejected
    }

    // MARK: - Mnemonic Display

    public func confirmWrittenDown() async {
        guard case .displayingMnemonic(let words, let request) = state else {
            return
        }

        state = .encryptingSubmitting
        errorMessage = nil

        do {
            guard let baseURL = baseURL else {
                throw NSError(domain: "ImplementorInit", code: 1, userInfo: [NSLocalizedDescriptionKey: "No server configured"])
            }

            guard let deviceFingerprint = deviceFingerprint else {
                throw NSError(domain: "ImplementorInit", code: 2, userInfo: [NSLocalizedDescriptionKey: "No device fingerprint available"])
            }

            // Encrypt mnemonic to implementor's ephemeral pubkey
            guard let implementorPubkey = Data(base64Encoded: request.implementorEphemeralPublicKey) else {
                throw NSError(domain: "ImplementorInit", code: 3, userInfo: [NSLocalizedDescriptionKey: "Invalid implementor public key"])
            }

            let mnemonicString = words.joined(separator: " ")
            let encryptedMnemonic = try eciesService.encrypt(
                plaintext: mnemonicString.data(using: .utf8)!,
                recipientPublicKey: implementorPubkey
            )

            // Generate device signature: request_id || action || timestamp
            let action = "approve"
            let timestamp = Date().timeIntervalSince1970
            var signaturePayload = Data()
            signaturePayload.append(request.requestId.data(using: .utf8)!)
            signaturePayload.append(action.data(using: .utf8)!)
            signaturePayload.append(withUnsafeBytes(of: timestamp.bitPattern) { Data($0) })

            let signature = try await keychainService.sign(
                signaturePayload,
                domain: .auth,
                with: deviceKeyLabel
            )

            let respondRequest = InitRespondRequest(
                requestId: request.requestId,
                deviceFingerprint: deviceFingerprint,
                action: action,
                encryptedMnemonic: encryptedMnemonic.base64EncodedString(),
                rejectionReason: nil,
                deviceSignature: signature.base64EncodedString()
            )

            let response = try await networkService.respondToInitRequest(
                baseURL: baseURL,
                request: respondRequest
            )

            // Clear mnemonic from memory
            generatedMnemonic = nil

            state = .awaitingConfirmation

        } catch let error as NetworkError {
            let message = formatInitRespondError(error)
            state = .error(message)
            errorMessage = error.localizedDescription
        } catch {
            state = .error("Failed to submit mnemonic: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }

    private func formatInitRespondError(_ error: NetworkError) -> String {
        switch error {
        case .initInvalidMnemonic:
            return "Invalid mnemonic format"
        case .initRequestNotFound:
            return "Request not found or expired"
        case .initAlreadyResponded:
            return "You have already responded to this request"
        case .initAlreadyApproved:
            return "Another device has already approved this request"
        case .initRequestExpired:
            return "Request has expired"
        case .noInternetConnection:
            return "No internet connection"
        case .timeout:
            return "Request timed out"
        default:
            return "Network error occurred"
        }
    }

    public func copyMnemonicToClipboard() {
        guard case .displayingMnemonic(let words, _) = state else {
            return
        }

        let mnemonicString = words.joined(separator: " ")
        #if os(iOS)
        UIPasteboard.general.string = mnemonicString
        #endif

        // Auto-clear after 60 seconds
        Task {
            try? await Task.sleep(for: .seconds(60))
            #if os(iOS)
            if UIPasteboard.general.string == mnemonicString {
                UIPasteboard.general.string = ""
            }
            #endif
        }
    }

    // MARK: - Confirmation

    public func receiveImplementorConfirmation() {
        state = .confirmed
    }

    // MARK: - Timers

    private func startExpiryTimer(expiresAt: Date) {
        stopTimer()

        timerTask = Task { @MainActor in
            while !Task.isCancelled {
                let remaining = Int(expiresAt.timeIntervalSinceNow)
                timeRemaining = max(0, remaining)

                if remaining <= 0 {
                    state = .expired
                    break
                }

                try? await Task.sleep(for: .seconds(1))
            }
        }
    }

    private func startAwaitingPushTimeout(duration: Int) {
        stopTimer()
        timeRemaining = duration

        timerTask = Task { @MainActor in
            while !Task.isCancelled && timeRemaining > 0 {
                timeRemaining -= 1
                try? await Task.sleep(for: .seconds(1))
            }

            if timeRemaining == 0 {
                state = .expired
            }
        }
    }

    private func stopTimer() {
        timerTask?.cancel()
        timerTask = nil
    }

    public func reset() {
        stopTimer()
        state = .idle
        errorMessage = nil
        timeRemaining = 0
        generatedMnemonic = nil
    }

    deinit {
        stopTimer()
    }
}

public enum ImplementorInitState: Equatable {
    case idle

    // Flow B: Claim Gate
    case claimCodeEntry
    case claiming
    case awaitingPush
    case claimError(String)

    // Flow A: Approval (both flows converge here)
    case approvalRequested(ImplementorInitRequest)
    case generating
    case displayingMnemonic(words: [String], request: ImplementorInitRequest)
    case encryptingSubmitting
    case awaitingConfirmation
    case confirmed

    // Terminal states
    case rejected
    case expired
    case error(String)

    public static func == (lhs: ImplementorInitState, rhs: ImplementorInitState) -> Bool {
        switch (lhs, rhs) {
        case (.idle, .idle),
             (.claimCodeEntry, .claimCodeEntry),
             (.claiming, .claiming),
             (.awaitingPush, .awaitingPush),
             (.generating, .generating),
             (.encryptingSubmitting, .encryptingSubmitting),
             (.awaitingConfirmation, .awaitingConfirmation),
             (.confirmed, .confirmed),
             (.rejected, .rejected),
             (.expired, .expired):
            return true
        case (.claimError(let lhsMsg), .claimError(let rhsMsg)),
             (.error(let lhsMsg), .error(let rhsMsg)):
            return lhsMsg == rhsMsg
        case (.approvalRequested(let lhsReq), .approvalRequested(let rhsReq)):
            return lhsReq.requestId == rhsReq.requestId
        case (.displayingMnemonic(let lhsWords, let lhsReq), .displayingMnemonic(let rhsWords, let rhsReq)):
            return lhsWords == rhsWords && lhsReq.requestId == rhsReq.requestId
        default:
            return false
        }
    }
}
