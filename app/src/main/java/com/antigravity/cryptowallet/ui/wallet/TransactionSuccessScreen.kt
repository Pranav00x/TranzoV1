package com.antigravity.cryptowallet.ui.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.cryptowallet.ui.components.BrutalistButton

enum class TransactionStatus {
    SUCCESS,
    FAILED,
    PENDING
}

@Composable
fun TransactionResultScreen(
    status: TransactionStatus = TransactionStatus.SUCCESS,
    amount: String,
    symbol: String,
    recipient: String,
    txHash: String,
    networkName: String = "Ethereum",
    explorerUrl: String? = null,
    onDone: () -> Unit
) {
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = true
    
    val transition = updateTransition(transitionState, label = "ResultTransition")
    
    val circleProgress by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 600, easing = FastOutSlowInEasing) },
        label = "Circle"
    ) { if (it) 1f else 0f }
    
    val iconProgress by transition.animateFloat(
        transitionSpec = { 
            tween(durationMillis = 400, delayMillis = 600, easing = LinearOutSlowInEasing) 
        },
        label = "Icon"
    ) { if (it) 1f else 0f }

    // Modern Dark Theme Colors
    val successColor = Color(0xFF00E676) // Bright Green
    val errorColor = Color(0xFFFF5252)   // Bright Red
    val warningColor = Color(0xFFFFC400) // Amber
    
    val primaryColor = when(status) {
        TransactionStatus.SUCCESS -> successColor
        TransactionStatus.FAILED -> errorColor
        TransactionStatus.PENDING -> warningColor
    }
    
    val backgroundColor = Color(0xFF121212) // Deep Dark Background
    val cardBackgroundColor = Color(0xFF1E1E1E) // Slightly lighter for cards
    val contentColor = Color.White
    
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.1f))

        // Large Animated Header Icon
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 10.dp.toPx()
                val radius = size.minDimension / 2 - strokeWidth / 2
                
                // Draw Glowing Background Circle (Subtle)
                drawCircle(
                    color = primaryColor.copy(alpha = 0.1f),
                    radius = radius + strokeWidth
                )

                // Draw Animated Arc Stroke
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = 360f * circleProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Draw Icon based on status
                if (iconProgress > 0) {
                    when(status) {
                        TransactionStatus.SUCCESS -> {
                            val p1 = Offset(center.x - radius * 0.4f, center.y)
                            val p2 = Offset(center.x - radius * 0.1f, center.y + radius * 0.3f)
                            val p3 = Offset(center.x + radius * 0.5f, center.y - radius * 0.4f)
                            
                            val totalLen1 = (p2 - p1).getDistance()
                            val totalLen2 = (p3 - p2).getDistance()
                            val totalLen = totalLen1 + totalLen2
                            val currentLen = totalLen * iconProgress
                            
                            if (currentLen > 0) {
                                val end1 = if (currentLen > totalLen1) p2 else p1 + (p2 - p1) * (currentLen / totalLen1)
                                drawLine(primaryColor, p1, end1, strokeWidth, StrokeCap.Round)
                            }
                            if (currentLen > totalLen1) {
                                val len2 = currentLen - totalLen1
                                val end2 = p2 + (p3 - p2) * (len2 / totalLen2)
                                drawLine(primaryColor, p2, end2, strokeWidth, StrokeCap.Round)
                            }
                        }
                        TransactionStatus.FAILED -> {
                            val offset = radius * 0.35f * iconProgress
                            drawLine(primaryColor, 
                                Offset(center.x - offset, center.y - offset),
                                Offset(center.x + offset, center.y + offset),
                                strokeWidth, StrokeCap.Round)
                            drawLine(primaryColor,
                                Offset(center.x + offset, center.y - offset),
                                Offset(center.x - offset, center.y + offset),
                                strokeWidth, StrokeCap.Round)
                        }
                        TransactionStatus.PENDING -> {
                            val dotRadius = 8.dp.toPx()
                            for (i in 0..2) {
                                val angle = Math.PI / 2 + i * Math.PI * 2 / 3
                                val dotX = center.x + (radius * 0.5f * kotlin.math.cos(angle)).toFloat()
                                val dotY = center.y + (radius * 0.5f * kotlin.math.sin(angle)).toFloat()
                                drawCircle(primaryColor.copy(alpha = 0.3f + 0.7f * iconProgress), dotRadius, Offset(dotX, dotY))
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // Text Content
        AnimatedVisibility(
            visibleState = transitionState,
            enter = fadeIn(tween(800, 600)) + slideInVertically(tween(800, 600)) { 50 }
        ) {
             Column(
                 horizontalAlignment = Alignment.CenterHorizontally,
                 modifier = Modifier.padding(horizontal = 24.dp)
             ) {
                Text(
                    text = when(status) {
                        TransactionStatus.SUCCESS -> "Sent Successfully"
                        TransactionStatus.FAILED -> "Transaction Failed"
                        TransactionStatus.PENDING -> "Processing..."
                    },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Amount Display - Row to prevent overlap
                Row(
                    verticalAlignment = Alignment.CenterVertically, // Align baselines for better look
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = simpleFormatAmount(amount),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false) // Allow shrinking if really massive
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = symbol,
                        fontSize = 24.sp, // Smaller symbol
                        fontWeight = FontWeight.Medium,
                        color = primaryColor,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Details Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // To Item
                        DetailRow(
                            label = "To",
                            value = "${recipient.take(8)}...${recipient.takeLast(6)}",
                            iconVector = null,
                            isCopyable = true,
                            onCopy = { clipboardManager.setText(AnnotatedString(recipient)) }
                        )

                        Divider(color = Color.White.copy(alpha = 0.05f))

                        // Network Item
                        DetailRow(
                           label = "Network",
                            value = networkName,
                            valueColor = contentColor.copy(alpha = 0.9f)
                        )

                        Divider(color = Color.White.copy(alpha = 0.05f))

                        // Hash Item
                        DetailRow(
                            label = "Transaction Hash",
                            value = "${txHash.take(8)}...${txHash.takeLast(8)}",
                            isCopyable = true,
                            onCopy = { clipboardManager.setText(AnnotatedString(txHash)) },
                            onLink = if (explorerUrl != null) { { uriHandler.openUri("$explorerUrl/tx/$txHash") } } else null
                        )
                    }
                }
             }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        // Done Button
        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
            BrutalistButton(
                text = "DONE",
                onClick = onDone,
                textColor = if(status == TransactionStatus.SUCCESS) Color.Black else Color.White,
                backgroundColor = primaryColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Helper to prevent scientific notation for small numbers or truncate huge ones
fun simpleFormatAmount(amount: String): String {
    return try {
        val bd = java.math.BigDecimal(amount)
        bd.stripTrailingZeros().toPlainString()
    } catch (e: Exception) {
        if (amount.length > 12) amount.take(12) + "..." else amount
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = Color.White,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isCopyable: Boolean = false,
    onCopy: (() -> Unit)? = null,
    onLink: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f),
            fontWeight = FontWeight.Normal
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                color = valueColor,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clickable(enabled = onLink != null) { onLink?.invoke() }
            )
            
            if (isCopyable && onCopy != null) {
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (onLink != null) {
                Spacer(modifier = Modifier.width(4.dp))
                 Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = "Open",
                    tint = Color.Blue.copy(alpha = 0.8f), // Specific link color
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onLink.invoke() }
                )
            }
        }
    }
}

// Keep old function for backward compatibility
@Composable
fun TransactionSuccessScreen(
    amount: String,
    symbol: String,
    recipient: String,
    txHash: String,
    networkName: String = "Ethereum",
    onDone: () -> Unit
) {
    TransactionResultScreen(
        status = TransactionStatus.SUCCESS,
        amount = amount,
        symbol = symbol,
        recipient = recipient,
        txHash = txHash,
        networkName = networkName,
        onDone = onDone
    )
}
