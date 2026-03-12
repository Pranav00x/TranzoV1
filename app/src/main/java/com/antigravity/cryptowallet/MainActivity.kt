package com.antigravity.cryptowallet

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.antigravity.cryptowallet.data.repository.ThemeRepository
import com.antigravity.cryptowallet.ui.WalletApp
import com.antigravity.cryptowallet.ui.theme.CryptoWalletTheme
import com.antigravity.cryptowallet.ui.theme.ThemeType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import androidx.lifecycle.lifecycleScope
import com.antigravity.cryptowallet.data.wallet.WalletRepository
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : androidx.fragment.app.FragmentActivity() {

    @Inject
    lateinit var secureStorage: com.antigravity.cryptowallet.data.security.SecureStorage
    
    @Inject
    lateinit var walletRepository: WalletRepository
    
    @Inject
    lateinit var themeRepository: ThemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        // Initialize wallet asynchronously
        lifecycleScope.launch {
            walletRepository.loadWallet()
        }
        
        val startDestination = if (secureStorage.hasWallet()) {
            if (secureStorage.hasPin()) "unlock" else "security_setup"
        } else {
            "intro"
        }

        setContent {
            // Directly observe the theme repository for changes
            val currentTheme by themeRepository.currentTheme.collectAsState()
            
            CryptoWalletTheme(themeType = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    WalletApp(startDestination = startDestination)
                }
            }
        }
    }
}
