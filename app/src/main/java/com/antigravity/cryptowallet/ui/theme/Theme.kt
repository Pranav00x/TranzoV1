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

// Premium Color Palette
val PrimaryVariant = Color(0xFF6366F1) // Indigo 500
val SecondaryVariant = Color(0xFF8B5CF6) // Violet 500
val AccentColor = Color(0xFF10B981) // Emerald 500
val BackgroundDark = Color(0xFF0F172A) // Slate 900
val SurfaceDark = Color(0xFF1E293B) // Slate 800
val CardDark = Color(0xFF334155) // Slate 700

// Legacy Brutalist Colors (Kept for compatibility)
val BrutalBlack = Color(0xFF000000)
val BrutalWhite = Color(0xFFFFFFFF)

enum class ThemeType {
    DEFAULT, DARK, MIDNIGHT, OCEAN, FOREST, CRIMSON, VIOLET, SUNSET
}

// Light Theme (Default - Modern/Fluid)
private val LightScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF4338CA),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0)
)

// Dark Theme (Modern/Fluid)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF0F172A),
    background = BackgroundDark,
    onBackground = Color(0xFFF1F5F9),
    surface = SurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
)

// Midnight Blue Theme
private val MidnightScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFBAE6FD),
    background = Color(0xFF030712),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF374151)
)

// Ocean Blue Theme
private val OceanScheme = darkColorScheme(
    primary = Color(0xFF22D3EE),
    onPrimary = Color(0xFF083344),
    primaryContainer = Color(0xFF155E75),
    onBackground = Color(0xFFECFEFF),
    background = Color(0xFF083344),
    surface = Color(0xFF164E63),
    onSurface = Color(0xFFECFEFF),
    outline = Color(0xFF0E7490)
)

// Forest Green Theme
private val ForestScheme = darkColorScheme(
    primary = Color(0xFF4ADE80),
    onPrimary = Color(0xFF052E16),
    background = Color(0xFF022C22),
    surface = Color(0xFF064E3B),
    onSurface = Color(0xFFF0FDF4),
    outline = Color(0xFF059669)
)

// Crimson Red Theme
private val CrimsonScheme = darkColorScheme(
    primary = Color(0xFFFB7185),
    onPrimary = Color(0xFF4C0519),
    background = Color(0xFF4C0519),
    surface = Color(0xFF881337),
    onSurface = Color(0xFFFFF1F2),
    outline = Color(0xFFBE123C)
)

// Violet Purple Theme
private val VioletScheme = darkColorScheme(
    primary = Color(0xFFC084FC),
    onPrimary = Color(0xFF2E1065),
    background = Color(0xFF1E1B4B),
    surface = Color(0xFF312E81),
    onSurface = Color(0xFFFAF5FF),
    outline = Color(0xFF6D28D9)
)

// Sunset Orange Theme
private val SunsetScheme = darkColorScheme(
    primary = Color(0xFFFB923C),
    onPrimary = Color(0xFF431407),
    background = Color(0xFF2D140B),
    surface = Color(0xFF7C2D12),
    onSurface = Color(0xFFFFF7ED),
    outline = Color(0xFFEA580C)
)

val FluidTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)

@Composable
fun CryptoWalletTheme(
    themeType: ThemeType = ThemeType.DEFAULT,
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeType) {
        ThemeType.DEFAULT -> if (darkTheme) DarkScheme else LightScheme
        ThemeType.DARK -> DarkScheme
        ThemeType.MIDNIGHT -> MidnightScheme
        ThemeType.OCEAN -> OceanScheme
        ThemeType.FOREST -> ForestScheme
        ThemeType.CRIMSON -> CrimsonScheme
        ThemeType.VIOLET -> VioletScheme
        ThemeType.SUNSET -> SunsetScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FluidTypography,
        content = content
    )
}
