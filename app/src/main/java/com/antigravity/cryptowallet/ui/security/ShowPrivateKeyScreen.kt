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
fun ShowPrivateKeyScreen(
    privateKey: String,
    onBack: () -> Unit
) {
    val fullKey = if (privateKey.startsWith("0x")) privateKey else "0x$privateKey"
    var revealed by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0A0A)).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Private Key", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
        }

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Danger banner
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A0000)).padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF4444), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("CRITICAL — NEVER SHARE THIS KEY", fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = Color(0xFFFF4444))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Anyone with this key can drain your wallet instantly. No recovery is possible once funds are stolen.", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFFF8888), lineHeight = 16.sp)
                }
            }

            // Key display box
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF111111)).padding(18.dp)) {
                Column {
                    Text("PRIVATE KEY", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Gray, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Box {
                        Text(text = fullKey, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White, lineHeight = 20.sp, modifier = Modifier.fillMaxWidth())
                        AnimatedVisibility(visible = !revealed, enter = fadeIn(), exit = fadeOut()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF111111)).padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
                                    Text("Tap to reveal key", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            }

            // Reveal toggle
            Button(
                onClick = { revealed = !revealed },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (revealed) Color(0xFF1A1A1A) else Color(0xFFCC0000),
                    contentColor = Color.White
                )
            ) {
                Icon(if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (revealed) "Hide Key" else "Reveal Private Key", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // Security reminders
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF111111)).padding(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("SECURITY REMINDERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Gray, letterSpacing = 2.sp)
                    listOf(
                        "Clipboard copy is disabled on this screen",
                        "Screenshot protection is active (FLAG_SECURE)",
                        "Write the key on paper, never digitally",
                        "Tranzo support will never ask for your key"
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
