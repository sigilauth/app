package com.wagmilabs.sigil.ui.approval

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wagmilabs.sigil.R
import com.wagmilabs.sigil.network.models.Action
import java.text.SimpleDateFormat
import java.util.*

/**
 * Challenge approval screen per Aria §2.2
 * REQ: Action context display before biometric gate
 * REQ: 48x48dp touch targets, TalkBack announcements
 */
@Composable
fun ApprovalView(
    challenge: ChallengePayload,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Badge
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                stringResource(R.string.challenge_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Title
        Text(
            stringResource(R.string.challenge_approve_login_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        // Timestamp
        Text(
            formatTimestamp(challenge.expiresAt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Server card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.challenge_service_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    challenge.serverName,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Action card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.challenge_action_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    challenge.actionDescription,
                    style = MaterialTheme.typography.bodyLarge
                )

                // Parameters display
                challenge.actionParams?.forEach { (key, value) ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "$key:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(value)
                    }
                }
            }
        }

        // View Details button - shown when detailsUrl is present
        challenge.detailsUrl?.let { urlString ->
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.challenge_view_details_button))
            }
        }

        TimeRemainingView(expiresAt = challenge.expiresAt)

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons - 48dp minimum per Material accessibility
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onReject,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.btn_reject))
            }

            Button(
                onClick = onApprove,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.btn_approve))
            }
        }
    }
}

@Composable
fun TimeRemainingView(expiresAt: Date) {
    var timeRemaining by remember { mutableStateOf("") }

    LaunchedEffect(expiresAt) {
        while (true) {
            val remaining = (expiresAt.time - System.currentTimeMillis()) / 1000

            timeRemaining = when {
                remaining <= 0 -> "expired"
                remaining > 60 -> {
                    val minutes = (remaining / 60).toInt()
                    "$minutes minute${if (minutes == 1L) "" else "s"}"
                }
                else -> "$remaining second${if (remaining == 1L) "" else "s"}"
            }

            if (remaining <= 0) break
            kotlinx.coroutines.delay(1000)
        }
    }

    Text(
        "Expires: $timeRemaining",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// Placeholder - replace with real type when server integration complete
data class ChallengePayload(
    val serverName: String,
    val actionDescription: String,
    val actionParams: Map<String, String>? = null,
    val expiresAt: Date,
    val detailsUrl: String? = null  // Optional URL to view action details in external browser
)

private fun formatTimestamp(date: Date): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(date)
}
