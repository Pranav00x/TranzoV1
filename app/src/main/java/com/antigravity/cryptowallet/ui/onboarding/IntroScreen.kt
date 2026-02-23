package com.antigravity.cryptowallet.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.cryptowallet.ui.components.BrutalistButton

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.MaterialTheme

@Composable
fun IntroScreen(
    onCreateWallet: () -> Unit,
    onImportWallet: () -> Unit
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                // Brutalist Grid Pattern
                val cellSize = 40.dp.toPx()
                if (cellSize > 0) {
                    var x = 0f
                    while (x < size.width) {
                        drawLine(onBg.copy(alpha = 0.05f), Offset(x, 0f), Offset(x, size.height), 1f)
                        x += cellSize
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(onBg.copy(alpha = 0.05f), Offset(0f, y), Offset(size.width, y), 1f)
                        y += cellSize
                    }
                }
            }
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(80.dp))
            
            // Decorative Element
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, onBg)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "FLOWSTABLE",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "YOUR GATEWAY\nTO THE BLOCKCHAIN.",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 44.sp,
                color = onBg
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "DECENTRALIZED. SAFE. FAST.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = onBg.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 48.dp)
        ) {
            BrutalistButton(
                text = "Create New Wallet",
                onClick = onCreateWallet
            )
            BrutalistButton(
                text = "Restore Wallet",
                onClick = onImportWallet,
                inverted = true
            )
            
            Text(
                text = "By continuing you agree to the Terms of Service",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
