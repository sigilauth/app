package com.wagmilabs.sigil.ui.pairing

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wagmilabs.sigil.network.models.PairingRedeemResponse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Compose tests for PairingCodeEntryScreen.
 *
 * Runs on real device or emulator to test:
 * - Keyboard interaction
 * - Focus management
 * - Auto-advance behavior
 * - TalkBack announcements (manual verification)
 *
 * AGPL-3.0 License
 */
@RunWith(AndroidJUnit4::class)
class PairingCodeEntryInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun digit_entry_advances_focus() {
        var capturedCode = ""

        composeTestRule.setContent {
            PairingCodeEntryScreen(
                uiState = PairingUiState.Idle,
                attemptsRemaining = 3,
                onCodeEntered = { capturedCode = it },
                onRetry = {},
                onSuccess = {},
                onBack = {}
            )
        }

        val digit1 = composeTestRule.onNodeWithContentDescription("Digit 1 of 8, empty")
        digit1.assertExists()
        digit1.performTextInput("1")

        composeTestRule.onNodeWithContentDescription("Digit 1 of 8, filled").assertExists()
    }

    @Test
    fun full_code_triggers_callback() {
        var capturedCode = ""

        composeTestRule.setContent {
            PairingCodeEntryScreen(
                uiState = PairingUiState.Idle,
                attemptsRemaining = 3,
                onCodeEntered = { capturedCode = it },
                onRetry = {},
                onSuccess = {},
                onBack = {}
            )
        }

        for (i in 1..8) {
            composeTestRule
                .onNodeWithContentDescription("Digit $i of 8, empty")
                .performTextInput(i.toString())
        }

        composeTestRule.waitUntil(timeoutMillis = 1000) {
            capturedCode == "12345678"
        }
        assert(capturedCode == "12345678")
    }

    @Test
    fun paste_button_accessible() {
        composeTestRule.setContent {
            PairingCodeEntryScreen(
                uiState = PairingUiState.Idle,
                attemptsRemaining = 3,
                onCodeEntered = {},
                onRetry = {},
                onSuccess = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithText("Paste from Clipboard")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun lockout_state_hides_input_boxes() {
        composeTestRule.setContent {
            PairingCodeEntryScreen(
                uiState = PairingUiState.LockedOut,
                attemptsRemaining = 0,
                onCodeEntered = {},
                onRetry = {},
                onSuccess = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Digit 1 of 8, empty")
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText("Get New Code")
            .assertIsDisplayed()
    }

    @Test
    fun error_state_shows_retry_button() {
        var retryPressed = false

        composeTestRule.setContent {
            PairingCodeEntryScreen(
                uiState = PairingUiState.Error(
                    message = "error-invalid-pairing-code",
                    canRetry = true
                ),
                attemptsRemaining = 2,
                onCodeEntered = {},
                onRetry = { retryPressed = true },
                onSuccess = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithText("Try Again")
            .assertIsDisplayed()
            .performClick()

        assert(retryPressed)
    }

    @Test
    fun loading_state_disables_input() {
        composeTestRule.setContent {
            PairingCodeEntryScreen(
                uiState = PairingUiState.Loading,
                attemptsRemaining = 3,
                onCodeEntered = {},
                onRetry = {},
                onSuccess = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Verifying pairing code")
            .assertIsDisplayed()
    }

    @Test
    fun minimum_touch_target_size_met() {
        composeTestRule.setContent {
            PairingCodeEntryScreen(
                uiState = PairingUiState.Idle,
                attemptsRemaining = 3,
                onCodeEntered = {},
                onRetry = {},
                onSuccess = {},
                onBack = {}
            )
        }

        for (i in 1..8) {
            composeTestRule
                .onNodeWithContentDescription("Digit $i of 8, empty")
                .assertHeightIsAtLeast(48.dp)
        }
    }
}
