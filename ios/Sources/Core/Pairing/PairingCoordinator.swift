import Foundation

/// Coordinates all 4 pairing transports per AC #4
/// Transports: QR code, 8-digit code, Universal Link, manual entry
/// All converge on pictogram verification screen
@available(iOS 16.0, *)
public final class PairingCoordinator: ObservableObject {

    @Published public var state: PairingState = .idle
    @Published public var serverInfo: ServerInfo?
    @Published public var error: PairingError?

    private let networkService: NetworkService
    private let keychainService: KeychainService
    private let attestationService: AttestationService
    private let storageService: ServerConfigStorage

    public init() {
        self.networkService = DefaultNetworkService()
        self.keychainService = DefaultKeychainService()
        self.attestationService = DefaultAttestationService()
        self.storageService = DefaultServerConfigStorage()
    }

    init(
        networkService: NetworkService,
        keychainService: KeychainService,
        attestationService: AttestationService,
        storageService: ServerConfigStorage
    ) {
        self.networkService = networkService
        self.keychainService = keychainService
        self.attestationService = attestationService
        self.storageService = storageService
    }

    // MARK: - Transport 1: QR Code

    /// Parse QR code and fetch server info
    /// QR format: sigil://pair?url=https://sigil.example.com
    func handleQRCode(_ code: String) async {
        await handlePairingURL(code)
    }

    // MARK: - Transport 2: 8-Digit Code

    /// Redeem 8-digit pairing code
    /// Per product-spec §3.7: POST /pairing/redeem with constant-time comparison
    /// Code format: 12345678 (8 ASCII digits)
    public func redeemPairingCode(
        _ code: String,
        pairingURL: URL = URL(string: "https://relay.sigilauth.com")!
    ) async {
        guard code.count == 8, code.allSatisfy({ $0.isNumber }) else {
            await MainActor.run {
                self.state = .failed
                self.error = .invalidCode
            }
            return
        }

        await MainActor.run { self.state = .fetchingServerInfo }

        do {
            let payload = try await networkService.redeemPairingCode(code, pairingURL: pairingURL)

            let serverInfo = ServerInfo(
                serverId: payload.serverUrl,
                serverName: payload.serverName,
                serverPublicKey: payload.serverPublicKey,
                serverPictogram: payload.serverPictogram,
                serverPictogramSpeakable: payload.serverPictogramSpeakable,
                version: "1.0.0",
                mode: "operational",
                relayUrl: nil,
                features: ServerInfo.Features(
                    mpa: true,
                    secureDecrypt: true,
                    mnemonicGeneration: true,
                    webhooks: true
                )
            )

            await MainActor.run {
                self.serverInfo = serverInfo
                self.state = .verifyingPictogram
            }
        } catch let error as NetworkError {
            await MainActor.run {
                self.state = .failed
                switch error {
                case .pairingCodeInvalid:
                    self.error = .invalidCode
                case .pairingCodeNotFound:
                    self.error = .codeExpiredOrNotFound
                case .pairingCodeTooManyAttempts:
                    self.error = .tooManyAttempts
                default:
                    self.error = .networkError(error)
                }
            }
        } catch {
            await MainActor.run {
                self.state = .failed
                self.error = .networkError(error)
            }
        }
    }

    // MARK: - Transport 3: Universal Link

    /// Handle Universal Link / Deep Link
    /// Format: https://sigil.example.com/pair or sigil://pair?url=...
    func handleUniversalLink(_ url: URL) async {
        await handlePairingURL(url.absoluteString)
    }

    // MARK: - Transport 4: Manual Entry

    /// Manually entered server URL
    func handleManualEntry(serverURL: String) async {
        guard let url = URL(string: serverURL) else {
            await MainActor.run {
                self.state = .failed
                self.error = .invalidURL
            }
            return
        }

        await fetchServerInfo(baseURL: url)
    }

    // MARK: - Common Flow

    private func handlePairingURL(_ urlString: String) async {
        // Parse sigil:// URL or https:// URL
        guard let url = URL(string: urlString) else {
            await MainActor.run {
                self.state = .failed
                self.error = .invalidURL
            }
            return
        }

        // Extract base URL
        var baseURL: URL?

        if url.scheme == "sigil" {
            // Format: sigil://pair?url=https://sigil.example.com
            if let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
               let urlParam = components.queryItems?.first(where: { $0.name == "url" })?.value,
               let extractedURL = URL(string: urlParam) {
                baseURL = extractedURL
            }
        } else if url.scheme == "https" {
            // Direct HTTPS URL
            baseURL = url
        }

        guard let validURL = baseURL else {
            await MainActor.run {
                self.state = .failed
                self.error = .invalidURL
            }
            return
        }

        await fetchServerInfo(baseURL: validURL)
    }

    private func fetchServerInfo(baseURL: URL) async {
        await MainActor.run { self.state = .fetchingServerInfo }

        do {
            let info = try await networkService.fetchServerInfo(baseURL: baseURL)

            await MainActor.run {
                self.serverInfo = info
                self.state = .verifyingPictogram
            }
        } catch {
            await MainActor.run {
                self.state = .failed
                self.error = .networkError(error)
            }
        }
    }

    /// Complete pairing after pictogram verification
    func completePairing(confirmed: Bool) async {
        guard confirmed, let serverInfo = serverInfo else {
            await MainActor.run {
                self.state = .failed
                self.error = .userRejected
            }
            return
        }

        await MainActor.run { self.state = .generatingKeys }

        do {
            // Generate device keypair in Secure Enclave
            let keyLabel = "device-key-\(serverInfo.serverId)"
            _ = try await keychainService.generateDeviceKeypair(label: keyLabel)

            // Save server configuration
            let config = ServerConfig(
                serverId: serverInfo.serverId,
                serverName: serverInfo.serverName,
                serverURL: URL(string: serverInfo.serverId)!,  // TODO: Extract actual URL
                serverPublicKey: Data(base64Encoded: serverInfo.serverPublicKey) ?? Data(),
                pictogram: serverInfo.serverPictogram,
                pictogramSpeakable: serverInfo.serverPictogramSpeakable,
                deviceKeyLabel: keyLabel,
                registeredAt: Date()
            )

            try await storageService.saveServerConfig(config)

            await MainActor.run {
                self.state = .completed
            }
        } catch {
            await MainActor.run {
                self.state = .failed
                self.error = .keyGenerationFailed(error)
            }
        }
    }
}

// MARK: - State

public enum PairingState {
    case idle
    case fetchingServerInfo
    case verifyingPictogram
    case generatingKeys
    case completed
    case failed
}

public enum PairingError: Error, LocalizedError {
    case invalidURL
    case invalidCode
    case codeExpiredOrNotFound
    case tooManyAttempts
    case networkError(Error)
    case userRejected
    case keyGenerationFailed(Error)
    case notImplemented(String)

    public var errorDescription: String? {
        switch self {
        case .invalidURL:
            return LocalizationService.shared.string(for: "errors.invalid-url")
        case .invalidCode:
            return LocalizationService.shared.string(for: "auth.pairing-code-error-invalid-format")
        case .codeExpiredOrNotFound:
            return LocalizationService.shared.string(for: "auth.pairing-code-error-not-found")
        case .tooManyAttempts:
            return LocalizationService.shared.string(for: "auth.pairing-code-error-too-many-attempts")
        case .networkError:
            return LocalizationService.shared.string(for: "auth.pairing-code-error-network")
        case .userRejected:
            return LocalizationService.shared.string(for: "errors.user-cancelled")
        case .keyGenerationFailed(let error):
            return "Failed to generate device key: \(error.localizedDescription)"
        case .notImplemented(let feature):
            return "Not implemented: \(feature)"
        }
    }
}
