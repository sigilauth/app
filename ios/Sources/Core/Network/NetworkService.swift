import Foundation

/// Network service for Sigil Auth API operations
/// Per OpenAPI spec: /api/openapi.yaml
protocol NetworkService: Sendable {
    /// Fetch server information (no authentication required)
    /// GET /info
    func fetchServerInfo(baseURL: URL) async throws -> ServerInfo

    /// Submit challenge response with device signature
    /// POST /respond (self-authenticated via signature)
    func respondToChallenge(
        baseURL: URL,
        response: ChallengeResponse
    ) async throws -> ChallengeVerified

    /// Redeem 8-digit pairing code
    /// POST /pairing/redeem (integrator endpoint)
    func redeemPairingCode(
        _ code: String,
        pairingURL: URL
    ) async throws -> PairingPayload

    /// Initiate pair handshake (plaintext)
    /// GET /pair/init?client_pub=<base64>
    func initiatePair(
        baseURL: URL,
        clientPublicKey: Data
    ) async throws -> PairInitResponse

    /// Complete pair handshake (plaintext)
    /// POST /pair/complete
    func completePair(
        baseURL: URL,
        request: PairCompleteRequest
    ) async throws -> PairCompleteResponse

    /// Claim init request with code (Path B)
    /// POST /device/init/claim
    func claimInitRequest(
        baseURL: URL,
        request: ClaimRequest
    ) async throws -> ClaimResponse

    /// Respond to init request (approve/reject)
    /// POST /device/init/respond
    func respondToInitRequest(
        baseURL: URL,
        request: InitRespondRequest
    ) async throws -> InitRespondResponse
}

/// URLSession-based implementation of NetworkService
final class DefaultNetworkService: NetworkService, @unchecked Sendable {
    private let session: URLSession
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder

    init(session: URLSession = .shared) {
        self.session = session

        self.decoder = JSONDecoder()
        self.decoder.dateDecodingStrategy = .iso8601

        self.encoder = JSONEncoder()
        self.encoder.dateEncodingStrategy = .iso8601
    }

    func fetchServerInfo(baseURL: URL) async throws -> ServerInfo {
        let url = baseURL.appendingPathComponent("/info")

        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let (data, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.invalidResponse
        }

        guard (200..<300).contains(httpResponse.statusCode) else {
            let apiError = try? decoder.decode(APIError.self, from: data)
            throw NetworkError.serverError(
                statusCode: httpResponse.statusCode,
                message: apiError?.error.message
            )
        }

        do {
            return try decoder.decode(ServerInfo.self, from: data)
        } catch {
            throw NetworkError.decodingError(error.localizedDescription)
        }
    }

    func respondToChallenge(
        baseURL: URL,
        response: ChallengeResponse
    ) async throws -> ChallengeVerified {
        let url = baseURL.appendingPathComponent("/respond")

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        request.httpBody = try encoder.encode(response)

        let (data, httpResponse) = try await session.data(for: request)

        guard let httpResponse = httpResponse as? HTTPURLResponse else {
            throw NetworkError.invalidResponse
        }

        guard (200..<300).contains(httpResponse.statusCode) else {
            if let apiError = try? decoder.decode(APIError.self, from: data) {
                switch apiError.error.code {
                case "INVALID_SIGNATURE":
                    throw NetworkError.signatureVerificationFailed
                case "FINGERPRINT_MISMATCH":
                    throw NetworkError.fingerprintMismatch
                case "CHALLENGE_NOT_FOUND":
                    throw NetworkError.challengeNotFound
                default:
                    throw NetworkError.serverError(
                        statusCode: httpResponse.statusCode,
                        message: apiError.error.message
                    )
                }
            }
            throw NetworkError.serverError(statusCode: httpResponse.statusCode, message: nil)
        }

        do {
            return try decoder.decode(ChallengeVerified.self, from: data)
        } catch {
            throw NetworkError.decodingError(error.localizedDescription)
        }
    }

    func redeemPairingCode(
        _ code: String,
        pairingURL: URL
    ) async throws -> PairingPayload {
        let url = pairingURL.appendingPathComponent("/pairing/redeem")

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let redeemRequest = PairingCodeRedeemRequest(pairingCode: code)
        request.httpBody = try encoder.encode(redeemRequest)

        let (data, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.invalidResponse
        }

        guard (200..<300).contains(httpResponse.statusCode) else {
            switch httpResponse.statusCode {
            case 400:
                throw NetworkError.pairingCodeInvalid
            case 404:
                throw NetworkError.pairingCodeNotFound
            case 429:
                throw NetworkError.pairingCodeTooManyAttempts
            default:
                let apiError = try? decoder.decode(APIError.self, from: data)
                throw NetworkError.serverError(
                    statusCode: httpResponse.statusCode,
                    message: apiError?.error.message
                )
            }
        }

        do {
            return try decoder.decode(PairingPayload.self, from: data)
        } catch {
            throw NetworkError.decodingError(error.localizedDescription)
        }
    }

    func initiatePair(
        baseURL: URL,
        clientPublicKey: Data
    ) async throws -> PairInitResponse {
        var components = URLComponents(url: baseURL.appendingPathComponent("/pair/init"), resolvingAgainstBaseURL: true)
        components?.queryItems = [
            URLQueryItem(name: "client_pub", value: clientPublicKey.base64EncodedString())
        ]

        guard let url = components?.url else {
            throw NetworkError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let (data, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.invalidResponse
        }

        guard (200..<300).contains(httpResponse.statusCode) else {
            let apiError = try? decoder.decode(APIError.self, from: data)
            throw NetworkError.serverError(
                statusCode: httpResponse.statusCode,
                message: apiError?.error.message
            )
        }

        do {
            return try decoder.decode(PairInitResponse.self, from: data)
        } catch {
            throw NetworkError.decodingError(error.localizedDescription)
        }
    }

    func completePair(
        baseURL: URL,
        request: PairCompleteRequest
    ) async throws -> PairCompleteResponse {
        let url = baseURL.appendingPathComponent("/pair/complete")

        var httpRequest = URLRequest(url: url)
        httpRequest.httpMethod = "POST"
        httpRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        httpRequest.setValue("application/json", forHTTPHeaderField: "Accept")

        httpRequest.httpBody = try encoder.encode(request)

        let (data, response) = try await session.data(for: httpRequest)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.invalidResponse
        }

        guard (200..<300).contains(httpResponse.statusCode) else {
            if let apiError = try? decoder.decode(APIError.self, from: data) {
                switch apiError.error.code {
                case "HANDSHAKE_EXPIRED":
                    throw NetworkError.pairHandshakeExpired
                case "NOT_APPROVED":
                    throw NetworkError.pairHandshakeNotApproved
                case "NONCE_CONSUMED":
                    throw NetworkError.pairNonceConsumed
                default:
                    throw NetworkError.serverError(
                        statusCode: httpResponse.statusCode,
                        message: apiError.error.message
                    )
                }
            }
            throw NetworkError.serverError(statusCode: httpResponse.statusCode, message: nil)
        }

        do {
            return try decoder.decode(PairCompleteResponse.self, from: data)
        } catch {
            throw NetworkError.decodingError(error.localizedDescription)
        }
    }

    func claimInitRequest(
        baseURL: URL,
        request: ClaimRequest
    ) async throws -> ClaimResponse {
        let url = baseURL.appendingPathComponent("/device/init/claim")

        var httpRequest = URLRequest(url: url)
        httpRequest.httpMethod = "POST"
        httpRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        httpRequest.setValue("application/json", forHTTPHeaderField: "Accept")

        httpRequest.httpBody = try encoder.encode(request)

        let (data, response) = try await session.data(for: httpRequest)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.invalidResponse
        }

        guard (200..<300).contains(httpResponse.statusCode) else {
            if let apiError = try? decoder.decode(APIError.self, from: data) {
                switch apiError.error.code {
                case "INVALID_CODE":
                    throw NetworkError.initClaimInvalidCode
                case "REQUEST_NOT_FOUND":
                    throw NetworkError.initRequestNotFound
                case "ALREADY_CLAIMED":
                    throw NetworkError.initAlreadyClaimed
                case "CODE_EXPIRED":
                    throw NetworkError.initCodeExpired
                case "RATE_LIMIT":
                    throw NetworkError.initRateLimited
                default:
                    throw NetworkError.serverError(
                        statusCode: httpResponse.statusCode,
                        message: apiError.error.message
                    )
                }
            }
            throw NetworkError.serverError(statusCode: httpResponse.statusCode, message: nil)
        }

        do {
            return try decoder.decode(ClaimResponse.self, from: data)
        } catch {
            throw NetworkError.decodingError(error.localizedDescription)
        }
    }

    func respondToInitRequest(
        baseURL: URL,
        request: InitRespondRequest
    ) async throws -> InitRespondResponse {
        let url = baseURL.appendingPathComponent("/device/init/respond")

        var httpRequest = URLRequest(url: url)
        httpRequest.httpMethod = "POST"
        httpRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        httpRequest.setValue("application/json", forHTTPHeaderField: "Accept")

        httpRequest.httpBody = try encoder.encode(request)

        let (data, response) = try await session.data(for: httpRequest)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.invalidResponse
        }

        guard (200..<300).contains(httpResponse.statusCode) else {
            if let apiError = try? decoder.decode(APIError.self, from: data) {
                switch apiError.error.code {
                case "INVALID_MNEMONIC":
                    throw NetworkError.initInvalidMnemonic
                case "REQUEST_NOT_FOUND":
                    throw NetworkError.initRequestNotFound
                case "ALREADY_RESPONDED":
                    throw NetworkError.initAlreadyResponded
                case "ALREADY_APPROVED":
                    throw NetworkError.initAlreadyApproved
                case "REQUEST_EXPIRED":
                    throw NetworkError.initRequestExpired
                default:
                    throw NetworkError.serverError(
                        statusCode: httpResponse.statusCode,
                        message: apiError.error.message
                    )
                }
            }
            throw NetworkError.serverError(statusCode: httpResponse.statusCode, message: nil)
        }

        do {
            return try decoder.decode(InitRespondResponse.self, from: data)
        } catch {
            throw NetworkError.decodingError(error.localizedDescription)
        }
    }
}
