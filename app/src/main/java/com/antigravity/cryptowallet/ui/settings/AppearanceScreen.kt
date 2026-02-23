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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antigravity.cryptowallet.ui.components.BrutalistHeader
import com.antigravity.cryptowallet.ui.theme.ThemeType

// Theme preview colors
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
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            BrutalistHeader("Appearance")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SELECT THEME",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp, end = 4.dp)
    ) {
        // Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(4.dp, 4.dp)
                .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(12.dp))
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(12.dp)
                )
                .border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .clickable { onSelect() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Theme preview box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(previewColors.background)
                    .border(1.dp, previewColors.text.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mini header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(previewColors.primary, RoundedCornerShape(2.dp))
                    )
                    // Mini content
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .background(previewColors.surface, RoundedCornerShape(2.dp))
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .background(previewColors.surface, RoundedCornerShape(2.dp))
                        )
                    }
                    // Mini button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(previewColors.primary, RoundedCornerShape(2.dp))
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getThemeDisplayName(theme),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = getThemeDescription(theme).uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.onBackground, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check, 
                        contentDescription = "Selected", 
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

fun getThemeDisplayName(theme: ThemeType): String {
    return when(theme) {
        ThemeType.DEFAULT -> "Light"
        ThemeType.DARK -> "Dark"
        ThemeType.MIDNIGHT -> "Midnight"
        ThemeType.OCEAN -> "Ocean"
        ThemeType.FOREST -> "Forest"
        ThemeType.CRIMSON -> "Crimson"
        ThemeType.VIOLET -> "Violet"
        ThemeType.SUNSET -> "Sunset"
    }
}

fun getThemeDescription(theme: ThemeType): String {
    return when(theme) {
        ThemeType.DEFAULT -> "Clean white theme"
        ThemeType.DARK -> "Pure dark mode"
        ThemeType.MIDNIGHT -> "GitHub-inspired dark"
        ThemeType.OCEAN -> "Deep ocean vibes"
        ThemeType.FOREST -> "Nature green tones"
        ThemeType.CRIMSON -> "Bold red accents"
        ThemeType.VIOLET -> "Purple elegance"
        ThemeType.SUNSET -> "Warm orange glow"
    }
}

fun getThemePreviewColors(theme: ThemeType): ThemePreviewColors {
    return when(theme) {
        ThemeType.DEFAULT -> ThemePreviewColors(
            primary = Color(0xFF000000),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF5F5F5),
            text = Color(0xFF000000)
        )
        ThemeType.DARK -> ThemePreviewColors(
            primary = Color(0xFF3B82F6),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            text = Color(0xFFFFFFFF)
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
}
