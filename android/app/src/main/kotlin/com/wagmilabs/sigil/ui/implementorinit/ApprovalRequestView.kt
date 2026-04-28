package com.wagmilabs.sigil.ui.implementorinit

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wagmilabs.sigil.network.models.ImplementorInitRequest
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ApprovalRequestView(
    request: ImplementorInitRequest,
    timeRemaining: Int,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Key,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Mnemonic Request",
            style = MaterialTheme.typography.titleLarge
        )

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow("Implementor:", request.implementorName)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Identifier:", request.implementorId)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Requested:", formatDate(request.timestamp))
            }
        }

        Text(
            text = "A new 24-word mnemonic will be generated and sent to the implementor. You will need to write it down before submission.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (timeRemaining < 60) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AccessTime, contentDescription = null)
                Text("Expires in ${timeRemaining}s")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onReject) { Text("Reject") }
            Button(onClick = onApprove) { Text("Approve") }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Medium)
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MM/dd/yy hh:mm a", Locale.getDefault())
    return formatter.format(date)
}
