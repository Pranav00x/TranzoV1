package com.antigravity.cryptowallet.ui.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ShowSeedScreen(
    mnemonic: String,
    onBack: () -> Unit
) {
    val words = mnemonic.trim().split(" ")
    var revealed by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0A0A)).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Seed Phrase", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
        }
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A0000)).padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF4444), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("NEVER share your seed phrase", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFFFF4444))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Anyone with these 12 words has full control of your wallet. Store them offline, never in screenshots, cloud, or messages.", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFFF8888), lineHeight = 16.sp)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF111111)).padding(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    words.chunked(2).forEachIndexed { rowIndex, rowWords ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            rowWords.forEachIndexed { colIndex, word ->
                                val idx = rowIndex * 2 + colIndex + 1
                                Box(modifier = Modifier.weight(1f)) { SeedWordTile(idx, word, revealed) }
                            }
                            if (rowWords.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                AnimatedVisibility(visible = !revealed, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.matchParentSize()) {
                    Box(
                        modifier = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)).background(Color(0xFF111111).copy(alpha = 0.88f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                            Text("Tap to reveal", fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.4f))
                        }
                    }
                }
            }
            Button(
                onClick = { revealed = !revealed },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (revealed) Color(0xFF1A1A1A) else Color.White,
                    contentColor = if (revealed) Color.White else Color.Black
                )
            ) {
                Icon(if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (revealed) "Hide Phrase" else "Reveal Phrase", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF111111)).padding(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("HOW TO STORE SAFELY", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Gray, letterSpacing = 2.sp)
                    listOf(
                        "Write on paper, store in a fireproof safe",
                        "Never store digitally or in cloud storage",
                        "Never share with anyone, including Tranzo support",
                        "Keep multiple physical copies in separate locations"
                    ).forEach { tip ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("*", fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tip, fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                        }
                    }
                }
            }
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A), contentColor = Color.White)
            ) { Text("Done", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SeedWordTile(index: Int, word: String, revealed: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF1C1C1C)).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$index", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Gray, modifier = Modifier.width(20.dp))
        Text(text = if (revealed) word else "......", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = if (revealed) Color.White else Color(0xFF333333))
    }
}
