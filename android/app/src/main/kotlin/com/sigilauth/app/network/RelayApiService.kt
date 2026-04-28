package com.wagmilabs.sigil.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Push Relay API client interface (Retrofit).
 *
 * Per OpenAPI spec - relay endpoints.
 *
 * AGPL-3.0 License
 */
interface RelayApiService {

    /**
     * POST /devices/register
     *
     * Registers device push token with relay.
     * Relay derives fingerprint from device_public_key.
     */
    @POST("/devices/register")
    suspend fun registerPushToken(
        @Body request: PushTokenRegistration
    ): Response<PushTokenRegistered>

    data class PushTokenRegistration(
        @SerializedName("device_public_key")
        val devicePublicKey: String,  // Base64-encoded compressed P-256 key

        @SerializedName("push_token")
        val pushToken: String,  // FCM token

        @SerializedName("platform")
        val platform: String  // "fcm"
    )

    data class PushTokenRegistered(
        @SerializedName("fingerprint")
        val fingerprint: String,

        @SerializedName("registered")
        val registered: Boolean
    )
}
