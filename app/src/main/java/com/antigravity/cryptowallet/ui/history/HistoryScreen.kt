@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.antigravity.cryptowallet.ui.history

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.antigravity.cryptowallet.data.db.TransactionEntity
import com.antigravity.cryptowallet.ui.components.BrutalistButton
import com.antigravity.cryptowallet.ui.components.BrutalistHeader
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrutalistHeader("History")
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(
                    imageVector = Icons.Filled.Refresh, 
                    contentDescription = "Sync",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        // Network Filter Chips
        val selectedFilter by viewModel.selectedNetworkName.collectAsStateWithLifecycle()
        val networks = remember { 
            listOf("All") + viewModel.networkRepository.networks.map { it.name }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            networks.forEach { networkName ->
                FilterChip(
                    selected = selectedFilter == networkName,
                    onClick = { viewModel.selectNetwork(networkName) },
                    label = { Text(networkName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.outline,
                        borderWidth = 1.dp
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        val groupedTransactions by viewModel.groupedTransactions.collectAsStateWithLifecycle()

        if (groupedTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No transactions yet", 
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your transaction history will appear here",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                groupedTransactions.forEach { (date, txs) ->
                    item {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        )
                    }
                    items(txs) { tx ->
                        TransactionRow(
                            tx = tx,
                            onClick = { selectedTransaction = tx }
                        )
                        Divider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
    
    // Transaction Detail Dialog
    selectedTransaction?.let { tx ->
        TransactionDetailDialog(
            transaction = tx,
            onDismiss = { selectedTransaction = null }
        )
    }
}

@Composable
fun TransactionRow(
    tx: TransactionEntity,
    onClick: () -> Unit
) {
    val isReceive = tx.type == "receive"
    val isSuccess = tx.status.lowercase() == "success"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Simple circular icon with arrow
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isReceive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Type + Address
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Transfer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isSuccess) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = androidx.compose.material.icons.filled.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = if (isReceive) 
                    "From: ${tx.fromAddress.take(6)}...${tx.fromAddress.takeLast(4)}" 
                else 
                    "To: ${tx.toAddress.take(6)}...${tx.toAddress.takeLast(4)}",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
        
        // Amount + Approx
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if(isReceive) "+" else "-"} ${formatTransactionAmount(tx.value)} ${tx.symbol}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isReceive) Color(0xFF00C853) else MaterialTheme.colorScheme.onSurface
            )
            // Approx USD placeholder (actual value should come from elsewhere if available, for now show approx)
            Text(
                text = "≈ $... ", // Placeholder as we don't store USD in history yet
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun TransactionDetailDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    
    val isReceive = transaction.type == "receive"
    val statusColor = when(transaction.status.lowercase()) {
        "success" -> Color(0xFF00C853)
        "failed" -> Color(0xFFD32F2F)
        else -> Color(0xFFFFA000)
    }
    
    // Get explorer URL based on network
    val explorerUrl = when(transaction.network.lowercase()) {
        "ethereum", "eth" -> "https://etherscan.io"
        "bnb smart chain", "bsc", "bnb" -> "https://bscscan.com"
        "polygon", "matic" -> "https://polygonscan.com"
        "base" -> "https://basescan.org"
        "arbitrum", "arb" -> "https://arbiscan.io"
        "optimism", "op" -> "https://optimistic.etherscan.io"
        else -> null
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Transaction Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Status Badge
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(statusColor, androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            transaction.status.replaceFirstChar { it.uppercase() },
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Amount
                // Amount Row
                Row(
                   verticalAlignment = Alignment.CenterVertically,
                   horizontalArrangement = Arrangement.Center,
                   modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${if(isReceive) "+" else "-"} ${formatTransactionAmount(transaction.value)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isReceive) Color(0xFF00C853) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = transaction.symbol,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isReceive) Color(0xFF00C853) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Details Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DetailRow("Type", if (isReceive) "Received" else "Sent")
                        Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))
                        DetailRow("Network", transaction.network)
                        Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))
                        DetailRow("Date", SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(transaction.timestamp)))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))
                        
                        Text("Transaction Hash", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { clipboardManager.setText(AnnotatedString(transaction.hash)) }
                        ) {
                            Text(
                                text = "${transaction.hash.take(12)}...${transaction.hash.takeLast(10)}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        if (!isReceive && transaction.toAddress.isNotEmpty()) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))
                            Text("To Address", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${transaction.toAddress.take(8)}...${transaction.toAddress.takeLast(6)}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // View on Explorer Button
                if (explorerUrl != null) {
                    OutlinedButton(
                        onClick = { uriHandler.openUri("$explorerUrl/tx/${transaction.hash}") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View on Explorer")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                BrutalistButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatTransactionAmount(value: String): String {
    return try {
        val bd = java.math.BigDecimal(value)
        // If it's too small, limit decimals but keep enough for user to see
        val scaled = if (bd.abs() < java.math.BigDecimal("0.0001") && bd.abs() > java.math.BigDecimal.ZERO) {
            bd.setScale(8, java.math.BigDecimal.ROUND_HALF_UP)
        } else {
            bd.setScale(4, java.math.BigDecimal.ROUND_HALF_UP)
        }
        scaled.stripTrailingZeros().toPlainString()
    } catch (e: Exception) {
        value
    }
}
