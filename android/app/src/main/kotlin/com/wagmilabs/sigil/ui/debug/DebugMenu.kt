package com.wagmilabs.sigil.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.wagmilabs.sigil.BuildConfig

/**
 * Debug menu for testing Firebase Crashlytics and other debug functionality.
 *
 * Only visible in debug builds (BuildConfig.DEBUG).
 *
 * AGPL-3.0 License
 */
@Composable
fun DebugMenu(
    modifier: Modifier = Modifier,
    onShowPushTest: () -> Unit = {}
) {
    if (!BuildConfig.DEBUG) return

    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        TextButton(onClick = { expanded = true }) {
            Text("Debug", color = MaterialTheme.colorScheme.error)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Test Push") },
                onClick = {
                    expanded = false
                    onShowPushTest()
                }
            )
            DropdownMenuItem(
                text = { Text("Test Crash") },
                onClick = {
                    FirebaseCrashlytics.getInstance().log("Test crash triggered by debug menu")
                    throw RuntimeException("Debug crash test")
                }
            )
            DropdownMenuItem(
                text = { Text("Send Test Report") },
                onClick = {
                    FirebaseCrashlytics.getInstance().log("Test report sent by debug menu")
                    FirebaseCrashlytics.getInstance().recordException(Exception("Test exception"))
                    expanded = false
                }
            )
        }
    }
}
