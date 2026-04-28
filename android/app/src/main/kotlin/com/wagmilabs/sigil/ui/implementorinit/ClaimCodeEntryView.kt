package com.wagmilabs.sigil.ui.implementorinit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wagmilabs.sigil.core.utilities.ClaimCodeValidator

@Composable
fun ClaimCodeEntryView(
    characters: List<String>,
    onCharacterChange: (Int, String) -> Unit,
    onClaim: () -> Void,
    onScanQR: () -> Unit
) {
    val focusRequesters = remember { List(6) { FocusRequester() } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Enter Claim Code",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Enter the 6-character code from your browser",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0..5) {
                OutlinedTextField(
                    value = characters[i],
                    onValueChange = { value ->
                        val filtered = value.uppercase().filter { char ->
                            ClaimCodeValidator.isValid(char.toString())
                        }
                        onCharacterChange(i, filtered.take(1))
                    },
                    modifier = Modifier
                        .width(50.dp)
                        .focusRequester(focusRequesters[i]),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii
                    )
                )

                if (i == 2) {
                    Text(
                        "-",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Button(
            onClick = onClaim,
            enabled = characters.all { it.isNotEmpty() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Claim")
        }

        HorizontalDivider()

        OutlinedButton(onClick = onScanQR) {
            Icon(Icons.Default.QrCode, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan QR Code Instead")
        }
    }

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }
}
