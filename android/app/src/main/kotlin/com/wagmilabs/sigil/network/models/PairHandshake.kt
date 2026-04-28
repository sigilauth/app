package com.wagmilabs.sigil.network.models

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Response from GET /pair/init endpoint
 */
data class PairInitResponse(
    @SerializedName("server_id")
    val serverId: String,
    @SerializedName("server_public_key")
    val serverPublicKey: String,
    @SerializedName("server_nonce")
    val serverNonce: String,
    @SerializedName("expires_at")
    val expiresAt: Date,
    @SerializedName("session_pictogram")
    val sessionPictogram: List<String>,
    @SerializedName("session_pictogram_speakable")
    val sessionPictogramSpeakable: String
)

/**
 * Request body for POST /pair/complete
 */
data class PairCompleteRequest(
    @SerializedName("server_nonce")
    val serverNonce: String,
    @SerializedName("client_public_key")
    val clientPublicKey: String,
    @SerializedName("device_info")
    val deviceInfo: DeviceInfo
) {
    data class DeviceInfo(
        val name: String,
        val platform: String,
        @SerializedName("os_version")
        val osVersion: String
    )
}

/**
 * Response from POST /pair/complete
 */
data class PairCompleteResponse(
    val status: String,
    @SerializedName("server_public_key")
    val serverPublicKey: String,
    @SerializedName("paired_at")
    val pairedAt: Date
)

/**
 * Stored trust after successful pair
 */
data class TrustedServer(
    val serverUrl: String,
    val serverId: String,
    val serverPublicKey: String,
    val serverFingerprint: String,
    val pairedAt: Date
)
