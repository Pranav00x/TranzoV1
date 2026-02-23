package com.antigravity.cryptowallet.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antigravity.cryptowallet.ui.components.FluidHeader
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            FluidHeader("Appearance")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Select Theme",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(themes) { theme ->
                ThemeOptionCard(
                    theme = theme,
                    isSelected = currentTheme == theme,
                    onSelect = { viewModel.setTheme(theme) }
                )
            }
        }
    }
}

@Composable
fun ThemeOptionCard(theme: ThemeType, isSelected: Boolean, onSelect: () -> Unit) {
    val previewColors = getThemePreviewColors(theme)
    
    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        tonalElevation = if (isSelected) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Theme preview box
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = previewColors.background,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(previewColors.primary, RoundedCornerShape(4.dp))
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .background(previewColors.surface, RoundedCornerShape(4.dp))
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .background(previewColors.surface, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getThemeDisplayName(theme),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = getThemeDescription(theme),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check, 
                        contentDescription = "Selected", 
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

fun getThemeDisplayName(theme: ThemeType): String = when(theme) {
    ThemeType.DEFAULT -> "Light"
    ThemeType.DARK -> "Dark"
    ThemeType.MIDNIGHT -> "Midnight"
    ThemeType.OCEAN -> "Ocean"
    ThemeType.FOREST -> "Forest"
    ThemeType.CRIMSON -> "Crimson"
    ThemeType.VIOLET -> "Violet"
    ThemeType.SUNSET -> "Sunset"
}

fun getThemeDescription(theme: ThemeType): String = when(theme) {
    ThemeType.DEFAULT -> "Clean and bright"
    ThemeType.DARK -> "Pure dark mode"
    ThemeType.MIDNIGHT -> "GitHub-inspired"
    ThemeType.OCEAN -> "Deep ocean vibes"
    ThemeType.FOREST -> "Nature green tones"
    ThemeType.CRIMSON -> "Bold red accents"
    ThemeType.VIOLET -> "Purple elegance"
    ThemeType.SUNSET -> "Warm orange glow"
}

fun getThemePreviewColors(theme: ThemeType): ThemePreviewColors = when(theme) {
    ThemeType.DEFAULT -> ThemePreviewColors(
        primary = Color(0xFF6366F1),
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFF3F4F6),
        text = Color(0xFF111827)
    )
    ThemeType.DARK -> ThemePreviewColors(
        primary = Color(0xFF60A5FA),
        background = Color(0xFF111827),
        surface = Color(0xFF1F2937),
        text = Color(0xFFF9FAFB)
    )
    ThemeType.MIDNIGHT -> ThemePreviewColors(
        primary = Color(0xFF5E81AC),
        background = Color(0xFF0D1117),
        surface = Color(0xFF161B22),
        text = Color(0xFFC9D1D9)
    )
    ThemeType.OCEAN -> ThemePreviewColors(
        primary = Color(0xFF00D9FF),
        background = Color(0xFF001F25),
        surface = Color(0xFF00363F),
        text = Color(0xFFA6EEFF)
    )
    ThemeType.FOREST -> ThemePreviewColors(
        primary = Color(0xFF4ADE80),
        background = Color(0xFF052E16),
        surface = Color(0xFF14532D),
        text = Color(0xFFDCFCE7)
    )
    ThemeType.CRIMSON -> ThemePreviewColors(
        primary = Color(0xFFF87171),
        background = Color(0xFF1F0808),
        surface = Color(0xFF450A0A),
        text = Color(0xFFFEE2E2)
    )
    ThemeType.VIOLET -> ThemePreviewColors(
        primary = Color(0xFFA78BFA),
        background = Color(0xFF0C0527),
        surface = Color(0xFF2E1065),
        text = Color(0xFFEDE9FE)
    )
    ThemeType.SUNSET -> ThemePreviewColors(
        primary = Color(0xFFFB923C),
        background = Color(0xFF1A0F05),
        surface = Color(0xFF431407),
        text = Color(0xFFFFF7ED)
    )
}
