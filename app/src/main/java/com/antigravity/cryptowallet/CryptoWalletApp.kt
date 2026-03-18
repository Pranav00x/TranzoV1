package com.antigravity.cryptowallet

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet

@HiltAndroidApp
class CryptoWalletApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize WalletConnect / Reown
        val projectId = BuildConfig.WALLETCONNECT_PROJECT_ID
        if (projectId.isEmpty()) {
            if (BuildConfig.DEBUG) {
                println("WalletConnect disabled: WALLETCONNECT_PROJECT_ID is not set")
            }
            return
        }
        val relayServerUrl = "wss://relay.walletconnect.com?projectId=$projectId"
        val connectionType = ConnectionType.AUTOMATIC
        
        val appMetaData = Core.Model.AppMetaData(
            name = "Tranzo Wallet",
            description = "A non-custodial multi-chain Android wallet",
            url = "https://github.com/Pranav00x/TranzoV1",
            icons = listOf("https://cryptologos.cc/logos/cosmos-atom-logo.png"),
            redirect = "tranzo://"
        )
        
        CoreClient.initialize(
            relayServerUrl = relayServerUrl,
            connectionType = connectionType,
            application = this,
            metaData = appMetaData
        ) { error ->
            if (BuildConfig.DEBUG) {
                println("WalletConnect init error: ${error.throwable.stackTraceToString()}")
            }
        }
        
        Web3Wallet.initialize(Wallet.Params.Init(core = CoreClient)) { error ->
            if (BuildConfig.DEBUG) {
                println("Web3Wallet init error: ${error.throwable.stackTraceToString()}")
            }
        }
    }
}
