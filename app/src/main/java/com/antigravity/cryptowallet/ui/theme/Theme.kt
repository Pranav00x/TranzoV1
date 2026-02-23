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

val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val BrutalBlack = Black
val BrutalWhite = White

enum class ThemeType {
    DEFAULT, DARK, MIDNIGHT, OCEAN, FOREST, CRIMSON, VIOLET, SUNSET
}

// Light Theme (Default)
private val DefaultScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    primaryContainer = Color(0xFFF0F0F0),
    onPrimaryContainer = Black,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF444444),
    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFE0E0E0)
)

// Pure Dark Theme
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = White,
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFB4D4FF),
    background = Color(0xFF121212),
    onBackground = White,
    surface = Color(0xFF1E1E1E),
    onSurface = White,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFBBBBBB),
    outline = Color(0xFF444444),
    outlineVariant = Color(0xFF333333)
)

// Midnight Blue Theme
private val MidnightScheme = darkColorScheme(
    primary = Color(0xFF5E81AC),
    onPrimary = White,
    primaryContainer = Color(0xFF2E3440),
    onPrimaryContainer = Color(0xFF88C0D0),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFC9D1D9),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFC9D1D9),
    surfaceVariant = Color(0xFF21262D),
    onSurfaceVariant = Color(0xFF8B949E),
    outline = Color(0xFF30363D),
    outlineVariant = Color(0xFF21262D)
)

// Ocean Blue Theme
private val OceanScheme = darkColorScheme(
    primary = Color(0xFF00D9FF),
    onPrimary = Black,
    primaryContainer = Color(0xFF003D47),
    onPrimaryContainer = Color(0xFF97F0FF),
    background = Color(0xFF001F25),
    onBackground = Color(0xFFA6EEFF),
    surface = Color(0xFF00363F),
    onSurface = Color(0xFFA6EEFF),
    surfaceVariant = Color(0xFF004D59),
    onSurfaceVariant = Color(0xFF6FF0FF),
    outline = Color(0xFF006A7A),
    outlineVariant = Color(0xFF004D59)
)

// Forest Green Theme
private val ForestScheme = darkColorScheme(
    primary = Color(0xFF4ADE80),
    onPrimary = Black,
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFBBF7D0),
    background = Color(0xFF052E16),
    onBackground = Color(0xFFDCFCE7),
    surface = Color(0xFF14532D),
    onSurface = Color(0xFFDCFCE7),
    surfaceVariant = Color(0xFF166534),
    onSurfaceVariant = Color(0xFF86EFAC),
    outline = Color(0xFF22C55E),
    outlineVariant = Color(0xFF166534)
)

// Crimson Red Theme
private val CrimsonScheme = darkColorScheme(
    primary = Color(0xFFF87171),
    onPrimary = Black,
    primaryContainer = Color(0xFF7F1D1D),
    onPrimaryContainer = Color(0xFFFECACA),
    background = Color(0xFF1F0808),
    onBackground = Color(0xFFFEE2E2),
    surface = Color(0xFF450A0A),
    onSurface = Color(0xFFFEE2E2),
    surfaceVariant = Color(0xFF7F1D1D),
    onSurfaceVariant = Color(0xFFFCA5A5),
    outline = Color(0xFFDC2626),
    outlineVariant = Color(0xFF7F1D1D)
)

// Violet Purple Theme
private val VioletScheme = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Black,
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFDDD6FE),
    background = Color(0xFF0C0527),
    onBackground = Color(0xFFEDE9FE),
    surface = Color(0xFF2E1065),
    onSurface = Color(0xFFEDE9FE),
    surfaceVariant = Color(0xFF4C1D95),
    onSurfaceVariant = Color(0xFFC4B5FD),
    outline = Color(0xFF7C3AED),
    outlineVariant = Color(0xFF4C1D95)
)

// Sunset Orange Theme
private val SunsetScheme = darkColorScheme(
    primary = Color(0xFFFB923C),
    onPrimary = Black,
    primaryContainer = Color(0xFF7C2D12),
    onPrimaryContainer = Color(0xFFFED7AA),
    background = Color(0xFF1A0F05),
    onBackground = Color(0xFFFFF7ED),
    surface = Color(0xFF431407),
    onSurface = Color(0xFFFFF7ED),
    surfaceVariant = Color(0xFF7C2D12),
    onSurfaceVariant = Color(0xFFFDBA74),
    outline = Color(0xFFEA580C),
    outlineVariant = Color(0xFF7C2D12)
)

val BrutalistTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.1.sp
    )
)

@Composable
fun CryptoWalletTheme(
    themeType: ThemeType = ThemeType.DEFAULT,
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(), // Kept for compatibility, ignored
    content: @Composable () -> Unit
) {
    // Theme is now independent of system dark mode
    // User's selected theme always takes precedence
    val colorScheme = when (themeType) {
        ThemeType.DEFAULT -> DefaultScheme
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
        typography = BrutalistTypography,
        content = content
    )
}
