package com.wagmilabs.sigil.network.models

import com.google.gson.annotations.SerializedName

/**
 * Challenge-related data models per OpenAPI schema.
 *
 * Per D10: uses pictogram_speakable (space-separated).
 *
 * AGPL-3.0 License
 */

/**
 * Challenge request (POST /challenge).
 */
data class ChallengeRequest(
    @SerializedName("fingerprint")
    val fingerprint: String,  // Hex-encoded 64-char fingerprint

    @SerializedName("device_public_key")
    val devicePublicKey: String,  // Base64-encoded compressed P-256 key

    @SerializedName("action")
    val action: Action
)

/**
 * Challenge created response (201 from POST /challenge).
 */
data class ChallengeCreated(
    @SerializedName("challenge_id")
    val challengeId: String,  // UUID

    @SerializedName("fingerprint")
    val fingerprint: String,

    @SerializedName("pictogram")
    val pictogram: List<String>,

    @SerializedName("pictogram_speakable")
    val pictogramSpeakable: String,  // Space-separated per D10

    @SerializedName("expires_at")
    val expiresAt: String  // ISO8601 timestamp
)

/**
 * Challenge response (POST /respond).
 */
data class ChallengeResponse(
    @SerializedName("challenge_id")
    val challengeId: String,

    @SerializedName("device_public_key")
    val devicePublicKey: String,

    @SerializedName("decision")
    val decision: String,  // "approved" | "denied"

    @SerializedName("signature")
    val signature: String,  // Base64-encoded ECDSA signature

    @SerializedName("timestamp")
    val timestamp: String  // ISO8601 timestamp
)

/**
 * Challenge verified response (200 from POST /respond).
 */
data class ChallengeVerified(
    @SerializedName("verified")
    val verified: Boolean,

    @SerializedName("fingerprint")
    val fingerprint: String,

    @SerializedName("pictogram")
    val pictogram: List<String>,

    @SerializedName("pictogram_speakable")
    val pictogramSpeakable: String,  // Space-separated per D10

    @SerializedName("action")
    val action: Action
)

/**
 * Challenge status (GET /v1/auth/challenge/{id}/status).
 */
data class ChallengeStatus(
    @SerializedName("challenge_id")
    val challengeId: String,

    @SerializedName("status")
    val status: String,  // "pending" | "verified" | "denied" | "expired"

    @SerializedName("fingerprint")
    val fingerprint: String? = null,

    @SerializedName("pictogram")
    val pictogram: List<String>? = null,

    @SerializedName("pictogram_speakable")
    val pictogramSpeakable: String? = null,  // Space-separated per D10

    @SerializedName("decision")
    val decision: String? = null,

    @SerializedName("verified_at")
    val verifiedAt: String? = null
)

/**
 * Action context (step-up, login, decrypt, MPA).
 */
data class Action(
    @SerializedName("type")
    val type: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("params")
    val params: Map<String, Any>? = null,

    @SerializedName("details_url")
    val detailsUrl: String? = null  // Optional URL to view action details in external browser
)
