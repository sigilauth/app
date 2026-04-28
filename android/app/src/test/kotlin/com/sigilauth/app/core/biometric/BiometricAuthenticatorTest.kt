package com.wagmilabs.sigil.core.biometric

import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for BiometricAuthenticator.
 *
 * Note: Biometric authentication testing is limited in Robolectric.
 * Tests focus on capability detection and configuration.
 *
 * AGPL-3.0 License
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class BiometricAuthenticatorTest {

    @Test
    fun `checkBiometricCapability returns valid result`() {
        ActivityScenario.launch(FragmentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val authenticator = BiometricAuthenticator(activity)
                val capability = authenticator.checkBiometricCapability()

                // In Robolectric, capability will be NO_HARDWARE or HARDWARE_UNAVAILABLE
                // Real devices would show AVAILABLE
                assertNotNull(capability)
                assertTrue(
                    capability is BiometricAuthenticator.BiometricCapability.NO_HARDWARE ||
                    capability is BiometricAuthenticator.BiometricCapability.HARDWARE_UNAVAILABLE ||
                    capability is BiometricAuthenticator.BiometricCapability.AVAILABLE
                )
            }
        }
    }

    @Test
    fun `BiometricResult sealed class has all expected types`() {
        // Verify all result types can be instantiated
        val success = BiometricAuthenticator.BiometricResult.Success
        val canceled = BiometricAuthenticator.BiometricResult.Canceled
        val lockout = BiometricAuthenticator.BiometricResult.Lockout
        val error = BiometricAuthenticator.BiometricResult.Error("test")

        assertNotNull(success)
        assertNotNull(canceled)
        assertNotNull(lockout)
        assertNotNull(error)
        assertEquals("test", error.message)
    }

    @Test
    fun `BiometricCapability enum has all expected values`() {
        val capabilities = listOf(
            BiometricAuthenticator.BiometricCapability.AVAILABLE,
            BiometricAuthenticator.BiometricCapability.NO_HARDWARE,
            BiometricAuthenticator.BiometricCapability.HARDWARE_UNAVAILABLE,
            BiometricAuthenticator.BiometricCapability.NOT_ENROLLED,
            BiometricAuthenticator.BiometricCapability.SECURITY_UPDATE_REQUIRED,
            BiometricAuthenticator.BiometricCapability.UNSUPPORTED,
            BiometricAuthenticator.BiometricCapability.UNKNOWN
        )

        assertEquals(7, capabilities.size)
        capabilities.forEach { assertNotNull(it) }
    }
}
