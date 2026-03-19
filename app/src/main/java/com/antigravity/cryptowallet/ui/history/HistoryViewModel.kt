package com.antigravity.cryptowallet.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.cryptowallet.data.blockchain.NetworkRepository
import com.antigravity.cryptowallet.data.wallet.TransactionRepository
import com.antigravity.cryptowallet.data.wallet.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val walletRepository: WalletRepository,
    val networkRepository: NetworkRepository
) : ViewModel() {

    private val _selectedNetworkName = MutableStateFlow("All")
    val selectedNetworkName = _selectedNetworkName.asStateFlow()

    val groupedTransactions = combine(repository.transactions, _selectedNetworkName) { txs, filter ->
        val filtered = if (filter == "All") txs 
        else txs.filter { it.network.equals(filter, ignoreCase = true) }
        
        filtered.groupBy { tx ->
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(tx.timestamp))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectNetwork(name: String) {
        _selectedNetworkName.value = name
    }

    private var isRefreshing = false
    fun refresh() {
        if (isRefreshing) return
        viewModelScope.launch {
            isRefreshing = true
            try {
                if (!walletRepository.isWalletCreated()) return@launch
                
                networkRepository.networks.forEach { network ->
                    val address = walletRepository.getAddress(network.id)
                    // Refresh native transactions
                    repository.refreshTransactions(address, network, action = "txlist")
                    delay(500) // Small delay between calls to avoid rate limits
                    // Refresh all token transactions for this address
                    repository.refreshTransactions(address, network, action = "tokentx")
                    delay(1000) // Stagger between networks
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    init {
        viewModelScope.launch {
            walletRepository.activeCredentialsFlow.collect { creds ->
                if (creds != null) {
                    refresh()
                }
            }
        }
        
        // Continuous background polling like in MetaMask / Trust Wallet
        viewModelScope.launch {
            while (true) {
                delay(15000) // Poll every 15 seconds
                if (walletRepository.isWalletCreated()) {
                    refresh()
                }
            }
        }
    }
}
