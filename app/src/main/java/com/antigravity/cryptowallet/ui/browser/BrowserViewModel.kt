package com.antigravity.cryptowallet.ui.browser

import androidx.lifecycle.ViewModel
import com.antigravity.cryptowallet.data.blockchain.NetworkRepository
import com.antigravity.cryptowallet.data.wallet.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    val walletRepository: WalletRepository,
    val networkRepository: NetworkRepository
) : ViewModel() {
    val activeNetwork get() = networkRepository.activeNetwork
    val networks = networkRepository.networks
    
    fun switchNetwork(id: String) {
        networkRepository.setActiveNetwork(id)
    }
}
