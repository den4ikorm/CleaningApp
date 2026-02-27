package com.cleaningos.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Custom spacing / radius tokens ───────────────────────────────────────────
data class CleaningSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val cardRadius: Dp = 25.dp,   // "extreme liquid" radius per spec
    val btnRadius: Dp = 25.dp,
    val inputRadius: Dp = 18.dp,
)

val LocalSpacing = staticCompositionLocalOf { CleaningSpacing() }

private val DarkColorScheme = darkColorScheme(
    primary          = CyanMint,
    onPrimary        = OceanDeep,
    primaryContainer = FrostedGlass,
    onPrimaryContainer = TextPrimary,

    secondary        = SkyBlue,
    onSecondary      = OceanDeep,
    secondaryContainer = FrostedGlassDark,
    onSecondaryContainer = TextPrimary,

    tertiary         = SemanticPurple,
    onTertiary       = OceanDeep,

    background       = OceanDeep,
    onBackground     = TextPrimary,

    surface          = FrostedGlass,
    onSurface        = TextPrimary,
    surfaceVariant   = FrostedGlassDark,
    onSurfaceVariant = TextSecondary,

    outline          = GlowCyan35,
    outlineVariant   = GlowCyan20,

    error            = SemanticError,
    onError          = Color.White,

    scrim            = Color(0xCC0F172A),
)

@Composable
fun CleaningOSTheme(
    darkTheme: Boolean = true, // Always dark — Ocean theme
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme   // force dark; ignore system setting per design

    CompositionLocalProvider(
        LocalSpacing provides CleaningSpacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = CleaningTypography,
            content     = content
        )
    }
}
