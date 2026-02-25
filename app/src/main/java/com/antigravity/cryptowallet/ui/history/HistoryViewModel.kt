package com.antigravity.cryptowallet.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.cryptowallet.data.blockchain.NetworkRepository
import com.antigravity.cryptowallet.data.wallet.TransactionRepository
import com.antigravity.cryptowallet.data.wallet.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val walletRepository: WalletRepository,
    private val networkRepository: NetworkRepository
) : ViewModel() {

    val transactions = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refresh() {
        viewModelScope.launch {
            if (!walletRepository.isWalletCreated()) return@launch
            
            networkRepository.networks.forEach { network ->
                val address = walletRepository.getAddress(network.id)
                repository.refreshTransactions(address, network)
            }
        }
    }

    init {
        refresh()
    }
}
