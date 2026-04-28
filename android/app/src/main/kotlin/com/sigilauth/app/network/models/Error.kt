package com.wagmilabs.sigil.network.models

import com.google.gson.annotations.SerializedName

/**
 * Standard error response per OpenAPI schema.
 *
 * Used for 4xx and 5xx responses.
 *
 * AGPL-3.0 License
 */
data class ErrorResponse(
    @SerializedName("error")
    val error: ErrorDetail
) {
    data class ErrorDetail(
        @SerializedName("code")
        val code: String,

        @SerializedName("message")
        val message: String,

        @SerializedName("details")
        val details: Map<String, Any>? = null
    )
}

/**
 * Error codes per OpenAPI spec.
 */
object ErrorCodes {
    const val FINGERPRINT_MISMATCH = "FINGERPRINT_MISMATCH"
    const val INVALID_PUBLIC_KEY = "INVALID_PUBLIC_KEY"
    const val INVALID_SIGNATURE = "INVALID_SIGNATURE"
    const val CHALLENGE_NOT_FOUND = "CHALLENGE_NOT_FOUND"
    const val CHALLENGE_ALREADY_USED = "CHALLENGE_ALREADY_USED"
    const val CHALLENGE_EXPIRED = "CHALLENGE_EXPIRED"
    const val UNAUTHORIZED = "UNAUTHORIZED"
    const val RATE_LIMITED = "RATE_LIMITED"
}
