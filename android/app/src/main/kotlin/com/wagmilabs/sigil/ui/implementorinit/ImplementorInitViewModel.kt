package com.wagmilabs.sigil.ui.implementorinit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wagmilabs.sigil.core.crypto.BIP39MnemonicGenerator
import com.wagmilabs.sigil.core.crypto.DomainTag
import com.wagmilabs.sigil.core.crypto.ECIESService
import com.wagmilabs.sigil.core.crypto.KeyStoreService
import com.wagmilabs.sigil.network.NetworkError
import com.wagmilabs.sigil.network.NetworkService
import com.wagmilabs.sigil.network.models.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URL
import java.nio.ByteBuffer
import java.util.Date

class ImplementorInitViewModel(
    private val mnemonicGenerator: BIP39MnemonicGenerator,
    private val eciesService: ECIESService,
    private val networkService: NetworkService,
    private val keyStoreService: KeyStoreService
) : ViewModel() {

    private val _state = MutableStateFlow<ImplementorInitState>(ImplementorInitState.Idle)
    val state: StateFlow<ImplementorInitState> = _state.asStateFlow()

    private val _timeRemaining = MutableStateFlow(0)
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()

    private var timerJob: Job? = null
    private var generatedMnemonic: List<String>? = null

    // Configuration
    private var baseURL: String? = null
    private var deviceFingerprint: String? = null
    private val deviceKeyAlias: String = "device-key"

    fun configure(baseURL: String, deviceFingerprint: String) {
        this.baseURL = baseURL
        this.deviceFingerprint = deviceFingerprint
    }

    // MARK: - Flow B: Claim Gate

    fun startClaimFlow() {
        _state.value = ImplementorInitState.ClaimCodeEntry
    }

    fun submitClaimCode(code: String) {
        viewModelScope.launch {
            _state.value = ImplementorInitState.Claiming

            try {
                val currentBaseURL = baseURL ?: throw IllegalStateException("No server configured")
                val currentFingerprint = deviceFingerprint ?: throw IllegalStateException("No device fingerprint available")

                // Parse input: could be full URL or just code
                val (requestId, normalizedCode) = parseClaimInput(code)

                // Generate device signature: request_id || claim_code || timestamp
                val timestamp = System.currentTimeMillis() / 1000.0
                val signaturePayload = ByteBuffer.allocate(
                    requestId.toByteArray().size + normalizedCode.toByteArray().size + 8
                ).apply {
                    put(requestId.toByteArray())
                    put(normalizedCode.toByteArray())
                    putDouble(timestamp)
                }.array()

                val signature = keyStoreService.sign(signaturePayload, DomainTag.AUTH, deviceKeyAlias)

                val claimRequest = ClaimRequest(
                    requestId = requestId,
                    claimCode = normalizedCode,
                    deviceFingerprint = currentFingerprint,
                    deviceSignature = android.util.Base64.encodeToString(signature, android.util.Base64.NO_WRAP)
                )

                val response = networkService.claimInitRequest(currentBaseURL, claimRequest)

                // Success - now wait for push
                _state.value = ImplementorInitState.AwaitingPush
                startAwaitingPushTimeout(duration = 60)

            } catch (e: NetworkError) {
                val message = formatNetworkError(e)
                _state.value = ImplementorInitState.ClaimError(message)
            } catch (e: Exception) {
                _state.value = ImplementorInitState.ClaimError("Failed to claim request. Please try again.")
            }
        }
    }

    private fun parseClaimInput(input: String): Pair<String, String> {
        // Check if input is a URL
        if (input.startsWith("http://") || input.startsWith("https://")) {
            try {
                val url = URL(input)
                val params = url.query.split("&").associate {
                    val parts = it.split("=")
                    parts[0] to (parts.getOrNull(1) ?: "")
                }
                val requestId = params["r"] ?: throw IllegalArgumentException("Missing r= param")
                val code = params["c"] ?: throw IllegalArgumentException("Missing c= param")
                return requestId to normalizeClaimCode(code)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid claim URL format")
            }
        }

        // Just a code - error
        throw IllegalArgumentException("Request ID required - please scan QR code")
    }

    private fun normalizeClaimCode(code: String): String {
        return code.uppercase().replace(Regex("[\\s-]"), "")
    }

    private fun formatNetworkError(error: NetworkError): String {
        return when (error) {
            is NetworkError.InitClaimInvalidCode -> "Invalid claim code format"
            is NetworkError.InitRequestNotFound -> "Request not found or expired"
            is NetworkError.InitAlreadyClaimed -> "This request has already been claimed by another device"
            is NetworkError.InitCodeExpired -> "Claim code has expired (60 second limit)"
            is NetworkError.InitRateLimited -> "Too many attempts - request invalidated"
            is NetworkError.NoInternetConnection -> "No internet connection"
            is NetworkError.Timeout -> "Request timed out"
            else -> "Network error occurred"
        }
    }

    // MARK: - Flow A: Approval (Push-Triggered or Post-Claim)

    fun receiveInitRequest(request: ImplementorInitRequest) {
        _state.value = ImplementorInitState.ApprovalRequested(request)
        startExpiryTimer(expiresAt = request.expiresAt)
    }

    fun approveRequest() {
        val currentState = _state.value
        if (currentState !is ImplementorInitState.ApprovalRequested) return

        viewModelScope.launch {
            // Biometric prompt happens in UI layer
            _state.value = ImplementorInitState.Generating

            try {
                val words = mnemonicGenerator.generate()
                generatedMnemonic = words

                _state.value = ImplementorInitState.DisplayingMnemonic(
                    words = words,
                    request = currentState.request
                )

            } catch (e: Exception) {
                _state.value = ImplementorInitState.Error("Failed to generate mnemonic: ${e.message}")
            }
        }
    }

    fun rejectRequest() {
        _state.value = ImplementorInitState.Rejected
    }

    // MARK: - Mnemonic Display

    fun confirmWrittenDown() {
        val currentState = _state.value
        if (currentState !is ImplementorInitState.DisplayingMnemonic) return

        viewModelScope.launch {
            _state.value = ImplementorInitState.EncryptingSubmitting

            try {
                val currentBaseURL = baseURL ?: throw IllegalStateException("No server configured")
                val currentFingerprint = deviceFingerprint ?: throw IllegalStateException("No device fingerprint available")

                // Encrypt mnemonic to implementor's ephemeral pubkey
                val implementorPubkey = android.util.Base64.decode(
                    currentState.request.implementorEphemeralPublicKey,
                    android.util.Base64.NO_WRAP
                )

                val mnemonicString = currentState.words.joinToString(" ")
                val encryptedMnemonic = eciesService.encrypt(
                    plaintext = mnemonicString.toByteArray(Charsets.UTF_8),
                    recipientPublicKey = implementorPubkey
                )

                // Generate device signature: request_id || action || timestamp
                val action = "approve"
                val timestamp = System.currentTimeMillis() / 1000.0
                val signaturePayload = ByteBuffer.allocate(
                    currentState.request.requestId.toByteArray().size + action.toByteArray().size + 8
                ).apply {
                    put(currentState.request.requestId.toByteArray())
                    put(action.toByteArray())
                    putDouble(timestamp)
                }.array()

                val signature = keyStoreService.sign(signaturePayload, DomainTag.AUTH, deviceKeyAlias)

                val respondRequest = InitRespondRequest(
                    requestId = currentState.request.requestId,
                    deviceFingerprint = currentFingerprint,
                    action = action,
                    encryptedMnemonic = android.util.Base64.encodeToString(encryptedMnemonic, android.util.Base64.NO_WRAP),
                    rejectionReason = null,
                    deviceSignature = android.util.Base64.encodeToString(signature, android.util.Base64.NO_WRAP)
                )

                val response = networkService.respondToInitRequest(currentBaseURL, respondRequest)

                // Clear mnemonic from memory
                generatedMnemonic = null

                _state.value = ImplementorInitState.AwaitingConfirmation

            } catch (e: NetworkError) {
                val message = formatInitRespondError(e)
                _state.value = ImplementorInitState.Error(message)
            } catch (e: Exception) {
                _state.value = ImplementorInitState.Error("Failed to submit mnemonic: ${e.message}")
            }
        }
    }

    private fun formatInitRespondError(error: NetworkError): String {
        return when (error) {
            is NetworkError.InitInvalidMnemonic -> "Invalid mnemonic format"
            is NetworkError.InitRequestNotFound -> "Request not found or expired"
            is NetworkError.InitAlreadyResponded -> "You have already responded to this request"
            is NetworkError.InitAlreadyApproved -> "Another device has already approved this request"
            is NetworkError.InitRequestExpired -> "Request has expired"
            is NetworkError.NoInternetConnection -> "No internet connection"
            is NetworkError.Timeout -> "Request timed out"
            else -> "Network error occurred"
        }
    }

    fun copyMnemonicToClipboard(context: android.content.Context) {
        val currentState = _state.value
        if (currentState !is ImplementorInitState.DisplayingMnemonic) return

        val mnemonicString = currentState.words.joinToString(" ")
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("mnemonic", mnemonicString)
        clipboard.setPrimaryClip(clip)

        // Auto-clear after 60 seconds
        viewModelScope.launch {
            delay(60_000)
            val currentClip = clipboard.primaryClip
            if (currentClip?.getItemAt(0)?.text == mnemonicString) {
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("", ""))
            }
        }
    }

    // MARK: - Confirmation

    fun receiveImplementorConfirmation() {
        _state.value = ImplementorInitState.Confirmed
    }

    // MARK: - Timers

    private fun startExpiryTimer(expiresAt: Date) {
        stopTimer()

        timerJob = viewModelScope.launch {
            while (true) {
                val remaining = ((expiresAt.time - System.currentTimeMillis()) / 1000).toInt()
                _timeRemaining.value = maxOf(0, remaining)

                if (remaining <= 0) {
                    _state.value = ImplementorInitState.Expired
                    break
                }

                delay(1000)
            }
        }
    }

    private fun startAwaitingPushTimeout(duration: Int) {
        stopTimer()
        _timeRemaining.value = duration

        timerJob = viewModelScope.launch {
            repeat(duration) {
                if (_timeRemaining.value > 0) {
                    delay(1000)
                    _timeRemaining.value -= 1
                }
            }

            if (_timeRemaining.value == 0) {
                _state.value = ImplementorInitState.Expired
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun reset() {
        stopTimer()
        _state.value = ImplementorInitState.Idle
        _timeRemaining.value = 0
        generatedMnemonic = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}

sealed class ImplementorInitState {
    object Idle : ImplementorInitState()

    // Flow B: Claim Gate
    object ClaimCodeEntry : ImplementorInitState()
    object Claiming : ImplementorInitState()
    object AwaitingPush : ImplementorInitState()
    data class ClaimError(val message: String) : ImplementorInitState()

    // Flow A: Approval (both flows converge here)
    data class ApprovalRequested(val request: ImplementorInitRequest) : ImplementorInitState()
    object Generating : ImplementorInitState()
    data class DisplayingMnemonic(
        val words: List<String>,
        val request: ImplementorInitRequest
    ) : ImplementorInitState()
    object EncryptingSubmitting : ImplementorInitState()
    object AwaitingConfirmation : ImplementorInitState()
    object Confirmed : ImplementorInitState()

    // Terminal states
    object Rejected : ImplementorInitState()
    object Expired : ImplementorInitState()
    data class Error(val message: String) : ImplementorInitState()
}
