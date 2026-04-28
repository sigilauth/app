package com.wagmilabs.sigil.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber

/**
 * Secure storage using EncryptedSharedPreferences.
 *
 * Per cascade-data-architecture.md §4:
 * - Device metadata encrypted at rest
 * - Server configs stored in Room (structured) + EncryptedSharedPrefs (tokens)
 *
 * AGPL-3.0 License
 */
class SecurePreferences(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "sigil_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Stores FCM push token securely.
     */
    fun storePushToken(token: String) {
        sharedPreferences.edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
        Timber.d("FCM token stored securely")
    }

    /**
     * Retrieves FCM push token.
     */
    fun getPushToken(): String? {
        return sharedPreferences.getString(KEY_FCM_TOKEN, null)
    }

    /**
     * Stores current device public key (for quick lookup).
     */
    fun storeCurrentDevicePublicKey(publicKeyB64: String) {
        sharedPreferences.edit()
            .putString(KEY_CURRENT_DEVICE_PK, publicKeyB64)
            .apply()
    }

    /**
     * Retrieves current device public key.
     */
    fun getCurrentDevicePublicKey(): String? {
        return sharedPreferences.getString(KEY_CURRENT_DEVICE_PK, null)
    }

    /**
     * Stores onboarding completion flag.
     */
    fun setOnboardingComplete(complete: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETE, complete)
            .apply()
    }

    /**
     * Checks if onboarding is complete.
     */
    fun isOnboardingComplete(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    /**
     * Clears all secure preferences (for sign-out/reset).
     */
    fun clear() {
        sharedPreferences.edit().clear().apply()
        Timber.d("Secure preferences cleared")
    }

    companion object {
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_CURRENT_DEVICE_PK = "current_device_public_key"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }
}
