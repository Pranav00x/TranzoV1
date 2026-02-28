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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
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
    assetId: String,
    onBack: () -> Unit,
    onNavigateToSend: () -> Unit,
    viewModel: TokenDetailViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var showReceiveDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    
    LaunchedEffect(assetId) {
        viewModel.loadTokenData(assetId)
    }

    val symbol = viewModel.symbol
    val price = viewModel.price
    val description = viewModel.description
    val contractAddress = viewModel.contractAddress
    val points = viewModel.graphPoints
    val transactions = viewModel.transactions
    val walletAddress = viewModel.walletAddress
    val networkName = viewModel.networkName
    
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
                        "Receive $symbol on $networkName",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
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
            BrutalistHeader("$symbol ($networkName)")
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
        var selectedPrice by remember { mutableStateOf<Double?>(null) }
        var selectedTime by remember { mutableStateOf<Long?>(null) }
        
        // Timeframe Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val timeframes = listOf("1D", "7D", "1M", "1Y", "ALL")
            timeframes.forEach { tf ->
                val isSelected = viewModel.selectedTimeframe.uppercase() == tf
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.setTimeframeAndReload(tf) }
                ) {
                    Text(
                        text = tf,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.background else Color.Gray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (selectedPrice != null) {
            Text(
                text = String.format("$%.2f", selectedPrice),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = trendColor
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .pointerInput(ohlc) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (ohlc.isEmpty()) return@detectDragGestures
                            val index = (offset.x / size.width * ohlc.size).toInt().coerceIn(0, ohlc.size - 1)
                            selectedPrice = ohlc[index][4]
                            selectedTime = ohlc[index][0].toLong()
                        },
                        onDrag = { change, _ ->
                            if (ohlc.isEmpty()) return@detectDragGestures
                            val index = (change.position.x / size.width * ohlc.size).toInt().coerceIn(0, ohlc.size - 1)
                            selectedPrice = ohlc[index][1]
                            selectedTime = ohlc[index][0].toLong()
                        },
                        onDragEnd = {
                            selectedPrice = null
                            selectedTime = null
                        },
                        onDragCancel = {
                            selectedPrice = null
                            selectedTime = null
                        }
                    )
                }
        ) {
            if (ohlc.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    val maxOverall = ohlc.maxOf { it[2] }
                    val minOverall = ohlc.minOf { it[3] }
                    val range = (maxOverall - minOverall).coerceAtLeast(0.0001)
                    
                    val useLineChart = ohlc.size > 100 // Smooth line for long timeframes
                    
                    if (useLineChart) {
                        val path = androidx.compose.ui.graphics.Path()
                        ohlc.forEachIndexed { i, candle ->
                            val x = i * (w / (ohlc.size - 1))
                            val y = h - ((candle[4] - minOverall).toFloat() / range.toFloat()) * h
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path = path,
                            color = trendColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    } else {
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
                }
            } else {
                 Text("Loading Chart...", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
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
            Column {
                transactions.forEach { tx ->
                    DetailTransactionRow(tx = tx, symbol = symbol)
                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DetailTransactionRow(tx: com.antigravity.cryptowallet.data.db.TransactionEntity, symbol: String) {
    val isReceive = tx.type == "receive"
    val isSuccess = tx.status.lowercase() == "success"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isReceive) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Transfer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isSuccess) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Text(
                text = if (isReceive) "From: ${tx.fromAddress.take(6)}...${tx.fromAddress.takeLast(4)}" else "To: ${tx.toAddress.take(6)}...${tx.toAddress.takeLast(4)}",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
        
        // Amount
        Column(horizontalAlignment = Alignment.End) {
            val formattedAmount = try {
                val bd = java.math.BigDecimal(tx.value)
                val scaled = if (bd.abs() < java.math.BigDecimal("0.0001") && bd.abs() > java.math.BigDecimal.ZERO) {
                    bd.setScale(8, java.math.BigDecimal.ROUND_HALF_UP)
                } else {
                    bd.setScale(4, java.math.BigDecimal.ROUND_HALF_UP)
                }
                scaled.stripTrailingZeros().toPlainString()
            } catch (e: Exception) { tx.value }

            Text(
                text = "${if(isReceive) "+" else "-"} $formattedAmount $symbol",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isReceive) Color(0xFF00C853) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = tx.status.replaceFirstChar { it.uppercase() },
                fontSize = 10.sp,
                color = if (isSuccess) Color(0xFF00C853) else Color.Red
            )
        }
    }
}
