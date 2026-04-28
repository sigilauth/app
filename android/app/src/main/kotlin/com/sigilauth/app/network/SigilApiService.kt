package com.wagmilabs.sigil.network

import com.wagmilabs.sigil.network.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Sigil Auth API client interface (Retrofit).
 *
 * Per OpenAPI spec at /Volumes/Expansion/src/sigilauth/api/openapi.yaml
 *
 * AGPL-3.0 License
 */
interface SigilApiService {

    /**
     * GET /info
     *
     * Returns server information (public key, pictogram, features).
     * No authentication required.
     */
    @GET("/info")
    suspend fun getServerInfo(): Response<ServerInfo>

    /**
     * POST /challenge
     *
     * Creates authentication challenge.
     * Requires Bearer API key (integrator only - mobile apps don't call this directly).
     */
    @POST("/challenge")
    suspend fun createChallenge(
        @Header("Authorization") bearerToken: String,
        @Body request: ChallengeRequest
    ): Response<ChallengeCreated>

    /**
     * POST /respond
     *
     * Submits challenge response with device signature.
     * Device self-authenticates via public_key + signature.
     */
    @POST("/respond")
    suspend fun respondChallenge(
        @Body response: ChallengeResponse
    ): Response<ChallengeVerified>

    /**
     * GET /v1/auth/challenge/{id}/status
     *
     * Polls challenge status (alternative to webhook).
     * Requires Bearer API key (integrator only).
     */
    @GET("/v1/auth/challenge/{id}/status")
    suspend fun getChallengeStatus(
        @Path("id") challengeId: String,
        @Header("Authorization") bearerToken: String
    ): Response<ChallengeStatus>

    /**
     * POST /pairing/redeem
     *
     * Redeems 8-digit pairing code for server configuration.
     * Per protocol-spec.md §2350-2377.
     * No authentication required (code is proof).
     */
    @POST("/pairing/redeem")
    suspend fun redeemPairingCode(
        @Body request: PairingRedeemRequest
    ): Response<PairingRedeemResponse>

    /**
     * GET /pair/init
     *
     * Initiates pair handshake (plaintext).
     * Returns server public key, nonce, and session pictogram.
     */
    @GET("/pair/init")
    suspend fun initiatePair(
        @Query("client_pub") clientPublicKey: String
    ): Response<PairInitResponse>

    /**
     * POST /pair/complete
     *
     * Completes pair handshake (plaintext).
     * Marks nonce consumed and returns paired status.
     */
    @POST("/pair/complete")
    suspend fun completePair(
        @Body request: PairCompleteRequest
    ): Response<PairCompleteResponse>
}
