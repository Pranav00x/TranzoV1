package com.antigravity.cryptowallet.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.cryptowallet.data.blockchain.NetworkRepository
import com.antigravity.cryptowallet.data.wallet.TransactionRepository
import com.antigravity.cryptowallet.data.wallet.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
