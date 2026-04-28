package com.wagmilabs.sigil

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.wagmilabs.sigil.core.storage.EncryptedTrustStorage
import com.wagmilabs.sigil.core.storage.TrustStorageService
import com.wagmilabs.sigil.network.models.TrustedServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Main application class for Sigil Auth.
 * Loads trusted servers at startup per Phase 3 trust persistence.
 *
 * AGPL-3.0 License
 */
class SigilAuthApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var trustStorage: TrustStorageService

    // Trusted servers loaded at startup
    private val _trustedServers = mutableListOf<TrustedServer>()
    val trustedServers: List<TrustedServer>
        get() = _trustedServers.toList()

    override fun onCreate() {
        super.onCreate()

        // Initialize logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize trust storage
        trustStorage = EncryptedTrustStorage(this)

        // Create notification channel for push auth
        createNotificationChannels()

        // Load trusted servers
        loadTrustedServers()

        Timber.d("SigilAuth initialized")
    }

    private fun loadTrustedServers() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                val servers = trustStorage.loadAllTrustedServers()
                _trustedServers.clear()
                _trustedServers.addAll(servers)
                Timber.d("Loaded ${servers.size} trusted servers")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load trusted servers")
            }
        }
    }

    fun getTrust(fingerprint: String): TrustedServer? {
        return _trustedServers.firstOrNull { it.serverFingerprint == fingerprint }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Challenge notification channel
            val challengeChannel = NotificationChannel(
                getString(R.string.default_notification_channel_id),
                getString(R.string.notification_channel_challenges_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_challenges_description)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(challengeChannel)
        }
    }

    companion object {
        lateinit var instance: SigilAuthApplication
            private set
    }

    init {
        instance = this
    }
}
