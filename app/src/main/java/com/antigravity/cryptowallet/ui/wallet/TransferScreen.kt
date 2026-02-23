package com.antigravity.cryptowallet.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antigravity.cryptowallet.data.models.AssetUiModel
import com.antigravity.cryptowallet.ui.components.BrutalistButton
import com.antigravity.cryptowallet.ui.components.BrutalistHeader
import com.antigravity.cryptowallet.ui.theme.BrutalBlack
import com.antigravity.cryptowallet.ui.theme.BrutalWhite

@Composable
fun TransferScreen(
    onBack: () -> Unit,
    onTransactionSuccess: (String, String, String, String, String) -> Unit,
    initialSymbol: String? = null,
    viewModel: WalletViewModel = hiltViewModel()
) {
    var step by remember { mutableStateOf(0) } // 0: Select Asset, 1: Enter Details
    var selectedAsset by remember { mutableStateOf<AssetUiModel?>(null) }
    var recipientAddress by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    // Ensure assets are loaded
    LaunchedEffect(Unit) {
        if (viewModel.assets.isEmpty()) {
            // Trigger refresh if needed
        }
    }
    
    // Auto-select asset
    LaunchedEffect(viewModel.assets, initialSymbol) {
        if (initialSymbol != null && selectedAsset == null && viewModel.assets.isNotEmpty()) {
            selectedAsset = viewModel.assets.find { it.symbol.equals(initialSymbol, ignoreCase = true) }
            if (selectedAsset != null) {
                step = 1
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (step > 0) step-- else onBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            BrutalistHeader(if (step == 0) "Select Token" else "Send ${selectedAsset?.symbol}")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (step == 0) {
            if (viewModel.assets.isEmpty()) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                     Text("Loading Assets...", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = MaterialTheme.colorScheme.onBackground)
                 }
            } else {
                AssetSelector(
                    assets = viewModel.assets,
                    onSelected = {
                        selectedAsset = it
                        step = 1
                    }
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Asset Info Card
                selectedAsset?.let { asset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Balance", fontSize = 12.sp, color = Color.Gray)
                            Text(asset.balance, fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Price", fontSize = 12.sp, color = Color.Gray)
                            Text(String.format("$%.2f", asset.price), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }

                OutlinedTextField(
                    value = recipientAddress,
                    onValueChange = { recipientAddress = it },
                    label = { Text("Recipient Address") },
                    placeholder = { Text("0x...") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSending,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = MaterialTheme.colorScheme.onBackground,
                        focusedLabelColor = MaterialTheme.colorScheme.onBackground
                    )
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSending,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = MaterialTheme.colorScheme.onBackground,
                        focusedLabelColor = MaterialTheme.colorScheme.onBackground
                    )
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.weight(1f))

                BrutalistButton(
                    text = if (isSending) "Processing..." else "Send Now",
                    icon = if (isSending) null else Icons.Default.Send,
                    onClick = {
                        if (recipientAddress.isNotBlank() && amount.isNotBlank() && selectedAsset != null) {
                            val evmRegex = Regex("^0x[a-fA-F0-9]{40}$")
                            if (!evmRegex.matches(recipientAddress)) {
                                errorMsg = "Invalid EVM Address"
                                return@BrutalistButton
                            }
                            
                            val amountVal = amount.toDoubleOrNull()
                            if (amountVal == null || amountVal <= 0) {
                                errorMsg = "Invalid Amount"
                                return@BrutalistButton
                            }
                            
                            if (amountVal > selectedAsset!!.rawBalance) {
                                errorMsg = "Insufficient Balance (Max: ${selectedAsset!!.balance})"
                                return@BrutalistButton
                            }
                            
                            scope.launch {
                                isSending = true
                                errorMsg = null
                                // Call suspend function directly
                                val txHash = viewModel.sendAsset(selectedAsset!!, recipientAddress, amount)
                                
                                isSending = false
                                
                                if (txHash != null) {
                                    onTransactionSuccess(amount, selectedAsset!!.symbol, recipientAddress, txHash, selectedAsset!!.networkName)
                                } else {
                                    errorMsg = "Transaction Failed. Please check network/rpc."
                                }
                            }
                        } else {
                            errorMsg = "Please fill all fields"
                        }
                    },
                    enabled = !isSending,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


@Composable
fun AssetSelector(
    assets: List<AssetUiModel>,
    onSelected: (AssetUiModel) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(assets) { asset ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelected(asset) }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.onBackground, CircleShape)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            asset.symbol.take(1),
                            color = MaterialTheme.colorScheme.background,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(asset.symbol, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                        Text(asset.name, fontSize = 10.sp, color = Color.Gray, lineHeight = 12.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(asset.balance, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text(asset.balanceUsd, fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}
