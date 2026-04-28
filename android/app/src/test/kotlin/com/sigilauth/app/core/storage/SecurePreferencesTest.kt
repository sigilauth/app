package com.wagmilabs.sigil.core.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for SecurePreferences (EncryptedSharedPreferences).
 *
 * Uses Robolectric for Android context.
 *
 * AGPL-3.0 License
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class SecurePreferencesTest {

    private lateinit var securePreferences: SecurePreferences
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        securePreferences = SecurePreferences(context)
        securePreferences.clear() // Start fresh
    }

    @Test
    fun `storePushToken and getPushToken work correctly`() {
        val testToken = "test_fcm_token_12345"

        securePreferences.storePushToken(testToken)
        val retrieved = securePreferences.getPushToken()

        assertEquals(testToken, retrieved)
    }

    @Test
    fun `getPushToken returns null when no token stored`() {
        val retrieved = securePreferences.getPushToken()
        assertNull(retrieved)
    }

    @Test
    fun `storeCurrentDevicePublicKey and getCurrentDevicePublicKey work correctly`() {
        val testPublicKey = "base64_encoded_public_key"

        securePreferences.storeCurrentDevicePublicKey(testPublicKey)
        val retrieved = securePreferences.getCurrentDevicePublicKey()

        assertEquals(testPublicKey, retrieved)
    }

    @Test
    fun `onboarding completion flag works correctly`() {
        assertFalse(securePreferences.isOnboardingComplete())

        securePreferences.setOnboardingComplete(true)
        assertTrue(securePreferences.isOnboardingComplete())

        securePreferences.setOnboardingComplete(false)
        assertFalse(securePreferences.isOnboardingComplete())
    }

    @Test
    fun `clear removes all stored data`() {
        securePreferences.storePushToken("token123")
        securePreferences.storeCurrentDevicePublicKey("pk123")
        securePreferences.setOnboardingComplete(true)

        securePreferences.clear()

        assertNull(securePreferences.getPushToken())
        assertNull(securePreferences.getCurrentDevicePublicKey())
        assertFalse(securePreferences.isOnboardingComplete())
    }

    @Test
    fun `multiple updates persist correctly`() {
        securePreferences.storePushToken("token1")
        assertEquals("token1", securePreferences.getPushToken())

        securePreferences.storePushToken("token2")
        assertEquals("token2", securePreferences.getPushToken())
    }
}
