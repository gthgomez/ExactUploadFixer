package com.workspace.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Theme-dependent design tokens. Supplied through [LocalGlassColors] so consumers
 * read the tokens for the theme currently in composition instead of a mutable
 * global that could still hold the previous theme's values on the first frame.
 */
@Immutable
data class GlassColors(
    val cardBorder: Color,
    val cardBorderDim: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDim: Color,
    val dividerColor: Color,
    val shimmerEdge: Color,
    val shimmerMid: Color,
    val glowCyanAlpha: Float,
    val glowVioletAlpha: Float,
    val glowTealAlpha: Float,
    val navIndicator: Color,
    val navBorderTop: Color,
)

private val DarkGlassColors = GlassColors(
    cardBorder = Color.White.copy(alpha = 0.20f),
    cardBorderDim = Color.White.copy(alpha = 0.12f),
    textPrimary = Color.White.copy(alpha = 0.95f),
    textSecondary = Color.White.copy(alpha = 0.66f),
    textDim = Color.White.copy(alpha = 0.50f),
    dividerColor = Color.White.copy(alpha = 0.14f),
    shimmerEdge = Color.White.copy(alpha = 0.22f),
    shimmerMid = Color.White.copy(alpha = 0.06f),
    glowCyanAlpha = 0.24f,
    glowVioletAlpha = 0.10f,
    glowTealAlpha = 0.14f,
    navIndicator = Color(0xFF22D3EE).copy(alpha = 0.24f),
    navBorderTop = Color.White.copy(alpha = 0.14f),
)

private val LightGlassColors = GlassColors(
    cardBorder = Color(0xFF9FB0C7).copy(alpha = 0.34f),
    cardBorderDim = Color(0xFF94A3B8).copy(alpha = 0.18f),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF334155),
    textDim = Color(0xFF64748B),
    dividerColor = Color(0xFFCBD5E1).copy(alpha = 0.90f),
    shimmerEdge = Color.White.copy(alpha = 0.42f),
    shimmerMid = Color.White.copy(alpha = 0.12f),
    glowCyanAlpha = 0.14f,
    glowVioletAlpha = 0.05f,
    glowTealAlpha = 0.08f,
    navIndicator = Color(0xFF22D3EE).copy(alpha = 0.18f),
    navBorderTop = Color(0xFF94A3B8).copy(alpha = 0.24f),
)

val LocalGlassColors = staticCompositionLocalOf { DarkGlassColors }

object GlassTokens {
    // Brand accents — theme-independent, safe to read outside composition.
    val Cyan = Color(0xFF06B6D4)
    val CyanBright = Color(0xFF67E8F9)
    val Violet = Color(0xFF7C3AED)
    val VioletLight = Color(0xFFA78BFA)
    val VioletSoft = Color(0xFF8B5CF6)
    val VioletDeep = Color(0xFF4C1D95)
    val Indigo = Color(0xFF6366F1)
    val Teal = Color(0xFF14B8A6)
    val TealDeep = Color(0xFF0F766E)
    val ErrorRed = Color(0xFFFF6B6B)
    val PositiveGreen = Color(0xFF4ADE80)

    // Borders — theme-aware so glass stays crisp in both dark and light modes.
    val CardBorder: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current.cardBorder
    val CardBorderDim: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current.cardBorderDim

    // Text — provided by AppTheme so light mode does not collapse into washed-out white.
    val TextPrimary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current.textPrimary
    val TextSecondary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current.textSecondary
    val TextDim: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current.textDim
    val DividerColor: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current.dividerColor

    // Top-edge shimmer stops for Hero cards
    val ShimmerEdge: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current.shimmerEdge
    val ShimmerMid: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current.shimmerMid

    // Ambient background glow alphas
    val GlowCyanAlpha: Float
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current.glowCyanAlpha
    val GlowVioletAlpha: Float
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current.glowVioletAlpha
    val GlowTealAlpha: Float
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current.glowTealAlpha

    // Nav indicator pill
    val NavIndicator: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current.navIndicator
    val NavBorderTop: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current.navBorderTop
}

private val BrandGlassDark = darkColorScheme(
    primary = GlassTokens.CyanBright,
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF155E75),
    onPrimaryContainer = DarkGlassColors.textPrimary,
    secondary = GlassTokens.Indigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = DarkGlassColors.textPrimary,
    tertiary = GlassTokens.Teal,
    onTertiary = Color(0xFF082F49),
    tertiaryContainer = Color(0xFF134E4A),
    onTertiaryContainer = DarkGlassColors.textPrimary,
    background = Color(0xFF0B1120),
    onBackground = DarkGlassColors.textPrimary,
    surface = Color(0xFF111827),
    onSurface = DarkGlassColors.textPrimary,
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = DarkGlassColors.textSecondary,
    surfaceTint = GlassTokens.CyanBright,
    surfaceContainerLowest = Color(0xFF0F172A),
    surfaceContainerLow = Color(0xFF111827),
    surfaceContainer = Color(0xFF172033),
    surfaceContainerHigh = Color(0xFF1E293B),
    surfaceContainerHighest = Color(0xFF263244),
    outline = Color.White.copy(alpha = 0.20f),
    outlineVariant = Color.White.copy(alpha = 0.10f),
    error = GlassTokens.ErrorRed,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color.White,
    onError = Color.White,
)

private val BrandGlassLight = lightColorScheme(
    primary = GlassTokens.Cyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F8FD),
    onPrimaryContainer = Color(0xFF083344),
    secondary = GlassTokens.Indigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF312E81),
    tertiary = GlassTokens.TealDeep,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD5F7F3),
    onTertiaryContainer = Color(0xFF042F2E),
    background = Color(0xFFF4F7FB),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    surfaceTint = GlassTokens.Cyan,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FBFF),
    surfaceContainer = Color(0xFFF1F5FA),
    surfaceContainerHigh = Color(0xFFE8EEF6),
    surfaceContainerHighest = Color(0xFFDDE6F0),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
    error = GlassTokens.ErrorRed,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337),
    onError = Color.White,
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = if (darkTheme) BrandGlassDark else BrandGlassLight
    val glassColors = if (darkTheme) DarkGlassColors else LightGlassColors

    CompositionLocalProvider(LocalGlassColors provides glassColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
