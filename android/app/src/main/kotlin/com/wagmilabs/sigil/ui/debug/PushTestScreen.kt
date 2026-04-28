package com.wagmilabs.sigil.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

/**
 * Debug-only screen for simulating push notifications without relay
 * Per task: "write a test mode that lets QA simulate a push without needing the real relay"
 */
@Composable
fun PushTestScreen(
    onDismiss: () -> Unit
) {
    var jsonPayload by remember {
        mutableStateOf(
            """
{
  "challenge_id": "ch_test_${UUID.randomUUID().toString().take(8)}",
  "server_name": "test.sigilauth.com",
  "server_fingerprint": "fp_${UUID.randomUUID().toString().take(12)}",
  "action": "login",
  "action_description": "Sign in to Dashboard",
  "metadata": {
    "ip": "192.168.1.100",
    "user_agent": "Mozilla/5.0",
    "location": "Brisbane, AU"
  },
  "expires_at": "${Instant.now().plusSeconds(300)}"
}
""".trimIndent()
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showApproval by remember { mutableStateOf(false) }
    var parsedChallenge by remember { mutableStateOf<TestChallenge?>(null) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Push Test") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Paste JSON challenge payload below",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = jsonPayload,
                onValueChange = {
                    jsonPayload = it
                    errorMessage = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    errorMessage = null
                    try {
                        val json = Json { ignoreUnknownKeys = true }
                        val decoded = json.decodeFromString<TestChallenge>(jsonPayload)
                        parsedChallenge = decoded
                        showApproval = true
                    } catch (e: Exception) {
                        errorMessage = "JSON parse error: ${e.message}"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simulate Push")
            }
        }
    }

    if (showApproval && parsedChallenge != null) {
        TestApprovalDialog(
            challenge = parsedChallenge!!,
            onDismiss = { showApproval = false }
        )
    }
}

@Serializable
data class TestChallenge(
    val challenge_id: String,
    val server_name: String,
    val server_fingerprint: String,
    val action: String,
    val action_description: String,
    val metadata: Map<String, String>,
    val expires_at: String
)

@Composable
fun TestApprovalDialog(
    challenge: TestChallenge,
    onDismiss: () -> Unit
) {
    var result by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Test Approval") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Server info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Server",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            challenge.server_name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Action info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Action",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            challenge.action_description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Metadata
                if (challenge.metadata.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Metadata",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            challenge.metadata.forEach { (key, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        key,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        value,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                if (result != null) {
                    Text(
                        result!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result!!.startsWith("✅")) {
                            Color(0xFF00E676)
                        } else {
                            Color(0xFFFF5A5A)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    result = "✅ Challenge approved (test mode - no crypto)"
                    kotlinx.coroutines.MainScope().launch {
                        kotlinx.coroutines.delay(1500)
                        onDismiss()
                    }
                }
            ) {
                Text("Approve")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    result = "❌ Challenge denied"
                    kotlinx.coroutines.MainScope().launch {
                        kotlinx.coroutines.delay(1000)
                        onDismiss()
                    }
                }
            ) {
                Text("Deny")
            }
        }
    )
}
