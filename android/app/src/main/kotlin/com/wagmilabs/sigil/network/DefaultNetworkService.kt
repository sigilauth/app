package com.wagmilabs.sigil.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.wagmilabs.sigil.network.models.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * OkHttp-based implementation of NetworkService
 * Handles JSON encoding/decoding with ISO8601 dates
 */
class DefaultNetworkService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : NetworkService {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, DateSerializer())
        .registerTypeAdapter(Date::class.java, DateDeserializer())
        .create()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun fetchServerInfo(baseURL: String): ServerInfo {
        val url = "$baseURL/info"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .build()

        return executeRequest(request)
    }

    override suspend fun respondToChallenge(
        baseURL: String,
        response: ChallengeResponse
    ): ChallengeVerified {
        val url = "$baseURL/respond"

        val json = gson.toJson(response)
        val body = json.toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        return executeRequestWithErrorHandling(request) { statusCode, apiError ->
            when (apiError?.error?.code) {
                "INVALID_SIGNATURE" -> NetworkError.SignatureVerificationFailed
                "FINGERPRINT_MISMATCH" -> NetworkError.FingerprintMismatch
                "CHALLENGE_NOT_FOUND" -> NetworkError.ChallengeNotFound
                else -> NetworkError.ServerError(statusCode, apiError?.error?.message)
            }
        }
    }

    override suspend fun redeemPairingCode(
        code: String,
        pairingURL: String
    ): PairingPayload {
        val url = "$pairingURL/pairing/redeem"

        val redeemRequest = mapOf("pairingCode" to code)
        val json = gson.toJson(redeemRequest)
        val body = json.toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        return executeRequestWithErrorHandling(request) { statusCode, _ ->
            when (statusCode) {
                400 -> NetworkError.PairingCodeInvalid
                404 -> NetworkError.PairingCodeNotFound
                429 -> NetworkError.PairingCodeTooManyAttempts
                else -> NetworkError.ServerError(statusCode, null)
            }
        }
    }

    override suspend fun initiatePair(
        baseURL: String,
        clientPublicKey: ByteArray
    ): PairInitResponse {
        val encodedPubKey = android.util.Base64.encodeToString(
            clientPublicKey,
            android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE
        )
        val url = "$baseURL/pair/init?client_pub=$encodedPubKey"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .build()

        return executeRequest(request)
    }

    override suspend fun completePair(
        baseURL: String,
        request: PairCompleteRequest
    ): PairCompleteResponse {
        val url = "$baseURL/pair/complete"

        val json = gson.toJson(request)
        val body = json.toRequestBody(jsonMediaType)

        val httpRequest = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        return executeRequestWithErrorHandling(httpRequest) { statusCode, apiError ->
            when (apiError?.error?.code) {
                "HANDSHAKE_EXPIRED" -> NetworkError.PairHandshakeExpired
                "NOT_APPROVED" -> NetworkError.PairHandshakeNotApproved
                "NONCE_CONSUMED" -> NetworkError.PairNonceConsumed
                else -> NetworkError.ServerError(statusCode, apiError?.error?.message)
            }
        }
    }

    override suspend fun claimInitRequest(
        baseURL: String,
        request: ClaimRequest
    ): ClaimResponse {
        val url = "$baseURL/device/init/claim"

        val json = gson.toJson(request)
        val body = json.toRequestBody(jsonMediaType)

        val httpRequest = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        return executeRequestWithErrorHandling(httpRequest) { statusCode, apiError ->
            when (apiError?.error?.code) {
                "INVALID_CODE" -> NetworkError.InitClaimInvalidCode
                "REQUEST_NOT_FOUND" -> NetworkError.InitRequestNotFound
                "ALREADY_CLAIMED" -> NetworkError.InitAlreadyClaimed
                "CODE_EXPIRED" -> NetworkError.InitCodeExpired
                "RATE_LIMIT" -> NetworkError.InitRateLimited
                else -> NetworkError.ServerError(statusCode, apiError?.error?.message)
            }
        }
    }

    override suspend fun respondToInitRequest(
        baseURL: String,
        request: InitRespondRequest
    ): InitRespondResponse {
        val url = "$baseURL/device/init/respond"

        val json = gson.toJson(request)
        val body = json.toRequestBody(jsonMediaType)

        val httpRequest = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        return executeRequestWithErrorHandling(httpRequest) { statusCode, apiError ->
            when (apiError?.error?.code) {
                "INVALID_MNEMONIC" -> NetworkError.InitInvalidMnemonic
                "REQUEST_NOT_FOUND" -> NetworkError.InitRequestNotFound
                "ALREADY_RESPONDED" -> NetworkError.InitAlreadyResponded
                "ALREADY_APPROVED" -> NetworkError.InitAlreadyApproved
                "REQUEST_EXPIRED" -> NetworkError.InitRequestExpired
                else -> NetworkError.ServerError(statusCode, apiError?.error?.message)
            }
        }
    }

    // MARK: - Helpers

    private inline fun <reified T> executeRequest(request: Request): T {
        return try {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                val apiError = try {
                    gson.fromJson(errorBody, APIError::class.java)
                } catch (e: Exception) {
                    null
                }
                throw NetworkError.ServerError(response.code, apiError?.error?.message)
            }

            val body = response.body?.string()
                ?: throw NetworkError.DecodingError("Empty response body")

            gson.fromJson(body, T::class.java)
                ?: throw NetworkError.DecodingError("Failed to decode response")

        } catch (e: IOException) {
            throw NetworkError.NoInternetConnection
        } catch (e: NetworkError) {
            throw e
        } catch (e: Exception) {
            throw NetworkError.DecodingError(e.message ?: "Unknown error")
        }
    }

    private inline fun <reified T> executeRequestWithErrorHandling(
        request: Request,
        errorMapper: (statusCode: Int, apiError: APIError?) -> NetworkError
    ): T {
        return try {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                val apiError = try {
                    gson.fromJson(errorBody, APIError::class.java)
                } catch (e: Exception) {
                    null
                }
                throw errorMapper(response.code, apiError)
            }

            val body = response.body?.string()
                ?: throw NetworkError.DecodingError("Empty response body")

            gson.fromJson(body, T::class.java)
                ?: throw NetworkError.DecodingError("Failed to decode response")

        } catch (e: IOException) {
            throw NetworkError.NoInternetConnection
        } catch (e: NetworkError) {
            throw e
        } catch (e: Exception) {
            throw NetworkError.DecodingError(e.message ?: "Unknown error")
        }
    }

    // MARK: - Date Serialization

    private class DateSerializer : JsonSerializer<Date> {
        private val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        override fun serialize(
            src: Date?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            return JsonPrimitive(format.format(src ?: Date()))
        }
    }

    private class DateDeserializer : JsonDeserializer<Date> {
        private val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): Date {
            return format.parse(json?.asString ?: "") ?: Date()
        }
    }
}

/**
 * API error response model
 */
data class APIError(
    val error: ErrorDetail
) {
    data class ErrorDetail(
        val code: String,
        val message: String
    )
}
