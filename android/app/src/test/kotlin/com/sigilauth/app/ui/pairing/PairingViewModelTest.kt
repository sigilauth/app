package com.wagmilabs.sigil.ui.pairing

import com.wagmilabs.sigil.network.SigilApiService
import com.wagmilabs.sigil.network.models.PairingRedeemRequest
import com.wagmilabs.sigil.network.models.PairingRedeemResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Unit tests for PairingViewModel.
 *
 * Tests:
 * - Valid code redemption success
 * - Invalid code (400) decrements attempts
 * - 3 failed attempts trigger lockout
 * - Non-8-digit code validation
 * - Network error handling
 *
 * AGPL-3.0 License
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PairingViewModelTest {

    private lateinit var apiService: SigilApiService
    private lateinit var viewModel: PairingViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        apiService = mockk()
        viewModel = PairingViewModel(apiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `valid 8-digit code succeeds`() = runTest {
        val mockResponse = PairingRedeemResponse(
            serverUrl = "https://sigil.example.com",
            serverPublicKey = "base64key",
            serverName = "Test Server",
            serverPictogram = listOf("🔒", "🛡️", "🔐", "🗝️", "⚡"),
            serverPictogramSpeakable = "lock shield key oldkey lightning",
            callbackUrl = null,
            sessionToken = "stk_abc123"
        )

        coEvery {
            apiService.redeemPairingCode(PairingRedeemRequest("12345678"))
        } returns Response.success(mockResponse)

        viewModel.redeemCode("12345678")
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state is PairingUiState.Success)
        assertEquals(mockResponse, (state as PairingUiState.Success).response)
        coVerify { apiService.redeemPairingCode(PairingRedeemRequest("12345678")) }
    }

    @Test
    fun `invalid code returns 400 and decrements attempts`() = runTest {
        coEvery {
            apiService.redeemPairingCode(any())
        } returns Response.error(400, mockk(relaxed = true))

        viewModel.redeemCode("11111111")
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state is PairingUiState.Error)
        assertEquals("error-invalid-pairing-code", (state as PairingUiState.Error).message)
        assertEquals(2, viewModel.attemptsRemaining.first())
    }

    @Test
    fun `three failed attempts trigger lockout`() = runTest {
        coEvery {
            apiService.redeemPairingCode(any())
        } returns Response.error(400, mockk(relaxed = true))

        viewModel.redeemCode("11111111")
        advanceUntilIdle()
        assertEquals(2, viewModel.attemptsRemaining.first())

        viewModel.resetState()
        viewModel.redeemCode("22222222")
        advanceUntilIdle()
        assertEquals(1, viewModel.attemptsRemaining.first())

        viewModel.resetState()
        viewModel.redeemCode("33333333")
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state is PairingUiState.LockedOut)
        assertEquals(0, viewModel.attemptsRemaining.first())
    }

    @Test
    fun `non-8-digit code returns error immediately`() = runTest {
        viewModel.redeemCode("123")
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state is PairingUiState.Error)
        assertEquals("error-invalid-pairing-code", (state as PairingUiState.Error).message)
        coVerify(exactly = 0) { apiService.redeemPairingCode(any()) }
    }

    @Test
    fun `non-numeric code returns error immediately`() = runTest {
        viewModel.redeemCode("1234abcd")
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state is PairingUiState.Error)
        coVerify(exactly = 0) { apiService.redeemPairingCode(any()) }
    }

    @Test
    fun `404 response indicates expired code`() = runTest {
        coEvery {
            apiService.redeemPairingCode(any())
        } returns Response.error(404, mockk(relaxed = true))

        viewModel.redeemCode("12345678")
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state is PairingUiState.Error)
        assertEquals("error-pairing-code-expired", (state as PairingUiState.Error).message)
    }

    @Test
    fun `409 response indicates already used code`() = runTest {
        coEvery {
            apiService.redeemPairingCode(any())
        } returns Response.error(409, mockk(relaxed = true))

        viewModel.redeemCode("12345678")
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state is PairingUiState.Error)
        assertEquals("error-pairing-code-used", (state as PairingUiState.Error).message)
        assertFalse((state as PairingUiState.Error).canRetry)
    }

    @Test
    fun `429 response triggers immediate lockout`() = runTest {
        coEvery {
            apiService.redeemPairingCode(any())
        } returns Response.error(429, mockk(relaxed = true))

        viewModel.redeemCode("12345678")
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state is PairingUiState.LockedOut)
    }

    @Test
    fun `network exception decrements attempts`() = runTest {
        coEvery {
            apiService.redeemPairingCode(any())
        } throws Exception("Network error")

        viewModel.redeemCode("12345678")
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state is PairingUiState.Error)
        assertEquals("error-network", (state as PairingUiState.Error).message)
        assertEquals(2, viewModel.attemptsRemaining.first())
    }

    @Test
    fun `resetAttempts restores 3 attempts and idle state`() = runTest {
        coEvery {
            apiService.redeemPairingCode(any())
        } returns Response.error(400, mockk(relaxed = true))

        viewModel.redeemCode("11111111")
        advanceUntilIdle()
        assertEquals(2, viewModel.attemptsRemaining.first())

        viewModel.resetAttempts()

        assertEquals(3, viewModel.attemptsRemaining.first())
        assertTrue(viewModel.uiState.first() is PairingUiState.Idle)
    }
}
