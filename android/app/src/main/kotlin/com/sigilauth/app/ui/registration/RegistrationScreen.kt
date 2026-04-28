package com.wagmilabs.sigil.ui.registration

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.wagmilabs.sigil.ui.theme.SigilColors
import com.wagmilabs.sigil.ui.theme.SigilSpacing

/**
 * Device registration screen.
 *
 * Supports:
 * - QR code scanning (CameraX + ML Kit)
 * - 8-digit pairing code entry
 * - Manual server URL entry
 *
 * Per aria-a11y-requirements.md §2.1:
 * - TalkBack accessible inputs
 * - Error messages with field + problem + solution
 *
 * AGPL-3.0 License
 */
@Composable
fun RegistrationScreen(
    onScanQR: () -> Unit,
    onPairingCodeEntered: (String) -> Unit,
    onManualEntry: (String) -> Unit,
    onBack: () -> Unit
) {
    var pairingCode by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Add Server") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(SigilSpacing.s6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SigilSpacing.s6)
        ) {
            Text(
                text = "Pair Device",
                style = MaterialTheme.typography.titleLarge,
                color = SigilColors.Text,
                modifier = Modifier.semantics {
                    heading()
                }
            )

            Text(
                text = "Choose a registration method",
                style = MaterialTheme.typography.bodyMedium,
                color = SigilColors.TextMuted
            )

            Spacer(modifier = Modifier.height(SigilSpacing.s4))

            // QR Code option
            OutlinedCard(
                onClick = onScanQR,
                colors = CardDefaults.outlinedCardColors(
                    containerColor = SigilColors.Surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = "Scan QR code for quick registration"
                        role = Role.Button
                    }
            ) {
                Column(
                    modifier = Modifier.padding(SigilSpacing.s5)
                ) {
                    Text(
                        text = "Scan QR Code",
                        style = MaterialTheme.typography.titleMedium,
                        color = SigilColors.Text
                    )
                    Text(
                        text = "Fastest method - scan the QR code from your setup page",
                        style = MaterialTheme.typography.bodySmall,
                        color = SigilColors.TextMuted
                    )
                }
            }

            // 8-digit pairing code
            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(
                    containerColor = SigilColors.Surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(SigilSpacing.s5)
                ) {
                    Text(
                        text = "Enter Pairing Code",
                        style = MaterialTheme.typography.titleMedium,
                        color = SigilColors.Text
                    )
                    Spacer(modifier = Modifier.height(SigilSpacing.s3))

                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = { newValue ->
                            // Only allow digits, max 8
                            if (newValue.length <= 8 && newValue.all { it.isDigit() }) {
                                pairingCode = newValue
                                errorMessage = null

                                // Auto-submit on 8 digits
                                if (newValue.length == 8) {
                                    onPairingCodeEntered(newValue)
                                }
                            }
                        },
                        label = { Text("8-digit code") },
                        placeholder = { Text("12345678") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        isError = errorMessage != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Enter 8-digit pairing code from your setup page"
                            }
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = SigilColors.Danger,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Manual entry
            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(
                    containerColor = SigilColors.Surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(SigilSpacing.s5)
                ) {
                    Text(
                        text = "Manual Entry",
                        style = MaterialTheme.typography.titleMedium,
                        color = SigilColors.Text
                    )
                    Spacer(modifier = Modifier.height(SigilSpacing.s3))

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("Server URL") },
                        placeholder = { Text("https://sigil.example.com") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Enter server URL manually"
                            }
                    )

                    Spacer(modifier = Modifier.height(SigilSpacing.s3))

                    Button(
                        onClick = { onManualEntry(serverUrl) },
                        enabled = serverUrl.startsWith("https://"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SigilColors.Primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                    ) {
                        Text("Connect")
                    }
                }
            }
        }
    }
}
