package com.wagmilabs.sigil.ui.pairing

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wagmilabs.sigil.core.crypto.SessionPictogramDerivation
import com.wagmilabs.sigil.core.crypto.SessionPictogram as CryptoPictogram
import com.wagmilabs.sigil.core.storage.TrustStorageService
import com.wagmilabs.sigil.network.SigilApiService
import com.wagmilabs.sigil.network.models.PairCompleteRequest
import com.wagmilabs.sigil.network.models.TrustedServer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.util.Base64
import java.util.Date

class PairFlowViewModel(
    private val pictogramDerivation: SessionPictogramDerivation,
    private val trustStorage: TrustStorageService,
    private val getDevicePublicKey: suspend () -> ByteArray
) : ViewModel() {

    private val _state = MutableStateFlow<PairState>(PairState.Idle)
    val state: StateFlow<PairState> = _state.asStateFlow()

    private val _timeRemaining = MutableStateFlow(10)
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var timerJob: Job? = null
    private var expiryDate: Date? = null

    fun startPair(serverUrl: String) {
        viewModelScope.launch {
            _state.value = PairState.Loading
            _errorMessage.value = null

            try {
                val normalizedUrl = if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                    "https://$serverUrl"
                } else {
                    serverUrl
                }

                val retrofit = Retrofit.Builder()
                    .baseUrl(normalizedUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val apiService = retrofit.create(SigilApiService::class.java)

                val clientPublicKey = getDevicePublicKey()
                val clientPubBase64 = Base64.getEncoder().encodeToString(clientPublicKey)

                val initResponse = apiService.initiatePair(clientPubBase64)

                if (!initResponse.isSuccessful) {
                    throw Exception("Server error: ${initResponse.code()}")
                }

                val initData = initResponse.body() ?: throw Exception("Empty response from server")

                val serverPublicKey = Base64.getDecoder().decode(initData.serverPublicKey)
                val serverNonce = Base64.getDecoder().decode(initData.serverNonce)

                val derivedPictogram = pictogramDerivation.derive(
                    serverPublicKey = serverPublicKey,
                    clientPublicKey = clientPublicKey,
                    serverNonce = serverNonce
                )

                val serverPictogramWords = initData.sessionPictogram

                if (derivedPictogram.names != serverPictogramWords) {
                    throw Exception("Pictogram mismatch - possible MITM attack")
                }

                expiryDate = initData.expiresAt
                startTimer()

                _state.value = PairState.AwaitingConfirmation(
                    pictogram = derivedPictogram,
                    serverUrl = normalizedUrl,
                    serverId = initData.serverId,
                    serverPublicKey = serverPublicKey,
                    serverNonce = initData.serverNonce,
                    clientPublicKey = clientPublicKey,
                    apiService = apiService
                )

            } catch (e: Exception) {
                _state.value = PairState.Error(e.message ?: "Unknown error")
                _errorMessage.value = e.message
            }
        }
    }

    fun confirmPair() {
        val currentState = _state.value
        if (currentState !is PairState.AwaitingConfirmation) return

        viewModelScope.launch {
            _state.value = PairState.Completing
            stopTimer()

            try {
                val deviceName = Build.MODEL.take(64)
                val osVersion = Build.VERSION.RELEASE

                val completeRequest = PairCompleteRequest(
                    serverNonce = currentState.serverNonce,
                    clientPublicKey = Base64.getEncoder().encodeToString(currentState.clientPublicKey),
                    deviceInfo = PairCompleteRequest.DeviceInfo(
                        name = deviceName,
                        platform = "android",
                        osVersion = osVersion
                    )
                )

                // Try completion first - if admin pre-approved, succeeds immediately
                var response = currentState.apiService.completePair(completeRequest)

                if (!response.isSuccessful) {
                    val errorCode = response.errorBody()?.string() // Parse error code

                    if (response.code() == 403 && errorCode?.contains("NOT_APPROVED") == true) {
                        // Needs admin approval - start polling
                        _state.value = PairState.AwaitingAdminApproval(
                            serverUrl = currentState.serverUrl,
                            serverId = currentState.serverId,
                            serverPublicKey = currentState.serverPublicKey,
                            completeRequest = completeRequest,
                            apiService = currentState.apiService,
                            pollAttempts = 0
                        )

                        // Poll every 2 seconds for up to 5 minutes (150 attempts)
                        var attempts = 0
                        val maxAttempts = 150

                        while (attempts < maxAttempts) {
                            delay(2000) // 2 seconds
                            attempts++

                            // Update state with attempt count
                            _state.value = PairState.AwaitingAdminApproval(
                                serverUrl = currentState.serverUrl,
                                serverId = currentState.serverId,
                                serverPublicKey = currentState.serverPublicKey,
                                completeRequest = completeRequest,
                                apiService = currentState.apiService,
                                pollAttempts = attempts
                            )

                            response = currentState.apiService.completePair(completeRequest)

                            if (response.isSuccessful) {
                                // Success! Break out of poll loop
                                break
                            } else if (response.code() == 403 && errorCode?.contains("NOT_APPROVED") == true) {
                                // Still not approved, continue polling
                                if (attempts >= maxAttempts) {
                                    throw Exception("Pairing timed out waiting for admin approval")
                                }
                                continue
                            } else {
                                // Other error - fail immediately
                                throw Exception("Server error: ${response.code()}")
                            }
                        }
                    } else {
                        throw Exception("Server error: ${response.code()}")
                    }
                }

                val completeData = response.body() ?: throw Exception("Empty response from server")

                val serverFingerprint = MessageDigest.getInstance("SHA-256")
                    .digest(currentState.serverPublicKey)
                    .joinToString("") { "%02x".format(it) }

                val trustedServer = TrustedServer(
                    serverUrl = currentState.serverUrl,
                    serverId = currentState.serverId,
                    serverPublicKey = Base64.getEncoder().encodeToString(currentState.serverPublicKey),
                    serverFingerprint = serverFingerprint,
                    pairedAt = completeData.pairedAt
                )

                trustStorage.saveTrustedServer(trustedServer)

                _state.value = PairState.Paired(trustedServer)

            } catch (e: Exception) {
                _state.value = PairState.Error(e.message ?: "Unknown error")
                _errorMessage.value = e.message
            }
        }
    }

    fun denyPair() {
        stopTimer()
        _state.value = PairState.Denied
    }

    fun reset() {
        stopTimer()
        _state.value = PairState.Idle
        _errorMessage.value = null
        _timeRemaining.value = 10
    }

    private fun startTimer() {
        stopTimer()

        timerJob = viewModelScope.launch {
            while (true) {
                val expiry = expiryDate ?: break
                val now = Date()
                val remaining = ((expiry.time - now.time) / 1000).toInt()
                _timeRemaining.value = maxOf(0, remaining)

                if (remaining <= 0) {
                    denyPair()
                    break
                }

                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}

sealed class PairState {
    object Idle : PairState()
    object Loading : PairState()
    data class AwaitingConfirmation(
        val pictogram: CryptoPictogram,
        val serverUrl: String,
        val serverId: String,
        val serverPublicKey: ByteArray,
        val serverNonce: String,
        val clientPublicKey: ByteArray,
        val apiService: SigilApiService
    ) : PairState()
    object Completing : PairState()
    data class AwaitingAdminApproval(
        val serverUrl: String,
        val serverId: String,
        val serverPublicKey: ByteArray,
        val completeRequest: PairCompleteRequest,
        val apiService: SigilApiService,
        val pollAttempts: Int
    ) : PairState()
    data class Paired(val server: TrustedServer) : PairState()
    object Denied : PairState()
    data class Error(val message: String) : PairState()
}
