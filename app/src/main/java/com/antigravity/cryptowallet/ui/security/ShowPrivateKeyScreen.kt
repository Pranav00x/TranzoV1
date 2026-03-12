package com.antigravity.cryptowallet.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.cryptowallet.ui.components.BrutalistHeader
import com.antigravity.cryptowallet.ui.theme.BrutalBlack
import com.antigravity.cryptowallet.ui.theme.BrutalWhite

@Composable
fun ShowPrivateKeyScreen(
    privateKey: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrutalWhite)
            .padding(16.dp)
    ) {
        BrutalistHeader("Private Key")
        
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "NEVER SHARE YOUR PRIVATE KEY. Anyone with this key can steal your funds. Keep it offline and safe.",
            fontSize = 14.sp,
            color = Color.Red,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        val fullKey = if (privateKey.startsWith("0x")) privateKey else "0x$privateKey"

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, BrutalBlack, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = fullKey,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BrutalBlack,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                modifier = Modifier.weight(1f),
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                maxLines = 2
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        com.antigravity.cryptowallet.ui.components.BrutalistButton(
            text = "DONE",
            onClick = { onBack() }
        )
    }
}
