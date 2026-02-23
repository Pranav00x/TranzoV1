package com.antigravity.cryptowallet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.antigravity.cryptowallet.ui.components.BottomNavItem
import com.antigravity.cryptowallet.ui.components.BrutalistBottomBar
import com.antigravity.cryptowallet.ui.theme.BrutalBlack
import com.antigravity.cryptowallet.ui.wallet.WalletScreen
import com.antigravity.cryptowallet.ui.history.HistoryScreen

@Composable
fun MainScreen(
    onNavigateToSecuritySetup: () -> Unit,
    onNavigateToRevealSeed: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToAppInfo: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToTokenDetail: (String) -> Unit,
    onNavigateToWalletConnect: () -> Unit,
    onNavigateToRevealPrivateKey: () -> Unit,
    onNavigateToWalletManager: () -> Unit
) {
    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem("Wallet", Icons.Filled.Home, "wallet"),
        BottomNavItem("Card", Icons.Filled.CreditCard, "card"),
        BottomNavItem("Browser", Icons.Filled.Public, "browser"),
        BottomNavItem("History", Icons.Filled.History, "history"),
        BottomNavItem("Settings", Icons.Filled.Settings, "settings")
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            BrutalistBottomBar(
                items = items,
                currentRoute = currentRoute,
                onItemClick = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "wallet",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("wallet") {
                WalletScreen(
                    onSetupSecurity = onNavigateToSecuritySetup,
                    onNavigateToSend = onNavigateToTransfer,
                    onNavigateToTokenDetail = onNavigateToTokenDetail
                )
            }
            composable("card") {
                com.antigravity.cryptowallet.ui.card.CardScreen()
            }
            composable("browser") {
                com.antigravity.cryptowallet.ui.browser.BrowserScreen()
            }
            composable("history") {
                HistoryScreen()
            }
            composable("settings") {
                com.antigravity.cryptowallet.ui.settings.SettingsScreen(
                    onSetupSecurity = onNavigateToSecuritySetup,
                    onViewSeedPhrase = onNavigateToRevealSeed,
                    onRevealPrivateKey = onNavigateToRevealPrivateKey,
                    onViewAppInfo = onNavigateToAppInfo,
                    onAppearance = onNavigateToAppearance,
                    onWalletConnect = onNavigateToWalletConnect,
                    onManageWallets = onNavigateToWalletManager
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = BrutalBlack, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
