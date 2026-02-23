package com.antigravity.cryptowallet.ui.wallet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.antigravity.cryptowallet.ui.components.BrutalistButton
import com.antigravity.cryptowallet.ui.components.BrutalistHeader
import com.antigravity.cryptowallet.utils.QrCodeGenerator
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun TokenDetailScreen(
    symbol: String,
    onBack: () -> Unit,
    onNavigateToSend: () -> Unit,
    viewModel: TokenDetailViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var showReceiveDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    
    LaunchedEffect(symbol) {
        viewModel.loadTokenData(symbol)
    }

    val price = viewModel.price
    val description = viewModel.description
    val contractAddress = viewModel.contractAddress
    val points = viewModel.graphPoints
    val transactions = viewModel.transactions
    val walletAddress = viewModel.walletAddress
    
    val isPositive = if (points.size > 1) points.last() >= points.first() else true
    val trendColor = if (isPositive) Color(0xFF00C853) else Color.Red

    // Receive Dialog
    if (showReceiveDialog) {
        Dialog(onDismissRequest = { showReceiveDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Receive $symbol",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (walletAddress.length > 10) {
                        val qrBitmap = remember(walletAddress) {
                            QrCodeGenerator.generateQrCode(walletAddress)
                        }
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Wallet Address QR Code",
                            modifier = Modifier.size(180.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = walletAddress,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboardManager.setText(AnnotatedString(walletAddress))
                            }
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    )
                    
                    Text(
                        text = "Tap to copy",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    BrutalistButton(
                        text = "Close",
                        onClick = { showReceiveDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            BrutalistHeader(symbol)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Price
        Text("Current Price", fontSize = 12.sp, color = Color.Gray)
        Text(price, fontSize = 48.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
        
        Spacer(modifier = Modifier.height(16.dp))

        // Balance
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Balance: ",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = viewModel.balance,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // Graph Section
        val ohlc = viewModel.ohlcData
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            if (ohlc.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    val maxOverall = ohlc.maxOf { it[2] }
                    val minOverall = ohlc.minOf { it[3] }
                    val range = (maxOverall - minOverall).coerceAtLeast(0.0001)
                    
                    val candleWidth = w / ohlc.size
                    val spacing = candleWidth * 0.2f
                    
                    ohlc.forEachIndexed { i, candle ->
                        val open = candle[1]
                        val high = candle[2]
                        val low = candle[3]
                        val close = candle[4]
                        
                        val isGreen = close >= open
                        val color = if (isGreen) Color(0xFF00C853) else Color.Red
                        
                        val x = i * candleWidth + spacing / 2
                        
                        fun normalize(v: Double) = h - ((v - minOverall).toFloat() / range.toFloat()) * h
                        
                        val yHigh = normalize(high)
                        val yLow = normalize(low)
                        val yOpen = normalize(open)
                        val yClose = normalize(close)
                        
                        drawLine(
                            color = color,
                            start = Offset(x + (candleWidth - spacing) / 2, yHigh),
                            end = Offset(x + (candleWidth - spacing) / 2, yLow),
                            strokeWidth = 1.dp.toPx()
                        )
                        
                        val top = kotlin.math.min(yOpen, yClose)
                        val bottom = kotlin.math.max(yOpen, yClose)
                        val rectHeight = (bottom - top).coerceAtLeast(1f)
                        
                        drawRect(
                            color = color,
                            topLeft = Offset(x, top),
                            size = Size(candleWidth - spacing, rectHeight)
                        )
                    }
                }
            } else {
                 Text("Loading 1Y Chart...", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Contract Address
        if (contractAddress.isNotEmpty()) {
            Text("Contract Address", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { clipboardManager.setText(AnnotatedString(contractAddress)) }
                    .padding(12.dp)
            ) {
                Text(
                    text = contractAddress,
                    modifier = Modifier.weight(1f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Icon(
                    imageVector = Icons.Default.ContentCopy, 
                    contentDescription = "Copy",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Description
        Text("About $symbol", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            description, 
            fontSize = 12.sp, 
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BrutalistButton(
                text = "Send",
                onClick = onNavigateToSend,
                icon = Icons.Default.ArrowOutward,
                modifier = Modifier.weight(1f)
            )
            BrutalistButton(
                text = "Receive",
                onClick = { showReceiveDialog = true },
                icon = Icons.Default.FileDownload,
                modifier = Modifier.weight(1f),
                inverted = true
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // History
        Text("Transaction History", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (transactions.isEmpty()) {
            Text("No transactions found", color = Color.Gray, fontSize = 12.sp)
        } else {
            transactions.forEach { tx ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(if (tx.type == "send") "Sent" else "Received", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text(tx.hash.take(8) + "...", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${tx.value} $symbol", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text(tx.status, fontSize = 10.sp, color = if (tx.status == "success") Color(0xFF00C853) else Color.Red)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
