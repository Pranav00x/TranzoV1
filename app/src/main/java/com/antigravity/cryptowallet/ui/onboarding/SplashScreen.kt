package com.antigravity.cryptowallet.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateNext: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    // Decrypting Text Effect
    val targetText = "FLOWSTABLE"
    var displayedText by remember { mutableStateOf("") }
    val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*"
    
    LaunchedEffect(Unit) {
        startAnimation = true
        
        // Decrypt text
        var ticks = 0
        while (ticks < 20) {
            displayedText = (0 until targetText.length).map { 
                characters.random() 
            }.joinToString("")
            delay(50)
            ticks++
        }
        
        // Reveal text one by one
        for (i in 1..targetText.length) {
            displayedText = targetText.take(i) + (0 until (targetText.length - i)).map { 
                characters.random() 
            }.joinToString("")
            delay(50)
        }
        
        delay(1000)
        onNavigateNext()
    }

    // Background Grid Animation
    val infiniteTransition = rememberInfiniteTransition(label = "grid")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // Techy Dark Background
        contentAlignment = Alignment.Center
    ) {
        // Grid Background
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().alpha(0.2f)) {
            val step = 40.dp.toPx()
            for (x in 0..size.width.toInt() step step.toInt()) {
                drawLine(
                    color = Color.Green,
                    start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f),
                    end = androidx.compose.ui.geometry.Offset(x.toFloat(), size.height),
                    strokeWidth = 1f
                )
            }
            for (y in 0..size.height.toInt() step step.toInt()) {
                val animatedY = (y + offsetY) % size.height
                drawLine(
                    color = Color.Green,
                    start = androidx.compose.ui.geometry.Offset(0f, animatedY),
                    end = androidx.compose.ui.geometry.Offset(size.width, animatedY),
                    strokeWidth = 1f
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Hexagon/Tech Logo Placeholder
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .border(2.dp, Color.Green, RoundedCornerShape(20.dp))
                    .rotate(45f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.Green.copy(alpha = 0.2f))
                        .border(1.dp, Color.Green, RoundedCornerShape(10.dp))
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Decrypting Text
            Text(
                text = displayedText,
                style = MaterialTheme.typography.displayMedium,
                color = Color.Green,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "SYSTEM READY",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Green.copy(alpha = 0.5f),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}
