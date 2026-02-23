package com.antigravity.cryptowallet.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.cryptowallet.ui.components.FluidHeader

@Composable
fun SettingsScreen(
    onSetupSecurity: () -> Unit,
    onViewSeedPhrase: () -> Unit,
    onRevealPrivateKey: () -> Unit,
    onViewAppInfo: () -> Unit,
    onAppearance: () -> Unit,
    onWalletConnect: () -> Unit,
    onManageWallets: () -> Unit,
    viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        FluidHeader("Settings")
        
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                SettingsSection("Wallet")
                SettingsItem(
                    title = "Manage Wallets",
                    subtitle = "Add, switch, or remove wallets",
                    icon = Icons.Default.AccountBalanceWallet,
                    onClick = onManageWallets
                )
            }
            
            item {
                SettingsSection("Security")
                SettingsItem(
                    title = "PIN & Biometrics",
                    subtitle = "Secure your wallet",
                    icon = Icons.Default.Lock,
                    onClick = onSetupSecurity
                )
                if (viewModel.hasMnemonic()) {
                    SettingsItem(
                        title = "Reveal Seed Phrase",
                        subtitle = "Backup your 12-word phrase",
                        icon = Icons.Default.VpnKey,
                        onClick = onViewSeedPhrase
                    )
                }
                SettingsItem(
                    title = "Reveal Private Key",
                    subtitle = "Sensitive access key",
                    icon = Icons.Default.Lock,
                    onClick = onRevealPrivateKey
                )
            }

            item {
                SettingsSection("App")
                SettingsItem(
                    title = "Wallet Connect",
                    subtitle = "Connect to dApps",
                    icon = Icons.Default.QrCodeScanner,
                    onClick = onWalletConnect
                )
                SettingsItem(
                    title = "Appearance",
                    subtitle = "Themes & Fonts",
                    icon = Icons.Default.Palette,
                    onClick = onAppearance
                )
                SettingsItem(
                    title = "About",
                    subtitle = "Version & Info",
                    icon = Icons.Default.Info,
                    onClick = onViewAppInfo
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title, 
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle, 
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
