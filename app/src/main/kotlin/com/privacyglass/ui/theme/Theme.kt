package com.privacyglass.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Color tokens ──────────────────────────────────────────────────────────────
val PrimaryBlue     = Color(0xFF4B8EF1)
val PrimaryDark     = Color(0xFF1A56B0)
val BgDark          = Color(0xFF0F1117)
val BgCard          = Color(0xFF1A1D27)
val BgCardAlt       = Color(0xFF1A2035)
val TextPrimary     = Color(0xFFFFFFFF)
val TextSecondary   = Color(0xFF8A8FA8)
val SuccessGreen    = Color(0xFF4CAF50)
val WarningOrange   = Color(0xFFFFA726)
val DividerColor    = Color(0xFF2A2D3A)

private val DarkColorScheme = darkColorScheme(
    primary          = PrimaryBlue,
    onPrimary        = Color.White,
    primaryContainer = PrimaryDark,
    secondary        = Color(0xFF6B7AE8),
    background       = BgDark,
    surface          = BgCard,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    outline          = DividerColor,
    error            = Color(0xFFCF6679),
)

@Composable
fun PrivacyGlassTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography(),
        content     = content
    )
}
