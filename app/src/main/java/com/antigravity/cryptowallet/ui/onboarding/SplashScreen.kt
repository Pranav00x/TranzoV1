package com.antigravity.cryptowallet.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateNext: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    val slideY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 40f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "slideY"
    )

    val lineProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 700, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "line"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2000)
        onNavigateNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(alpha)
                .offset(y = slideY.dp)
        ) {
            // Bar mark
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                listOf(18.dp, 30.dp, 24.dp, 36.dp).forEach { h ->
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(h)
                            .background(Color.White)
                    )
                }
            }

            Text(
                text = "TRANZO",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Animated divider
            Box(
                modifier = Modifier
                    .height(1.dp)
                    .width(160.dp)
                    .drawBehind {
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width * lineProgress, 0f),
                            strokeWidth = 1f
                        )
                    }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "non-custodial · multichain",
                color = Color.Gray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 36.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "by Tranzo",
                color = Color.DarkGray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}
