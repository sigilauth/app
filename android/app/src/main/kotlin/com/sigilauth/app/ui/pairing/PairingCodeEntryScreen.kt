package com.wagmilabs.sigil.ui.pairing

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.wagmilabs.sigil.ui.theme.SigilColors
import com.wagmilabs.sigil.ui.theme.SigilSpacing
import com.wagmilabs.sigil.ui.theme.SigilRadius
import kotlinx.coroutines.delay

/**
 * 8-digit pairing code entry screen with enhanced UX.
 *
 * Features:
 * - Individual digit boxes with auto-advance
 * - Paste support from clipboard
 * - TalkBack accessible (WCAG 2.2 AA per Aria §10)
 * - 3-attempt lockout UI
 * - Visual feedback per digit
 *
 * Per protocol-spec.md §2350-2377:
 * - 8-digit numeric code
 * - 3 attempts max
 * - Single-use, IP-bound
 *
 * AGPL-3.0 License
 */
@Composable
fun PairingCodeEntryScreen(
    uiState: PairingUiState,
    attemptsRemaining: Int,
    onCodeEntered: (String) -> Unit,
    onRetry: () -> Unit,
    onSuccess: (PairingRedeemResponse) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var code by remember { mutableStateOf(List(8) { "" }) }
    val focusRequesters = remember { List(8) { FocusRequester() } }

    LaunchedEffect(uiState) {
        if (uiState is PairingUiState.Success) {
            delay(500)
            onSuccess(uiState.response)
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Enter Pairing Code") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(SigilSpacing.s6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SigilSpacing.s6)
        ) {
            Text(
                text = "Enter Pairing Code",
                fontSize = 28.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = SigilColors.Text
            )

            Text(
                text = "8-digit code from the service",
                style = MaterialTheme.typography.bodyMedium,
                color = SigilColors.TextMuted,
                textAlign = TextAlign.Center
            )

            if (attemptsRemaining < 3 && uiState !is PairingUiState.LockedOut) {
                Text(
                    text = "$attemptsRemaining ${if (attemptsRemaining == 1) "attempt" else "attempts"} remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = SigilColors.Danger
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState) {
                is PairingUiState.LockedOut -> {
                    LockedOutView(onRetry = onRetry)
                }
                else -> {
                    CodeInputRow(
                        code = code,
                        focusRequesters = focusRequesters,
                        enabled = uiState !is PairingUiState.Loading,
                        onCodeChange = { newCode ->
                            code = newCode
                            if (newCode.all { it.isNotEmpty() }) {
                                val fullCode = newCode.joinToString("")
                                onCodeEntered(fullCode)
                            }
                        },
                        onPaste = { pastedCode ->
                            if (pastedCode.length == 8 && pastedCode.all { it.isDigit() }) {
                                code = pastedCode.map { it.toString() }
                                onCodeEntered(pastedCode)
                            }
                        }
                    )

                    if (uiState is PairingUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(16.dp)
                                .semantics { contentDescription = "Verifying pairing code" }
                        )
                    }

                    if (uiState is PairingUiState.Error) {
                        Text(
                            text = getErrorMessage(uiState.message),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        if (uiState.canRetry) {
                            TextButton(onClick = {
                                code = List(8) { "" }
                                onRetry()
                            }) {
                                Text("Try Again")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val clipboardManager = ContextCompat.getSystemService(
                                context,
                                ClipboardManager::class.java
                            )
                            val clipText = clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            if (clipText.length == 8 && clipText.all { it.isDigit() }) {
                                code = clipText.map { it.toString() }
                                onCodeEntered(clipText)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Paste from Clipboard")
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeInputRow(
    code: List<String>,
    focusRequesters: List<FocusRequester>,
    enabled: Boolean,
    onCodeChange: (List<String>) -> Unit,
    onPaste: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "8-digit pairing code entry, ${code.count { it.isNotEmpty() }} of 8 digits entered"
        }
    ) {
        code.forEachIndexed { index, digit ->
            OutlinedTextField(
                value = digit,
                onValueChange = { newValue ->
                    if (newValue.length <= 1 && (newValue.isEmpty() || newValue[0].isDigit())) {
                        val newCode = code.toMutableList()
                        newCode[index] = newValue
                        onCodeChange(newCode)

                        if (newValue.isNotEmpty() && index < 7) {
                            focusRequesters[index + 1].requestFocus()
                        }
                    } else if (newValue.length == 8 && newValue.all { it.isDigit() }) {
                        onPaste(newValue)
                    }
                },
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                modifier = Modifier
                    .widthIn(min = 44.dp)  // WCAG 2.5.8: minimum 44dp width (MAJOR-AND-1 fix)
                    .heightIn(min = 56.dp)
                    .focusRequester(focusRequesters[index])
                    .border(
                        width = if (digit.isNotEmpty()) 2.dp else 1.dp,
                        color = if (digit.isNotEmpty()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = MaterialTheme.shapes.small
                    )
                    .semantics {
                        contentDescription = "Digit ${index + 1} of 8, ${if (digit.isEmpty()) "empty" else "filled"}"
                    }
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }
}

@Composable
private fun LockedOutView(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(24.dp)
    ) {
        Text(
            text = "Too many attempts",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = "Request a new pairing code from the setup page.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Get New Code")
        }
    }
}

private fun getErrorMessage(errorKey: String): String {
    return when (errorKey) {
        "error-invalid-pairing-code" -> "That code didn't work. Check the code and try again."
        "error-pairing-code-expired" -> "This code has expired. Get a new one from the setup page."
        "error-pairing-code-used" -> "This pairing code has already been used."
        "error-pairing-code-attempts" -> "Too many attempts. Request a new code."
        "error-network" -> "No connection. Check your network and try again."
        "error-server" -> "Something went wrong on the server. Please try again."
        else -> "An error occurred. Please try again."
    }
}
