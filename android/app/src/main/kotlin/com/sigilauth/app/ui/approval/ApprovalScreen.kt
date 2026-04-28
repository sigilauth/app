package com.wagmilabs.sigil.ui.approval

import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wagmilabs.sigil.network.models.Action
import com.wagmilabs.sigil.ui.theme.SigilColors
import com.wagmilabs.sigil.ui.theme.SigilSpacing
import timber.log.Timber

/**
 * Challenge approval screen.
 *
 * Per knox-threat-model.md:
 * - Displays action context before biometric prompt
 * - Shows server pictogram for verification
 *
 * Per aria-a11y-requirements.md §2.2:
 * - TalkBack announces action details
 * - Touch targets ≥44dp
 * - Accessible pictogram with speakable text
 *
 * AGPL-3.0 License
 */
@Composable
fun ApprovalScreen(
    challengeId: String,
    serverName: String,
    serverPictogram: List<String>,
    serverPictogramSpeakable: String,
    action: Action,
    expiresAt: String,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    val context = LocalContext.current

    // Prevent screenshots per security requirements
    DisposableEffect(Unit) {
        val window = (context as? androidx.activity.ComponentActivity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    Dialog(
        onDismissRequest = onDeny,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(SigilSpacing.s6)
                    .semantics(mergeDescendants = false) {},
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SigilSpacing.s4)
            ) {
                // Badge
                Text(
                    text = "Authentication Request",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(horizontal = SigilSpacing.s3, vertical = SigilSpacing.s2)
                )

                // Title
                Text(
                    text = "Approve Login?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SigilColors.Text,
                    modifier = Modifier.semantics {
                        contentDescription = "Authentication request from $serverName"
                    }
                )

                Spacer(modifier = Modifier.height(SigilSpacing.s4))

                // Server pictogram (TalkBack accessible)
                PictogramDisplay(
                    pictogram = serverPictogram,
                    pictogramSpeakable = serverPictogramSpeakable,
                    label = "Server pictogram"
                )

                Spacer(modifier = Modifier.height(SigilSpacing.s6))

                // Action details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = SigilColors.Surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(SigilSpacing.s4)
                    ) {
                        Text(
                            text = "Action",
                            style = MaterialTheme.typography.labelMedium,
                            color = SigilColors.TextDim
                        )
                        Text(
                            text = action.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = SigilColors.Text,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .semantics {
                                    contentDescription = "Requested action: ${action.description}"
                                }
                        )

                        // Action parameters if present
                        action.params?.forEach { (key, value) ->
                            Spacer(modifier = Modifier.height(SigilSpacing.s2))
                            Text(
                                text = "$key: $value",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SigilColors.TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(SigilSpacing.s6))

                // Action buttons (≥44dp touch targets per Aria)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SigilSpacing.s3)
                ) {
                    OutlinedButton(
                        onClick = onDeny,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SigilColors.Danger
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .semantics {
                                contentDescription = "Deny authentication request"
                            }
                    ) {
                        Text("Deny")
                    }

                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SigilColors.Primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .semantics {
                                contentDescription = "Approve with biometric authentication"
                            }
                    ) {
                        Text("Approve")
                    }
                }
            }
        }
    }
}

/**
 * Accessible pictogram display component.
 *
 * Per aria-a11y-requirements.md §3.2:
 * - role="img" equivalent
 * - Speakable text for screen readers
 * - Visual emoji + text alternative
 */
@Composable
fun PictogramDisplay(
    pictogram: List<String>,
    pictogramSpeakable: String,
    label: String
) {
    Column(
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                contentDescription = "$label: $pictogramSpeakable"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Emoji display (visual, aria-hidden equivalent)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            pictogram.forEach { emojiName ->
                Text(
                    text = getEmojiForName(emojiName),
                    style = MaterialTheme.typography.displayLarge  // WCAG 1.4.4: scales to 200% (MINOR-AND-1 fix)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Speakable text (always visible per aria-a11y-requirements.md §3.2)
        Text(
            text = pictogramSpeakable,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Maps emoji name to actual emoji character.
 * Matches PictogramDerivation.EMOJI_LIST order.
 */
private fun getEmojiForName(name: String): String {
    return when (name) {
        "apple" -> "🍎"
        "banana" -> "🍌"
        "grapes" -> "🍇"
        "orange" -> "🍊"
        "lemon" -> "🍋"
        "cherry" -> "🍒"
        "strawberry" -> "🍓"
        "kiwi" -> "🥝"
        "carrot" -> "🥕"
        "corn" -> "🌽"
        "broccoli" -> "🥦"
        "mushroom" -> "🍄"
        "pepper" -> "🌶️"
        "avocado" -> "🥑"
        "onion" -> "🧅"
        "peanut" -> "🥜"
        "pizza" -> "🍕"
        "burger" -> "🍔"
        "taco" -> "🌮"
        "donut" -> "🍩"
        "cookie" -> "🍪"
        "cake" -> "🎂"
        "cupcake" -> "🧁"
        "popcorn" -> "🍿"
        "car" -> "🚗"
        "taxi" -> "🚕"
        "bus" -> "🚌"
        "rocket" -> "🚀"
        "plane" -> "✈️"
        "helicopter" -> "🚁"
        "sailboat" -> "⛵"
        "bicycle" -> "🚲"
        "dog" -> "🐕"
        "cat" -> "🐈"
        "fish" -> "🐟"
        "butterfly" -> "🦋"
        "bee" -> "🐝"
        "fox" -> "🦊"
        "lion" -> "🦁"
        "elephant" -> "🐘"
        "tree" -> "🌲"
        "sunflower" -> "🌻"
        "cactus" -> "🌵"
        "clover" -> "🍀"
        "blossom" -> "🌸"
        "rainbow" -> "🌈"
        "star" -> "⭐"
        "moon" -> "🌙"
        "house" -> "🏠"
        "mountain" -> "🏔️"
        "peak" -> "⛰️"
        "volcano" -> "🌋"
        "island" -> "🏝️"
        "moai" -> "🗿"
        "tent" -> "⛺"
        "castle" -> "🏰"
        "key" -> "🔑"
        "bell" -> "🔔"
        "books" -> "📚"
        "guitar" -> "🎸"
        "anchor" -> "⚓"
        "crown" -> "👑"
        "diamond" -> "💎"
        "fire" -> "🔥"
        else -> "❓"
    }
}
