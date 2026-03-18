package com.antigravity.cryptowallet

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.antigravity.cryptowallet.data.repository.FontRepository
import com.antigravity.cryptowallet.data.repository.ThemeRepository
import com.antigravity.cryptowallet.ui.WalletApp
import com.antigravity.cryptowallet.ui.theme.CryptoWalletTheme
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

    @Inject
    lateinit var fontRepository: FontRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        lifecycleScope.launch {
            walletRepository.loadWallet()
        }

        val startDestination = if (secureStorage.hasWallet()) {
            if (secureStorage.hasPin()) "unlock" else "security_setup"
        } else {
            "intro"
        }

        setContent {
            val currentTheme by themeRepository.currentTheme.collectAsState()
            val currentFont by fontRepository.currentFont.collectAsState()

            CryptoWalletTheme(themeType = currentTheme, fontType = currentFont) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WalletApp(startDestination = startDestination)
                }
            }
        }
    }
}
