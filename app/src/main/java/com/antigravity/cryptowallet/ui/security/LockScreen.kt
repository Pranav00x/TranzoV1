package com.antigravity.cryptowallet.ui.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import android.content.Context

enum class LockMode {
    SETUP,
    UNLOCK
}

@Composable
fun LockScreen(
    mode: LockMode,
    onPinSet: (String) -> Unit = {},
    onUnlock: () -> Unit,
    checkPin: (String) -> Boolean = { false },
    biometricEnabled: Boolean = false
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(if (mode == LockMode.SETUP) 0 else 1) }
    var error by remember { mutableStateOf<String?>(null) }
    var shake by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    fun Context.findFragmentActivity(): FragmentActivity? {
        var ctx = this
        while (ctx is android.content.ContextWrapper) {
            if (ctx is FragmentActivity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun showBiometricPrompt() {
        if (!biometricEnabled) return
        
        val fragmentActivity = context.findFragmentActivity()
        if (fragmentActivity != null) {
            val biometricManager = BiometricManager.from(context)
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                val executor = ContextCompat.getMainExecutor(context)
                val biometricPrompt = BiometricPrompt(fragmentActivity, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            onUnlock()
                        }
                    })

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Wallet")
                    .setSubtitle("Use biometric credential")
                    .setNegativeButtonText("Use PIN")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (mode == LockMode.UNLOCK && biometricEnabled) {
            showBiometricPrompt()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo/Icon Area
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔐",
                    fontSize = 36.sp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = when {
                    mode == LockMode.UNLOCK -> "Welcome Back"
                    step == 0 -> "Create PIN"
                    else -> "Confirm PIN"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = when {
                    mode == LockMode.UNLOCK -> "Enter your PIN to unlock"
                    step == 0 -> "Set a 4-digit PIN to secure your wallet"
                    else -> "Re-enter your PIN to confirm"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                repeat(4) { index ->
                    val isFilled = pin.length > index
                    val dotScale by animateFloatAsState(
                        targetValue = if (isFilled) 1.2f else 1f,
                        animationSpec = tween(100),
                        label = "dot"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isFilled) MaterialTheme.colorScheme.primary else Color.LightGray,
                        label = "dotColor"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .scale(dotScale)
                            .background(dotColor, CircleShape)
                    )
                }
            }
            
            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Numpad
            val buttons = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(if (biometricEnabled) "Bio" else "", "0", "Del")
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                buttons.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { label ->
                            if (label.isEmpty()) {
                                Spacer(modifier = Modifier.weight(1f))
                            } else {
                                NumpadButton(
                                    label = label,
                                    biometricEnabled = biometricEnabled,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        error = null
                                        when (label) {
                                            "Del" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            "Bio" -> if (biometricEnabled) showBiometricPrompt()
                                            else -> {
                                                if (pin.length < 4) {
                                                    pin += label
                                                    if (pin.length == 4) {
                                                        if (mode == LockMode.UNLOCK) {
                                                            if (checkPin(pin)) {
                                                                onUnlock()
                                                            } else {
                                                                error = "Incorrect PIN"
                                                                pin = ""
                                                            }
                                                        } else {
                                                            if (step == 0) {
                                                                confirmPin = pin
                                                                pin = ""
                                                                step = 1
                                                            } else {
                                                                if (pin == confirmPin) {
                                                                    onPinSet(pin)
                                                                } else {
                                                                    error = "PINs don't match. Try again."
                                                                    pin = ""
                                                                    step = 0
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NumpadButton(
    label: String,
    biometricEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(50),
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                when (label) {
                    "Bio", "Del" -> Color.Transparent
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
                isPressed = false
            },
        contentAlignment = Alignment.Center
    ) {
        when (label) {
            "Del" -> Icon(
                Icons.Default.Backspace,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )
            "Bio" -> if (biometricEnabled) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = "Biometric",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            else -> Text(
                text = label,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
