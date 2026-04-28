package com.wagmilabs.sigil.network.models

import com.google.gson.annotations.SerializedName

/**
 * Pairing code redemption models.
 *
 * Per protocol-spec.md §2350-2377
 *
 * Security requirements:
 * - 8-digit numeric code
 * - Single-use, IP-bound
 * - 3 attempts max per code
 * - Constant-time comparison server-side
 *
 * AGPL-3.0 License
 */

data class PairingRedeemRequest(
    @SerializedName("pairing_code")
    val pairingCode: String
)

data class PairingRedeemResponse(
    @SerializedName("server_url")
    val serverUrl: String,

    @SerializedName("server_public_key")
    val serverPublicKey: String,

    @SerializedName("server_name")
    val serverName: String,

    @SerializedName("server_pictogram")
    val serverPictogram: List<String>,

    @SerializedName("server_pictogram_speakable")
    val serverPictogramSpeakable: String,

    @SerializedName("callback_url")
    val callbackUrl: String?,

    @SerializedName("session_token")
    val sessionToken: String
)

data class PairingError(
    @SerializedName("error")
    val error: ErrorDetail
)

data class ErrorDetail(
    @SerializedName("code")
    val code: String,

    @SerializedName("message")
    val message: String,

    @SerializedName("attempts_remaining")
    val attemptsRemaining: Int? = null
)
