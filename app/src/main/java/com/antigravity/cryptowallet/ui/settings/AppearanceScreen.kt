package com.antigravity.cryptowallet.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antigravity.cryptowallet.ui.theme.FontType
import com.antigravity.cryptowallet.ui.theme.ThemeType

data class ThemePreviewColors(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val text: Color
)

@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themes = ThemeType.values().toList()
    val currentTheme = viewModel.currentTheme.collectAsState(initial = ThemeType.DEFAULT).value
    val currentFont = viewModel.currentFont.collectAsState(initial = FontType.MONOSPACE).value

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    "Appearance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Theme section label
        item {
            SectionLabel("Theme", modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Theme cards
        items(themes) { theme ->
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                ThemeOptionCard(
                    theme = theme,
                    isSelected = currentTheme == theme,
                    onSelect = { viewModel.setTheme(theme) }
                )
            }
        }

        // Font section label
        item {
            Spacer(modifier = Modifier.height(24.dp))
            SectionLabel("Font", modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Font options
        items(FontType.values().toList()) { font ->
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                FontOptionCard(
                    fontType = font,
                    isSelected = currentFont == font,
                    onSelect = { viewModel.setFont(font) }
                )
            }
        }
    }
}

@Composable
fun ThemeOptionCard(theme: ThemeType, isSelected: Boolean, onSelect: () -> Unit) {
    val previewColors = getThemePreviewColors(theme)

    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp, end = 4.dp)) {
        // Shadow
        Box(
            modifier = Modifier.matchParentSize().offset(4.dp, 4.dp)
                .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(12.dp))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(12.dp)
                )
                .border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .clickable { onSelect() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini preview
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                    .background(previewColors.background)
                    .border(1.dp, previewColors.text.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp)
                        .background(previewColors.primary, RoundedCornerShape(2.dp)))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(modifier = Modifier.weight(1f).height(12.dp)
                            .background(previewColors.surface, RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.weight(1f).height(12.dp)
                            .background(previewColors.surface, RoundedCornerShape(2.dp)))
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp)
                        .background(previewColors.primary, RoundedCornerShape(2.dp)))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    getThemeDisplayName(theme),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    getThemeDescription(theme).uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.onBackground, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun FontOptionCard(fontType: FontType, isSelected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                getFontDisplayName(fontType),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                getFontSample(fontType),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier.size(22.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String, modifier: Modifier = Modifier) {
    Text(
        title.uppercase(),
        modifier = modifier,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        letterSpacing = 2.sp
    )
}

fun getThemeDisplayName(theme: ThemeType): String = when (theme) {
    ThemeType.DEFAULT  -> "Light"
    ThemeType.DARK     -> "Dark"
    ThemeType.MIDNIGHT -> "Midnight"
    ThemeType.OCEAN    -> "Ocean"
    ThemeType.FOREST   -> "Forest"
    ThemeType.CRIMSON  -> "Crimson"
    ThemeType.VIOLET   -> "Violet"
    ThemeType.SUNSET   -> "Sunset"
    ThemeType.NEON     -> "Neon"
    ThemeType.ROSE     -> "Rose"
    ThemeType.AMBER    -> "Amber"
    ThemeType.AMOLED   -> "AMOLED"
}

fun getThemeDescription(theme: ThemeType): String = when (theme) {
    ThemeType.DEFAULT  -> "Clean white theme"
    ThemeType.DARK     -> "Pure dark mode"
    ThemeType.MIDNIGHT -> "GitHub-inspired dark"
    ThemeType.OCEAN    -> "Deep ocean vibes"
    ThemeType.FOREST   -> "Nature green tones"
    ThemeType.CRIMSON  -> "Bold red accents"
    ThemeType.VIOLET   -> "Purple elegance"
    ThemeType.SUNSET   -> "Warm orange glow"
    ThemeType.NEON     -> "Cyberpunk neon green"
    ThemeType.ROSE     -> "Rose gold & pink"
    ThemeType.AMBER    -> "Warm gold tones"
    ThemeType.AMOLED   -> "True black for OLED"
}

fun getThemePreviewColors(theme: ThemeType): ThemePreviewColors = when (theme) {
    ThemeType.DEFAULT  -> ThemePreviewColors(Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFFF5F5F5), Color(0xFF000000))
    ThemeType.DARK     -> ThemePreviewColors(Color(0xFFFFFFFF), Color(0xFF121212), Color(0xFF1E1E1E), Color(0xFFFFFFFF))
    ThemeType.MIDNIGHT -> ThemePreviewColors(Color(0xFF5E81AC), Color(0xFF0D1117), Color(0xFF161B22), Color(0xFFC9D1D9))
    ThemeType.OCEAN    -> ThemePreviewColors(Color(0xFF00D9FF), Color(0xFF001F25), Color(0xFF00363F), Color(0xFFA6EEFF))
    ThemeType.FOREST   -> ThemePreviewColors(Color(0xFF4ADE80), Color(0xFF052E16), Color(0xFF14532D), Color(0xFFDCFCE7))
    ThemeType.CRIMSON  -> ThemePreviewColors(Color(0xFFF87171), Color(0xFF1F0808), Color(0xFF450A0A), Color(0xFFFEE2E2))
    ThemeType.VIOLET   -> ThemePreviewColors(Color(0xFFA78BFA), Color(0xFF0C0527), Color(0xFF2E1065), Color(0xFFEDE9FE))
    ThemeType.SUNSET   -> ThemePreviewColors(Color(0xFFFB923C), Color(0xFF1A0F05), Color(0xFF431407), Color(0xFFFFF7ED))
    ThemeType.NEON     -> ThemePreviewColors(Color(0xFF39FF14), Color(0xFF050508), Color(0xFF0D0F0A), Color(0xFFD0FFB0))
    ThemeType.ROSE     -> ThemePreviewColors(Color(0xFFF472B6), Color(0xFF1A0810), Color(0xFF2D0F1C), Color(0xFFFFE4F0))
    ThemeType.AMBER    -> ThemePreviewColors(Color(0xFFF59E0B), Color(0xFF160D00), Color(0xFF271600), Color(0xFFFFF8E8))
    ThemeType.AMOLED   -> ThemePreviewColors(Color(0xFF00F5FF), Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFE0FEFF))
}

fun getFontDisplayName(font: FontType): String = when (font) {
    FontType.MONOSPACE -> "Monospace  —  default"
    FontType.JETBRAINS -> "JetBrains Mono"
    FontType.GROTESK   -> "Space Grotesk"
    FontType.INTER     -> "Inter"
}

fun getFontSample(font: FontType): String = when (font) {
    FontType.MONOSPACE -> "0x1a2b3c  ·  wallet balance"
    FontType.JETBRAINS -> "sharp · developer-grade font"
    FontType.GROTESK   -> "modern · geometric fintech"
    FontType.INTER     -> "clean · highly legible ui"
}
