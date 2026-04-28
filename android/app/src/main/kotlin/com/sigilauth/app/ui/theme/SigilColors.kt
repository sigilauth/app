package com.wagmilabs.sigil.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Sigil Auth design tokens from sigilauth.com
 * Provides consistent color palette and spacing across all screens
 */
object SigilColors {
    // Backgrounds
    val Bg = Color(0xFF07070C)
    val BgRaised = Color(0xFF0E0E16)
    val Surface = Color(0xFF141420)

    // Borders & Dividers
    val Border = Color(0xFF252536)

    // Text
    val Text = Color(0xFFF5F5F7)
    val TextMuted = Color(0xFF9CA0B0)
    val TextDim = Color(0xFF636879)

    // Brand
    val Primary = Color(0xFF4D88FF)
    val Accent = Color(0xFF3DFCE8)

    // Semantic
    val Success = Color(0xFF00E676)
    val Danger = Color(0xFFFF5A5A)
    val Warning = Color(0xFFFFB300)
}

/**
 * Spacing tokens (in dp)
 */
object SigilSpacing {
    val s2 = 8.dp   // 0.5rem
    val s3 = 12.dp  // 0.75rem
    val s4 = 16.dp  // 1rem
    val s6 = 24.dp  // 1.5rem
    val s8 = 32.dp  // 2rem
}

/**
 * Corner radius tokens (in dp)
 */
object SigilRadius {
    val md = 10.dp  // --r-md
    val lg = 14.dp  // --r-lg
}
