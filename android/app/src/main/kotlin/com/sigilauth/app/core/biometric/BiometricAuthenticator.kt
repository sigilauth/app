package com.wagmilabs.sigil.core.biometric

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Handles biometric authentication with device credential fallback.
 *
 * Per aria-a11y-requirements.md §4.3:
 * - Device passcode fallback required when biometric unavailable
 * - WCAG 3.3.8 AA compliance
 *
 * Per knox-threat-model.md:
 * - Biometric gate on every signing operation
 * - User presence required
 *
 * AGPL-3.0 License
 */
class BiometricAuthenticator(
    private val activity: FragmentActivity
) {

    /**
     * Checks biometric availability on device.
     *
     * @return BiometricCapability enum
     */
    fun checkBiometricCapability(): BiometricCapability {
        val biometricManager = BiometricManager.from(activity)

        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricCapability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricCapability.SECURITY_UPDATE_REQUIRED
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricCapability.UNSUPPORTED
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricCapability.UNKNOWN
            else -> BiometricCapability.UNKNOWN
        }
    }

    /**
     * Authenticates user with biometric prompt.
     *
     * Per aria-a11y-requirements.md §4.3:
     * - Supports BIOMETRIC_STRONG or DEVICE_CREDENTIAL
     * - Device passcode allowed as fallback
     *
     * @param title Prompt title
     * @param subtitle Optional subtitle
     * @param description Optional description
     * @return BiometricResult (success or error)
     */
    suspend fun authenticate(
        title: String,
        subtitle: String? = null,
        description: String? = null
    ): BiometricResult = suspendCancellableCoroutine { continuation ->

        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Timber.d("Biometric authentication succeeded")
                    if (continuation.isActive) {
                        continuation.resume(BiometricResult.Success)
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Timber.w("Biometric authentication failed")
                    // Don't cancel continuation - user can retry
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Timber.e("Biometric authentication error: $errorCode - $errString")

                    val error = when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> BiometricResult.Canceled
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricResult.Lockout
                        else -> BiometricResult.Error(errString.toString())
                    }

                    if (continuation.isActive) {
                        continuation.resume(error)
                    }
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply {
                subtitle?.let { setSubtitle(it) }
                description?.let { setDescription(it) }
            }
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        continuation.invokeOnCancellation {
            biometricPrompt.cancelAuthentication()
        }

        biometricPrompt.authenticate(promptInfo)
    }

    enum class BiometricCapability {
        AVAILABLE,
        NO_HARDWARE,
        HARDWARE_UNAVAILABLE,
        NOT_ENROLLED,
        SECURITY_UPDATE_REQUIRED,
        UNSUPPORTED,
        UNKNOWN
    }

    sealed class BiometricResult {
        object Success : BiometricResult()
        object Canceled : BiometricResult()
        object Lockout : BiometricResult()
        data class Error(val message: String) : BiometricResult()
    }
}
