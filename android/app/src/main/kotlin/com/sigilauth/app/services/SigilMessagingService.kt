package com.wagmilabs.sigil.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.wagmilabs.sigil.MainActivity
import com.wagmilabs.sigil.R
import com.wagmilabs.sigil.core.storage.SecurePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Firebase Cloud Messaging service for receiving push notifications.
 *
 * Per nova-mobile-platform-spec.md §3.2:
 * - Handles challenge push notifications
 * - Handles MPA push notifications
 * - Handles decrypt request notifications
 * - Manages FCM token registration and refresh
 *
 * AGPL-3.0 License
 */
class SigilMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM token refreshed")

        // Store token securely
        val securePrefs = SecurePreferences(applicationContext)
        securePrefs.storePushToken(token)

        // Register with relay in background
        serviceScope.launch {
            registerTokenWithRelay(token)
        }
    }

    private suspend fun registerTokenWithRelay(token: String) {
        try {
            // TODO: Get device public key from keystore
            // TODO: Call RelayApiService.registerPushToken()
            // For now, just log
            Timber.d("Token registration deferred until device keypair exists")
        } catch (e: Exception) {
            Timber.e(e, "Failed to register push token with relay")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Timber.d("Push notification received from: ${message.from}")

        val data = message.data
        when (data["type"]) {
            "challenge" -> {
                handleChallenge(data["payload"] ?: return)
            }
            "mpa_challenge" -> {
                handleMPAChallenge(data["payload"] ?: return)
            }
            "decrypt_request" -> {
                handleDecryptRequest(data["payload"] ?: return)
            }
            else -> {
                Timber.w("Unknown push notification type: ${data["type"]}")
            }
        }
    }

    private fun handleChallenge(payload: String) {
        Timber.d("Challenge push received")

        try {
            // Parse challenge payload (JSON per D2: plaintext over TLS)
            // TODO: Define ChallengePushPayload data class
            // TODO: Verify server signature before displaying

            // Display notification
            showChallengeNotification(
                title = "Authentication Request",
                message = "Tap to approve or deny",
                challengeId = "pending_parsing" // TODO: Extract from payload
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle challenge push")
        }
    }

    private fun handleMPAChallenge(payload: String) {
        Timber.d("MPA challenge push received")

        try {
            // TODO: Parse MPA challenge JSON
            // TODO: Check if user's group approval is needed
            // TODO: Display MPA notification with progress context

            showMPANotification(
                title = "Group Approval Required",
                message = "Tap to review request",
                requestId = "pending_parsing"
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle MPA challenge push")
        }
    }

    private fun handleDecryptRequest(payload: String) {
        Timber.d("Decrypt request push received")

        try {
            // TODO: Parse decrypt request JSON
            // TODO: Verify server signature
            // TODO: Display notification

            showDecryptNotification(
                title = "Decrypt Request",
                message = "Tap to approve decryption",
                requestId = "pending_parsing"
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle decrypt request push")
        }
    }

    private fun showChallengeNotification(title: String, message: String, challengeId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("challenge_id", challengeId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(challengeId.hashCode(), notification)
    }

    private fun showMPANotification(title: String, message: String, requestId: String) {
        // Similar to showChallengeNotification but with MPA context
        showChallengeNotification(title, message, requestId) // Temporary
    }

    private fun showDecryptNotification(title: String, message: String, requestId: String) {
        // Similar to showChallengeNotification but with decrypt context
        showChallengeNotification(title, message, requestId) // Temporary
    }
}
