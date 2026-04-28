package com.wagmilabs.sigil.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wagmilabs.sigil.network.SigilApiService
import com.wagmilabs.sigil.network.models.PairingRedeemRequest
import com.wagmilabs.sigil.network.models.PairingRedeemResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for pairing code entry and redemption.
 *
 * Security requirements (protocol-spec.md §2371-2377):
 * - 8-digit numeric code
 * - 3 attempts max per code (enforced server-side + UI)
 * - IP-bound, single-use (server-side)
 * - Constant-time comparison (server-side)
 *
 * AGPL-3.0 License
 */
class PairingViewModel(
    private val apiService: SigilApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<PairingUiState>(PairingUiState.Idle)
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    private val _attemptsRemaining = MutableStateFlow(3)
    val attemptsRemaining: StateFlow<Int> = _attemptsRemaining.asStateFlow()

    fun redeemCode(code: String) {
        if (code.length != 8 || !code.all { it.isDigit() }) {
            _uiState.value = PairingUiState.Error(
                message = "error-invalid-pairing-code",
                canRetry = true
            )
            return
        }

        if (_attemptsRemaining.value <= 0) {
            _uiState.value = PairingUiState.LockedOut
            return
        }

        viewModelScope.launch {
            _uiState.value = PairingUiState.Loading

            try {
                val response = apiService.redeemPairingCode(
                    PairingRedeemRequest(pairingCode = code)
                )

                when {
                    response.isSuccessful && response.body() != null -> {
                        val result = response.body()!!
                        _uiState.value = PairingUiState.Success(result)
                        Timber.i("Pairing code redeemed successfully: ${result.serverName}")
                    }
                    response.code() == 400 -> {
                        _attemptsRemaining.value = _attemptsRemaining.value - 1
                        _uiState.value = if (_attemptsRemaining.value > 0) {
                            PairingUiState.Error(
                                message = "error-invalid-pairing-code",
                                canRetry = true
                            )
                        } else {
                            PairingUiState.LockedOut
                        }
                    }
                    response.code() == 404 -> {
                        _attemptsRemaining.value = _attemptsRemaining.value - 1
                        _uiState.value = PairingUiState.Error(
                            message = "error-pairing-code-expired",
                            canRetry = _attemptsRemaining.value > 0
                        )
                    }
                    response.code() == 409 -> {
                        _uiState.value = PairingUiState.Error(
                            message = "error-pairing-code-used",
                            canRetry = false
                        )
                    }
                    response.code() == 429 -> {
                        _uiState.value = PairingUiState.LockedOut
                    }
                    else -> {
                        _attemptsRemaining.value = _attemptsRemaining.value - 1
                        _uiState.value = PairingUiState.Error(
                            message = "error-server",
                            canRetry = _attemptsRemaining.value > 0
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Pairing code redemption failed")
                _attemptsRemaining.value = _attemptsRemaining.value - 1
                _uiState.value = PairingUiState.Error(
                    message = "error-network",
                    canRetry = _attemptsRemaining.value > 0
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = PairingUiState.Idle
    }

    fun resetAttempts() {
        _attemptsRemaining.value = 3
        _uiState.value = PairingUiState.Idle
    }
}

sealed class PairingUiState {
    object Idle : PairingUiState()
    object Loading : PairingUiState()
    data class Success(val response: PairingRedeemResponse) : PairingUiState()
    data class Error(val message: String, val canRetry: Boolean) : PairingUiState()
    object LockedOut : PairingUiState()
}
