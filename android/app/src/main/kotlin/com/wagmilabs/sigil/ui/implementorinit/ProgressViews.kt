package com.wagmilabs.sigil.ui.implementorinit

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun GeneratingView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(24.dp)
    ) {
        CircularProgressIndicator()
        Text("Generating Mnemonic...", style = MaterialTheme.typography.titleMedium)
        Text(
            "Creating a secure 24-word recovery phrase",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EncryptingSubmittingView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(24.dp)
    ) {
        CircularProgressIndicator()
        Text("Encrypting & Submitting...", style = MaterialTheme.typography.titleMedium)
        Text(
            "Securing mnemonic for implementor",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AwaitingConfirmationView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color.Green,
            modifier = Modifier.size(64.dp)
        )
        Text("Submitted", style = MaterialTheme.typography.titleLarge)
        Text(
            "Waiting for implementor to confirm receipt...",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CircularProgressIndicator()
    }
}

@Composable
fun ClaimingView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(24.dp)
    ) {
        CircularProgressIndicator()
        Text("Claiming Request...", style = MaterialTheme.typography.titleMedium)
        Text(
            "Verifying claim code",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AwaitingPushView(timeRemaining: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(24.dp)
    ) {
        CircularProgressIndicator()
        Text("Waiting for Request...", style = MaterialTheme.typography.titleMedium)
        Text(
            "Your claim was successful. Waiting for the mnemonic generation request.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (timeRemaining < 30) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AccessTime, contentDescription = null)
                Text("Timeout in ${timeRemaining}s")
            }
        }
    }
}
