package com.antigravity.cryptowallet.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.antigravity.cryptowallet.ui.onboarding.IntroScreen

import com.antigravity.cryptowallet.ui.onboarding.SplashScreen

import com.antigravity.cryptowallet.ui.onboarding.CreateWalletScreen
import com.antigravity.cryptowallet.ui.onboarding.ImportWalletScreen

@Composable
fun WalletApp(
    startDestination: String = "intro"
) {
    val navController = rememberNavController()
    
    // The "real" screen to show after splash
    val nextDestination = startDestination 

    NavHost(
        navController = navController, 
        startDestination = "splash",
        enterTransition = {
            androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }) + androidx.compose.animation.fadeIn()
        },
        exitTransition = {
            androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it }) + androidx.compose.animation.fadeOut()
        },
        popEnterTransition = {
            androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it }) + androidx.compose.animation.fadeIn()
        },
        popExitTransition = {
            androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }) + androidx.compose.animation.fadeOut()
        }
    ) {
        composable("splash") {
            SplashScreen(
                onNavigateNext = {
                    navController.navigate(nextDestination) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("intro") {
            IntroScreen(
                onCreateWallet = { navController.navigate("create_wallet") },
                onImportWallet = { navController.navigate("import_wallet") }
            )
        }
        composable("create_wallet") {
            CreateWalletScreen(
                onWalletCreated = {
                    navController.navigate("security_setup") {
                        popUpTo("intro") { inclusive = true }
                    }
                }
            )
        }
        composable("import_wallet") {
            ImportWalletScreen(
                onWalletImported = {
                    navController.navigate("security_setup") {
                        popUpTo("intro") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            MainScreen(
                onNavigateToSecuritySetup = { navController.navigate("security_setup") },
                onNavigateToRevealSeed = { navController.navigate("reveal_seed_verify") },
                onNavigateToTransfer = { navController.navigate("transfer") },
                onNavigateToAppInfo = { navController.navigate("app_info") },
                onNavigateToAppearance = { navController.navigate("appearance") },
                onNavigateToTokenDetail = { symbol -> navController.navigate("token_detail/$symbol") },
                onNavigateToWalletConnect = { navController.navigate("wallet_connect") },
                onNavigateToRevealPrivateKey = { navController.navigate("reveal_private_key_verify") },
                onNavigateToWalletManager = { navController.navigate("wallet_manager") }
            )
        }

        composable("unlock") {
            val viewModel: com.antigravity.cryptowallet.ui.security.SecurityViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            com.antigravity.cryptowallet.ui.security.LockScreen(
                mode = com.antigravity.cryptowallet.ui.security.LockMode.UNLOCK,
                onUnlock = {
                    navController.navigate("home") {
                        popUpTo("unlock") { inclusive = true }
                    }
                },
                checkPin = { viewModel.checkPin(it) },
                biometricEnabled = viewModel.isBiometricEnabled()
            )
        }

        composable("security_setup") {
            val viewModel: com.antigravity.cryptowallet.ui.security.SecurityViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            com.antigravity.cryptowallet.ui.security.LockScreen(
                mode = com.antigravity.cryptowallet.ui.security.LockMode.SETUP,
                onPinSet = { pin ->
                    viewModel.setPin(pin)
                    navController.navigate("home") {
                        popUpTo("security_setup") { inclusive = true }
                    }
                },
                onUnlock = {}
            )
        }

        composable("reveal_seed_verify") {
            val viewModel: com.antigravity.cryptowallet.ui.security.SecurityViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            com.antigravity.cryptowallet.ui.security.LockScreen(
                mode = com.antigravity.cryptowallet.ui.security.LockMode.UNLOCK,
                onUnlock = {
                    navController.navigate("reveal_seed") {
                        popUpTo("reveal_seed_verify") { inclusive = true }
                    }
                },
                checkPin = { viewModel.checkPin(it) },
                biometricEnabled = viewModel.isBiometricEnabled()
            )
        }

        composable("reveal_seed") {
            val viewModel: com.antigravity.cryptowallet.ui.security.SecurityViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            com.antigravity.cryptowallet.ui.security.ShowSeedScreen(
                mnemonic = viewModel.getMnemonic(),
                onBack = { navController.popBackStack() }
            )
        }

        composable("reveal_private_key_verify") {
            val viewModel: com.antigravity.cryptowallet.ui.security.SecurityViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            com.antigravity.cryptowallet.ui.security.LockScreen(
                mode = com.antigravity.cryptowallet.ui.security.LockMode.UNLOCK,
                onUnlock = {
                    navController.navigate("reveal_private_key") {
                        popUpTo("reveal_private_key_verify") { inclusive = true }
                    }
                },
                checkPin = { viewModel.checkPin(it) },
                biometricEnabled = viewModel.isBiometricEnabled()
            )
        }

        composable("reveal_private_key") {
            val viewModel: com.antigravity.cryptowallet.ui.security.SecurityViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            com.antigravity.cryptowallet.ui.security.ShowPrivateKeyScreen(
                privateKey = viewModel.getPrivateKey(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "transfer?symbol={symbol}",
            arguments = listOf(
                androidx.navigation.navArgument("symbol") { 
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val symbol = backStackEntry.arguments?.getString("symbol")
            com.antigravity.cryptowallet.ui.wallet.TransferScreen(
                onBack = { navController.popBackStack() },
                onTransactionSuccess = { amount, sym, recipient, txHash, networkName ->
                    navController.navigate("transaction_success/$amount/$sym/$recipient?txHash=$txHash&networkName=$networkName")
                },
                initialSymbol = symbol
            )
        }

        composable(
            route = "transaction_success/{amount}/{symbol}/{recipient}?txHash={txHash}&networkName={networkName}",
            arguments = listOf(
                androidx.navigation.navArgument("amount") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("symbol") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("recipient") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("txHash") { 
                    type = androidx.navigation.NavType.StringType 
                    defaultValue = "Unknown"
                },
                androidx.navigation.navArgument("networkName") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = "Ethereum"
                }
            )
        ) { backStackEntry ->
            val amount = backStackEntry.arguments?.getString("amount") ?: ""
            val symbol = backStackEntry.arguments?.getString("symbol") ?: ""
            val recipient = backStackEntry.arguments?.getString("recipient") ?: ""
            val txHash = backStackEntry.arguments?.getString("txHash") ?: "Unknown"
            val networkName = backStackEntry.arguments?.getString("networkName") ?: "Ethereum"

            com.antigravity.cryptowallet.ui.wallet.TransactionSuccessScreen(
                amount = amount,
                symbol = symbol,
                recipient = recipient,
                txHash = txHash,
                networkName = networkName,
                onDone = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("wallet_connect") {
            com.antigravity.cryptowallet.ui.walletconnect.WalletConnectScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("app_info") {
            com.antigravity.cryptowallet.ui.settings.AppInfoScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("appearance") {
            com.antigravity.cryptowallet.ui.settings.AppearanceScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("wallet_manager") {
            com.antigravity.cryptowallet.ui.settings.WalletManagerScreen(
                onBack = { navController.popBackStack() },
                onAddWallet = { navController.navigate("intro") }
            )
        }
        
        // ... (unlock blocks skipped for brevity, keeping existing structure if possible or cleaning up)

        // ...

        composable(
            route = "token_detail/{symbol}",
            arguments = listOf(
                androidx.navigation.navArgument("symbol") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val symbol = backStackEntry.arguments?.getString("symbol") ?: ""
            com.antigravity.cryptowallet.ui.wallet.TokenDetailScreen(
                symbol = symbol,
                onBack = { navController.popBackStack() },
                onNavigateToSend = { navController.navigate("transfer?symbol=$symbol") }
            )
        }
    }
}
