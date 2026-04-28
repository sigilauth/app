package com.wagmilabs.sigil.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Sigil Auth design tokens per iris-design-direction.md
 *
 * Primary: #0052FF (Trust blue)
 * Danger: #E53935
 * Success: #00C853
 * Warning: #FFB300
 */

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0052FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4D88FF),
    onPrimaryContainer = Color(0xFF001A52),

    secondary = Color(0xFF5A5766),
    onSecondary = Color.White,

    error = Color(0xFFE53935),
    onError = Color.White,

    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF0F0E17),

    surface = Color.White,
    onSurface = Color(0xFF0F0E17),
    surfaceVariant = Color(0xFFF7F7F8),
    onSurfaceVariant = Color(0xFF5A5766),

    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFBDBDBD)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0052FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0040CC),
    onPrimaryContainer = Color(0xFF4D88FF),

    secondary = Color(0xFFB3B3B3),
    onSecondary = Color(0xFF0F0E17),

    error = Color(0xFFE53935),
    onError = Color.White,

    background = Color(0xFF0F0E17),
    onBackground = Color(0xFFFAFAFA),

    surface = Color(0xFF1A1A1E),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF252529),
    onSurfaceVariant = Color(0xFFB3B3B3),

    outline = Color(0xFF3A3A3E),
    outlineVariant = Color(0xFF4F4F54)
)

@Composable
fun SigilAuthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
