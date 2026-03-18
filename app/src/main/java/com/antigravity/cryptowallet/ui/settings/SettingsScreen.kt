package com.antigravity.cryptowallet.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    onSetupSecurity: () -> Unit,
    onViewSeedPhrase: () -> Unit,
    onRevealPrivateKey: () -> Unit,
    onViewAppInfo: () -> Unit,
    onAppearance: () -> Unit,
    onWalletConnect: () -> Unit,
    onManageWallets: () -> Unit,
    onNavigateToSupport: () -> Unit = {},
    viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Text(
                "Settings",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Wallet ────────────────────────────────────────────────────────
        item { SectionLabel("Wallet") }
        item {
            SettingsItem(
                title = "Manage Wallets",
                subtitle = "Add, switch or remove wallets",
                icon = Icons.Default.AccountBalanceWallet,
                onClick = onManageWallets
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        // ── Security ──────────────────────────────────────────────────────
        item { SectionLabel("Security") }
        item {
            SettingsItem(
                title = "PIN & Biometrics",
                subtitle = "Secure your wallet",
                icon = Icons.Default.Lock,
                onClick = onSetupSecurity
            )
        }
        if (viewModel.hasMnemonic()) {
            item {
                SettingsItem(
                    title = "Reveal Seed Phrase",
                    subtitle = "Backup your 12-word recovery phrase",
                    icon = Icons.Default.VpnKey,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onViewSeedPhrase
                )
            }
        }
        item {
            SettingsItem(
                title = "Reveal Private Key",
                subtitle = "Raw private key — handle with care",
                icon = Icons.Default.Key,
                tint = MaterialTheme.colorScheme.error,
                onClick = onRevealPrivateKey
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        // ── App ───────────────────────────────────────────────────────────
        item { SectionLabel("App") }
        item {
            SettingsItem(
                title = "WalletConnect",
                subtitle = "Connect to dApps via QR code",
                icon = Icons.Default.QrCodeScanner,
                onClick = onWalletConnect
            )
        }
        item {
            SettingsItem(
                title = "Appearance",
                subtitle = "Theme & display options",
                icon = Icons.Default.Palette,
                onClick = onAppearance
            )
        }
        item {
            SettingsItem(
                title = "About",
                subtitle = "Version & build info",
                icon = Icons.Default.Info,
                onClick = onViewAppInfo
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        // ── Support ───────────────────────────────────────────────────────
        item { SectionLabel("Support") }
        item {
            SettingsItem(
                title = "Support & Legal",
                subtitle = "Contact, legal & privacy info",
                icon = Icons.Default.HeadsetMic,
                onClick = onNavigateToSupport
            )
        }
        item {
            SettingsItem(
                title = "Source Code",
                subtitle = "github.com/Pranav00x/TranzoV1",
                icon = Icons.Default.Code,
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Pranav00x/TranzoV1"))
                    )
                }
            )
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun SectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
        letterSpacing = 2.sp,
        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val iconTint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onBackground else tint

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
        )
    }
}
