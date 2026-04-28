package com.wagmilabs.sigil.network

import com.wagmilabs.sigil.network.models.*

/**
 * Network service for Sigil Auth API operations
 * Per OpenAPI spec: /api/openapi.yaml
 */
interface NetworkService {
    /**
     * Fetch server information (no authentication required)
     * GET /info
     */
    suspend fun fetchServerInfo(baseURL: String): ServerInfo

    /**
     * Submit challenge response with device signature
     * POST /respond (self-authenticated via signature)
     */
    suspend fun respondToChallenge(
        baseURL: String,
        response: ChallengeResponse
    ): ChallengeVerified

    /**
     * Redeem 8-digit pairing code
     * POST /pairing/redeem (integrator endpoint)
     */
    suspend fun redeemPairingCode(
        code: String,
        pairingURL: String
    ): PairingPayload

    /**
     * Initiate pair handshake (plaintext)
     * GET /pair/init?client_pub=<base64>
     */
    suspend fun initiatePair(
        baseURL: String,
        clientPublicKey: ByteArray
    ): PairInitResponse

    /**
     * Complete pair handshake (plaintext)
     * POST /pair/complete
     */
    suspend fun completePair(
        baseURL: String,
        request: PairCompleteRequest
    ): PairCompleteResponse

    /**
     * Claim init request with code (Path B)
     * POST /device/init/claim
     */
    suspend fun claimInitRequest(
        baseURL: String,
        request: ClaimRequest
    ): ClaimResponse

    /**
     * Respond to init request (approve/reject)
     * POST /device/init/respond
     */
    suspend fun respondToInitRequest(
        baseURL: String,
        request: InitRespondRequest
    ): InitRespondResponse
}

/**
 * Network errors
 */
sealed class NetworkError : Exception() {
    object InvalidURL : NetworkError()
    object NoInternetConnection : NetworkError()
    object Timeout : NetworkError()
    data class ServerError(val statusCode: Int, val message: String?) : NetworkError()
    data class DecodingError(val details: String) : NetworkError()
    object SignatureVerificationFailed : NetworkError()
    object FingerprintMismatch : NetworkError()
    object ChallengeNotFound : NetworkError()
    object InvalidResponse : NetworkError()
    object PairingCodeInvalid : NetworkError()
    object PairingCodeNotFound : NetworkError()
    object PairingCodeTooManyAttempts : NetworkError()
    object PairHandshakeExpired : NetworkError()
    object PairHandshakeNotApproved : NetworkError()
    object PairNonceConsumed : NetworkError()
    object PictogramMismatch : NetworkError()
    object InitClaimInvalidCode : NetworkError()
    object InitRequestNotFound : NetworkError()
    object InitAlreadyClaimed : NetworkError()
    object InitCodeExpired : NetworkError()
    object InitRateLimited : NetworkError()
    object InitInvalidMnemonic : NetworkError()
    object InitAlreadyResponded : NetworkError()
    object InitAlreadyApproved : NetworkError()
    object InitRequestExpired : NetworkError()
}
