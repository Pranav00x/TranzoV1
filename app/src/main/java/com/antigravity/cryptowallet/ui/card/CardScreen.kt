package com.antigravity.cryptowallet.ui.card

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antigravity.cryptowallet.ui.components.BrutalistButton
import com.antigravity.cryptowallet.ui.components.BrutalistHeader
import com.antigravity.cryptowallet.ui.components.BrutalistInfoRow

@Composable
fun CardScreen(
    viewModel: CardViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        BrutalistHeader("Crypto Card")
        
        Spacer(modifier = Modifier.height(24.dp))

        // Virtual Card Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            // Shadow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(8.dp, 8.dp)
                    .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(24.dp))
            )

            // Main Card
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        if (viewModel.isCardFrozen) Color.Gray else MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(24.dp)
                    )
                    .border(3.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "PREMIUM DEBIT",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.Nfc,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column {
                    Text(
                        viewModel.cardNumber,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Column {
                            Text("EXPIRY", fontSize = 8.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                            Text(viewModel.cardExpiry, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(modifier = Modifier.width(32.dp))
                        Column {
                            Text("CVV", fontSize = 8.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                            Text(viewModel.cardCvv, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        viewModel.cardHolderName,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        "VISA",
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            if (viewModel.isCardFrozen) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                        Text("FROZEN", fontWeight = FontWeight.Black, color = Color.White, fontSize = 24.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val freezeLabel = if (viewModel.isCardFrozen) "Unfreeze" else "Freeze"
            val freezeIcon = if (viewModel.isCardFrozen) Icons.Default.LockOpen else Icons.Default.Lock
            
            Box(modifier = Modifier.weight(1f)) {
                BrutalistButton(
                    text = freezeLabel,
                    onClick = { viewModel.toggleFreeze() },
                    icon = freezeIcon,
                    inverted = !viewModel.isCardFrozen
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                BrutalistButton(
                    text = if (viewModel.showSensitiveData) "Hide" else "Reveal",
                    onClick = { viewModel.toggleSensitiveData() },
                    icon = if (viewModel.showSensitiveData) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    inverted = true
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Spending Limit Info
        BrutalistInfoRow(label = "Spending Limit", value = viewModel.spendingLimitUsd)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Your spending limit is automatically calculated based on your USDT/USDC balances across all supported networks.",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        BrutalistButton(
            text = "Add to Google Pay",
            onClick = { /* Simulated */ },
            icon = Icons.Default.AddCard,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
