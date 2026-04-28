package com.wagmilabs.sigil.ui.pairing

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wagmilabs.sigil.core.crypto.SessionPictogram

@Composable
fun SessionPictogramView(
    pictogram: SessionPictogram,
    timeRemaining: Int,
    onConfirm: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpired by remember { mutableStateOf(false) }

    LaunchedEffect(timeRemaining) {
        if (timeRemaining <= 0 && !isExpired) {
            isExpired = true
            onDeny()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .widthIn(max = 600.dp)
            .semantics {
                contentDescription = "Session pictogram verification. Verify the six emoji-word pairs match what the server displays, then confirm or deny"
            },
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Confirm Server Identity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Verify this pictogram matches what the server displays:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        PictogramGrid(
            pictogram = pictogram,
            modifier = Modifier.fillMaxWidth()
        )

        SpeakableTextSection(
            speakableText = pictogram.speakable,
            modifier = Modifier.fillMaxWidth()
        )

        TimerSection(
            timeRemaining = timeRemaining,
            modifier = Modifier.fillMaxWidth()
        )

        ButtonsSection(
            onDeny = onDeny,
            onConfirm = onConfirm,
            isExpired = isExpired,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PictogramGrid(
    pictogram: SessionPictogram,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            for (row in 0..1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0..2) {
                        val index = row * 3 + col
                        if (index < pictogram.emojis.size) {
                            EmojiWordPair(
                                emoji = pictogram.emojis[index],
                                name = pictogram.names[index],
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiWordPair(
    emoji: String,
    name: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = name
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = emoji,
            fontSize = 48.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SpeakableTextSection(
    speakableText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Speakable format:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Text(
                text = speakableText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun TimerSection(
    timeRemaining: Int,
    modifier: Modifier = Modifier
) {
    val isWarning = timeRemaining <= 3
    val timerColor = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Expires in $timeRemaining seconds"
        },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isWarning) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = timerColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = "Expires in ${timeRemaining}s",
            style = MaterialTheme.typography.labelMedium,
            color = timerColor
        )
    }
}

@Composable
private fun ButtonsSection(
    onDeny: () -> Unit,
    onConfirm: () -> Unit,
    isExpired: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onDeny,
            enabled = !isExpired,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
        ) {
            Text("Deny")
        }

        Button(
            onClick = onConfirm,
            enabled = !isExpired,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
        ) {
            Text("Confirm")
        }
    }
}
