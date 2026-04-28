package com.wagmilabs.sigil.core.crypto

import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import com.wagmilabs.sigil.core.biometric.BiometricAuthenticator
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.PrivateKey

/**
 * Tests for SigningManager.
 *
 * Uses MockK for biometric mocking.
 *
 * AGPL-3.0 License
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class SigningManagerTest {

    private lateinit var keystoreManager: KeystoreManager
    private lateinit var biometricAuthenticator: BiometricAuthenticator
    private lateinit var signingManager: SigningManager

    @Before
    fun setup() {
        keystoreManager = mockk()
        biometricAuthenticator = mockk()
        signingManager = SigningManager(keystoreManager, biometricAuthenticator)
    }

    @Test
    fun `signChallengeResponse throws when user cancels biometric`() = runTest {
        // Mock biometric canceled
        coEvery {
            biometricAuthenticator.authenticate(any(), any(), any())
        } returns BiometricAuthenticator.BiometricResult.Canceled

        val exception = try {
            signingManager.signChallengeResponse(
                challengeId = "test-challenge-id",
                challengeBytes = byteArrayOf(1, 2, 3),
                decision = "approved",
                keyAlias = "test-key"
            )
            null
        } catch (e: SigningManager.SigningException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception?.message?.contains("canceled") == true)
    }

    @Test
    fun `signChallengeResponse throws when biometric locked out`() = runTest {
        // Mock biometric lockout
        coEvery {
            biometricAuthenticator.authenticate(any(), any(), any())
        } returns BiometricAuthenticator.BiometricResult.Lockout

        val exception = try {
            signingManager.signChallengeResponse(
                challengeId = "test-challenge-id",
                challengeBytes = byteArrayOf(1, 2, 3),
                decision = "approved",
                keyAlias = "test-key"
            )
            null
        } catch (e: SigningManager.SigningException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception?.message?.contains("lockout") == true)
    }

    @Test
    fun `signChallengeResponse throws when key not found`() = runTest {
        // Mock successful biometric
        coEvery {
            biometricAuthenticator.authenticate(any(), any(), any())
        } returns BiometricAuthenticator.BiometricResult.Success

        // Mock missing key
        every { keystoreManager.getPrivateKey(any()) } returns null

        val exception = try {
            signingManager.signChallengeResponse(
                challengeId = "test-challenge-id",
                challengeBytes = byteArrayOf(1, 2, 3),
                decision = "approved",
                keyAlias = "missing-key"
            )
            null
        } catch (e: SigningManager.SigningException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception?.message?.contains("not found") == true)
    }
}
