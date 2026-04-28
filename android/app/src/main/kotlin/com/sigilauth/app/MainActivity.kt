package com.wagmilabs.sigil

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wagmilabs.sigil.ui.theme.SigilAuthTheme
import timber.log.Timber

/**
 * Main activity for Sigil Auth Android app.
 *
 * Handles:
 * - App Links (verified HTTPS: https://sigilauth.com)
 * - Push notification taps
 *
 * AGPL-3.0 License
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle deep links
        handleIntent(intent)

        setContent {
            SigilAuthTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // TODO: Replace with navigation graph once B0 (OpenAPI) ships
                    PlaceholderScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    handleDeepLink(uri)
                }
            }
        }
    }

    private fun handleDeepLink(uri: Uri) {
        Timber.d("Deep link received: $uri")

        if (uri.scheme != "https" || uri.host != "sigilauth.com") {
            Timber.w("Invalid deep link scheme or host: $uri")
            return
        }

        when (uri.path) {
            "/register" -> {
                // TODO: Navigate to registration flow (after B0)
                Timber.d("Registration deep link: $uri")
            }
            "/mnemonic-init" -> {
                // TODO: Navigate to mnemonic generation flow (after B0)
                Timber.d("Mnemonic init deep link: $uri")
            }
            "/challenge" -> {
                // TODO: Navigate to challenge approval (after B0)
                Timber.d("Challenge deep link: $uri")
            }
            else -> {
                Timber.w("Unknown deep link path: ${uri.path}")
            }
        }
    }
}

@Composable
fun PlaceholderScreen() {
    Text("Sigil Auth - Scaffolding Complete\n\nWaiting for B0 (OpenAPI spec)")
}

@Preview(showBackground = true)
@Composable
fun PlaceholderScreenPreview() {
    SigilAuthTheme {
        PlaceholderScreen()
    }
}
