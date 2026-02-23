package com.antigravity.cryptowallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Premium Utility-First Color Palette
val PrimaryColor = Color(0xFF0F52BA) // Deep Sapphire
val SurfaceColor = Color(0xFFFFFFFF)
val BackgroundColor = Color(0xFFF9FAFB)
val TextPrimary = Color(0xFF111827)
val TextSecondary = Color(0xFF6B7280)
val OutlineColor = Color(0xFFE5E7EB)

val SuccessColor = Color(0xFF10B981)
val ErrorColor = Color(0xFFEF4444)
val WarningColor = Color(0xFFF59E0B)

val PrimaryVariant = PrimaryColor
val SecondaryVariant = PrimaryColor
val AccentColor = PrimaryColor
val BackgroundDark = Color(0xFF111827)
val SurfaceDark = Color(0xFF1F2937)
val CardDark = Color(0xFF374151)

// Legacy Brutalist Colors (Kept for compatibility)
val BrutalBlack = Color(0xFF111827)
val BrutalWhite = Color(0xFFFFFFFF)

enum class ThemeType {
    DEFAULT, DARK, MIDNIGHT, OCEAN, FOREST, CRIMSON, VIOLET, SUNSET
}

private val LightScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEFF6FF),
    onPrimaryContainer = Color(0xFF1E40AF),
    secondary = PrimaryColor,
    onSecondary = Color.White,
    background = BackgroundColor,
    onBackground = TextPrimary,
    surface = SurfaceColor,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = TextSecondary,
    outline = OutlineColor,
    error = ErrorColor,
    onError = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF3B82F6),
    onSecondary = Color.White,
    background = BackgroundDark,
    onBackground = Color(0xFFF9FAFB),
    surface = SurfaceDark,
    onSurface = Color(0xFFF9FAFB),
    surfaceVariant = CardDark,
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF4B5563),
    error = Color(0xFFF87171),
    onError = Color.White
)

val CleanTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
)

@Composable
fun CryptoWalletTheme(
    themeType: ThemeType = ThemeType.DEFAULT,
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We map all legacy themes to our utility first scheme
    val colorScheme = if (darkTheme) DarkScheme else LightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CleanTypography,
        content = content
    )
}
