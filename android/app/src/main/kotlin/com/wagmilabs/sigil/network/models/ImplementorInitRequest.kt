package com.wagmilabs.sigil.network.models

import com.google.gson.annotations.SerializedName
import java.util.Date

data class ImplementorInitRequest(
    @SerializedName("request_id")
    val requestId: String,
    @SerializedName("implementor_name")
    val implementorName: String,
    @SerializedName("implementor_id")
    val implementorId: String,
    @SerializedName("implementor_ephemeral_public_key")
    val implementorEphemeralPublicKey: String,
    @SerializedName("timestamp")
    val timestamp: Date,
    @SerializedName("expires_at")
    val expiresAt: Date
)

data class ClaimRequest(
    @SerializedName("request_id")
    val requestId: String,
    @SerializedName("claim_code")
    val claimCode: String,
    @SerializedName("device_fingerprint")
    val deviceFingerprint: String,
    @SerializedName("device_signature")
    val deviceSignature: String
)

data class ClaimResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("pinned_to_fingerprint")
    val pinnedToFingerprint: String
)

data class InitRespondRequest(
    @SerializedName("request_id")
    val requestId: String,
    @SerializedName("device_fingerprint")
    val deviceFingerprint: String,
    @SerializedName("action")
    val action: String,
    @SerializedName("encrypted_mnemonic")
    val encryptedMnemonic: String? = null,
    @SerializedName("rejection_reason")
    val rejectionReason: String? = null,
    @SerializedName("device_signature")
    val deviceSignature: String
)

data class InitRespondResponse(
    @SerializedName("status")
    val status: String
)
