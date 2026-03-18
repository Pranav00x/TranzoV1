package com.antigravity.cryptowallet.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.sp
import com.antigravity.cryptowallet.R

val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val BrutalBlack = Black
val BrutalWhite = White

// ─── Themes ───────────────────────────────────────────────────────────────────

enum class ThemeType {
    DEFAULT, DARK, MIDNIGHT, OCEAN, FOREST, CRIMSON, VIOLET, SUNSET,
    NEON, ROSE, AMBER, AMOLED
}

private val DefaultScheme = lightColorScheme(
    primary = Black, onPrimary = White,
    primaryContainer = Color(0xFFF0F0F0), onPrimaryContainer = Black,
    background = White, onBackground = Black,
    surface = White, onSurface = Black,
    surfaceVariant = Color(0xFFF5F5F5), onSurfaceVariant = Color(0xFF444444),
    outline = Color(0xFFCCCCCC), outlineVariant = Color(0xFFE0E0E0)
)

private val DarkScheme = darkColorScheme(
    primary = White, onPrimary = Black,
    primaryContainer = Color(0xFF2A2A2A), onPrimaryContainer = White,
    background = Color(0xFF121212), onBackground = White,
    surface = Color(0xFF1A1A1A), onSurface = White,
    surfaceVariant = Color(0xFF242424), onSurfaceVariant = Color(0xFFA0A0A0),
    outline = Color(0xFF333333), outlineVariant = Color(0xFF222222)
)

private val MidnightScheme = darkColorScheme(
    primary = Color(0xFF5E81AC), onPrimary = White,
    primaryContainer = Color(0xFF2E3440), onPrimaryContainer = Color(0xFF88C0D0),
    background = Color(0xFF0D1117), onBackground = Color(0xFFC9D1D9),
    surface = Color(0xFF161B22), onSurface = Color(0xFFC9D1D9),
    surfaceVariant = Color(0xFF21262D), onSurfaceVariant = Color(0xFF8B949E),
    outline = Color(0xFF30363D), outlineVariant = Color(0xFF21262D)
)

private val OceanScheme = darkColorScheme(
    primary = Color(0xFF00D9FF), onPrimary = Black,
    primaryContainer = Color(0xFF003D47), onPrimaryContainer = Color(0xFF97F0FF),
    background = Color(0xFF001F25), onBackground = Color(0xFFA6EEFF),
    surface = Color(0xFF00363F), onSurface = Color(0xFFA6EEFF),
    surfaceVariant = Color(0xFF004D59), onSurfaceVariant = Color(0xFF6FF0FF),
    outline = Color(0xFF006A7A), outlineVariant = Color(0xFF004D59)
)

private val ForestScheme = darkColorScheme(
    primary = Color(0xFF4ADE80), onPrimary = Black,
    primaryContainer = Color(0xFF14532D), onPrimaryContainer = Color(0xFFBBF7D0),
    background = Color(0xFF052E16), onBackground = Color(0xFFDCFCE7),
    surface = Color(0xFF14532D), onSurface = Color(0xFFDCFCE7),
    surfaceVariant = Color(0xFF166534), onSurfaceVariant = Color(0xFF86EFAC),
    outline = Color(0xFF22C55E), outlineVariant = Color(0xFF166534)
)

private val CrimsonScheme = darkColorScheme(
    primary = Color(0xFFF87171), onPrimary = Black,
    primaryContainer = Color(0xFF7F1D1D), onPrimaryContainer = Color(0xFFFECACA),
    background = Color(0xFF1F0808), onBackground = Color(0xFFFEE2E2),
    surface = Color(0xFF450A0A), onSurface = Color(0xFFFEE2E2),
    surfaceVariant = Color(0xFF7F1D1D), onSurfaceVariant = Color(0xFFFCA5A5),
    outline = Color(0xFFDC2626), outlineVariant = Color(0xFF7F1D1D)
)

private val VioletScheme = darkColorScheme(
    primary = Color(0xFFA78BFA), onPrimary = Black,
    primaryContainer = Color(0xFF4C1D95), onPrimaryContainer = Color(0xFFDDD6FE),
    background = Color(0xFF0C0527), onBackground = Color(0xFFEDE9FE),
    surface = Color(0xFF2E1065), onSurface = Color(0xFFEDE9FE),
    surfaceVariant = Color(0xFF4C1D95), onSurfaceVariant = Color(0xFFC4B5FD),
    outline = Color(0xFF7C3AED), outlineVariant = Color(0xFF4C1D95)
)

private val SunsetScheme = darkColorScheme(
    primary = Color(0xFFFB923C), onPrimary = Black,
    primaryContainer = Color(0xFF7C2D12), onPrimaryContainer = Color(0xFFFED7AA),
    background = Color(0xFF1A0F05), onBackground = Color(0xFFFFF7ED),
    surface = Color(0xFF431407), onSurface = Color(0xFFFFF7ED),
    surfaceVariant = Color(0xFF7C2D12), onSurfaceVariant = Color(0xFFFDBA74),
    outline = Color(0xFFEA580C), outlineVariant = Color(0xFF7C2D12)
)

// Cyberpunk neon green on near-black
private val NeonScheme = darkColorScheme(
    primary = Color(0xFF39FF14), onPrimary = Black,
    primaryContainer = Color(0xFF0A1A05), onPrimaryContainer = Color(0xFF84F55A),
    background = Color(0xFF050508), onBackground = Color(0xFFD0FFB0),
    surface = Color(0xFF0D0F0A), onSurface = Color(0xFFD0FFB0),
    surfaceVariant = Color(0xFF141A0E), onSurfaceVariant = Color(0xFF7FD95A),
    outline = Color(0xFF2E7D14), outlineVariant = Color(0xFF1A3A0C)
)

// Rose pink on dark wine
private val RoseScheme = darkColorScheme(
    primary = Color(0xFFF472B6), onPrimary = Black,
    primaryContainer = Color(0xFF4A0D2A), onPrimaryContainer = Color(0xFFFBCFE8),
    background = Color(0xFF1A0810), onBackground = Color(0xFFFFE4F0),
    surface = Color(0xFF2D0F1C), onSurface = Color(0xFFFFE4F0),
    surfaceVariant = Color(0xFF4A1429), onSurfaceVariant = Color(0xFFF9A8D4),
    outline = Color(0xFFBE185D), outlineVariant = Color(0xFF4A1429)
)

// Warm amber/gold on dark brown
private val AmberScheme = darkColorScheme(
    primary = Color(0xFFF59E0B), onPrimary = Black,
    primaryContainer = Color(0xFF3D2000), onPrimaryContainer = Color(0xFFFDE68A),
    background = Color(0xFF160D00), onBackground = Color(0xFFFFF8E8),
    surface = Color(0xFF271600), onSurface = Color(0xFFFFF8E8),
    surfaceVariant = Color(0xFF3D2300), onSurfaceVariant = Color(0xFFFCD34D),
    outline = Color(0xFFB45309), outlineVariant = Color(0xFF3D2300)
)

// Pure OLED black with electric cyan
private val AmoledScheme = darkColorScheme(
    primary = Color(0xFF00F5FF), onPrimary = Black,
    primaryContainer = Color(0xFF001C1E), onPrimaryContainer = Color(0xFF80FAFF),
    background = Color(0xFF000000), onBackground = Color(0xFFE0FEFF),
    surface = Color(0xFF0A0A0A), onSurface = Color(0xFFE0FEFF),
    surfaceVariant = Color(0xFF111111), onSurfaceVariant = Color(0xFF00C8D4),
    outline = Color(0xFF007A84), outlineVariant = Color(0xFF001C1E)
)

// ─── Fonts ────────────────────────────────────────────────────────────────────

enum class FontType {
    MONOSPACE,   // System monospace (default, classic crypto look)
    JETBRAINS,   // JetBrains Mono — sharp developer font
    GROTESK,     // Space Grotesk — modern geometric fintech
    INTER        // Inter — clean, highly legible UI font
}

@OptIn(ExperimentalTextApi::class)
private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

@OptIn(ExperimentalTextApi::class)
private fun getFontFamily(fontType: FontType): FontFamily = when (fontType) {
    FontType.MONOSPACE -> FontFamily.Monospace
    FontType.JETBRAINS -> FontFamily(
        Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = googleFontProvider)
    )
    FontType.GROTESK -> FontFamily(
        Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = googleFontProvider)
    )
    FontType.INTER -> FontFamily(
        Font(googleFont = GoogleFont("Inter"), fontProvider = googleFontProvider)
    )
}

private fun buildTypography(fontFamily: FontFamily) = Typography(
    displayLarge = TextStyle(
        fontFamily = fontFamily, fontWeight = FontWeight.Medium,
        fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = fontFamily, fontWeight = FontWeight.Medium,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = fontFamily, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = fontFamily, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.sp
    )
)

// ─── Theme composable ─────────────────────────────────────────────────────────

@OptIn(ExperimentalTextApi::class)
@Composable
fun CryptoWalletTheme(
    themeType: ThemeType = ThemeType.DARK,
    fontType: FontType = FontType.MONOSPACE,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeType) {
        ThemeType.DEFAULT  -> DefaultScheme
        ThemeType.DARK     -> DarkScheme
        ThemeType.MIDNIGHT -> MidnightScheme
        ThemeType.OCEAN    -> OceanScheme
        ThemeType.FOREST   -> ForestScheme
        ThemeType.CRIMSON  -> CrimsonScheme
        ThemeType.VIOLET   -> VioletScheme
        ThemeType.SUNSET   -> SunsetScheme
        ThemeType.NEON     -> NeonScheme
        ThemeType.ROSE     -> RoseScheme
        ThemeType.AMBER    -> AmberScheme
        ThemeType.AMOLED   -> AmoledScheme
    }

    val typography = remember(fontType) { buildTypography(getFontFamily(fontType)) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
