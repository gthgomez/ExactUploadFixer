package com.exactuploadfixer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand palette ─────────────────────────────────────────────────────────────
// Primary: cold sky blue — fresh, calm, utility-first
// Tertiary: success green — used only for confirmed pass states
// Surfaces: cool off-white with clean white panels and subtle blue tint

private val LightColorScheme = lightColorScheme(
    primary                = Color(0xFF2F80ED),
    onPrimary              = Color(0xFFFFFFFF),
    primaryContainer       = Color(0xFFD8E9FF),
    onPrimaryContainer     = Color(0xFF001B3B),
    secondary              = Color(0xFF51657F),
    onSecondary            = Color(0xFFFFFFFF),
    secondaryContainer     = Color(0xFFDCE7F4),
    onSecondaryContainer   = Color(0xFF0E1C2E),
    tertiary               = Color(0xFF1A7A2E),   // Success green — accessible on white
    onTertiary             = Color(0xFFFFFFFF),
    tertiaryContainer      = Color(0xFFC1EFC9),   // Light success tint for bg panels
    onTertiaryContainer    = Color(0xFF00210A),
    error                  = Color(0xFFBA1A1A),
    onError                = Color(0xFFFFFFFF),
    errorContainer         = Color(0xFFFFDAD6),
    onErrorContainer       = Color(0xFF410002),
    background             = Color(0xFFF7F9FC),
    onBackground           = Color(0xFF18212B),
    surface                = Color(0xFFFFFFFF),
    onSurface              = Color(0xFF18212B),
    surfaceTint            = Color.Transparent,
    // Supporting/helper text: blue-gray so it recedes clearly below primary blue
    onSurfaceVariant       = Color(0xFF5F6F88),
    surfaceContainerLowest = Color(0xFFFDFEFF),
    surfaceContainerLow    = Color(0xFFF2F8FF),   // Semantic alias: TrustStrip
    surfaceContainer       = Color(0xFFEAF2FB),
    surfaceContainerHigh   = Color(0xFFE1EBF7),   // Semantic alias: BeforeAfterSummary
    surfaceContainerHighest= Color(0xFFD7E4F2),
    outline                = Color(0xFF74859D),
    outlineVariant         = Color(0xFFE5E7EB),
)

private val DarkColorScheme = darkColorScheme(
    primary                = Color(0xFF8BC3FF),
    onPrimary              = Color(0xFF002D52),
    primaryContainer       = Color(0xFF00497D),
    onPrimaryContainer     = Color(0xFFD8E9FF),
    secondary              = Color(0xFFBBC9DC),
    onSecondary            = Color(0xFF223246),
    secondaryContainer     = Color(0xFF344459),
    onSecondaryContainer   = Color(0xFFDCE7F4),
    tertiary               = Color(0xFF7EDA94),   // Success green dark — passes WCAG on dark bg
    onTertiary             = Color(0xFF003913),
    tertiaryContainer      = Color(0xFF00531F),
    onTertiaryContainer    = Color(0xFFB6F0BF),
    error                  = Color(0xFFFFB4AB),
    onError                = Color(0xFF690005),
    errorContainer         = Color(0xFF93000A),
    onErrorContainer       = Color(0xFFFFDAD6),
    background             = Color(0xFF0E141C),
    onBackground           = Color(0xFFE2E8F0),
    surface                = Color(0xFF0E141C),
    onSurface              = Color(0xFFE2E8F0),
    surfaceTint            = Color.Transparent,
    // Blue-gray secondary text — maintains a cool brand hue on dark surfaces
    onSurfaceVariant       = Color(0xFFA4B7D4),
    surfaceContainerLowest = Color(0xFF111924),
    surfaceContainerLow    = Color(0xFF18212B),   // Semantic alias: TrustStrip
    surfaceContainer       = Color(0xFF1D2734),
    surfaceContainerHigh   = Color(0xFF223041),   // Semantic alias: BeforeAfterSummary
    surfaceContainerHighest= Color(0xFF2A3A4D),
    outline                = Color(0xFF8295AE),
    outlineVariant         = Color(0xFF39485C),
)

@Composable
fun ExactUploadFixerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
