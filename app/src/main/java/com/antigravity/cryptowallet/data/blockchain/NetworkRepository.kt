package com.antigravity.cryptowallet.data.blockchain

import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Network(
    val id: String,
    val name: String,
    val rpcUrl: String,
    val initialRpc: String,
    val chainId: Long,
    val symbol: String,
    val coingeckoId: String,
    val explorerApiUrl: String
)

@Singleton
class NetworkRepository @Inject constructor() {
    private var _activeNetworkId = "eth"
    
    // Convert to MutableStateFlow so the UI can react to added custom chains
    private val _networksFlow = MutableStateFlow(listOf(
        Network("eth", "Ethereum", "https://mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", "https://mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", 1, "ETH", "ethereum", "https://api.etherscan.io/api"),
        Network("bsc", "BNB Chain", "https://bsc-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", "https://bsc-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", 56, "BNB", "binancecoin", "https://api.bscscan.com/api"),
        Network("matic", "Polygon", "https://polygon-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", "https://polygon-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", 137, "POL", "matic-network", "https://api.polygonscan.com/api"),
        Network("base", "Base", "https://base-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", "https://base-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", 8453, "ETH", "ethereum", "https://api.basescan.org/api"),
        Network("arb", "Arbitrum One", "https://arbitrum-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", "https://arbitrum-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", 42161, "ETH", "ethereum", "https://api.arbiscan.io/api"),
        Network("op", "Optimism", "https://optimism-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", "https://optimism-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", 10, "ETH", "ethereum", "https://api-optimistic.etherscan.io/api")
    ))
    
    val networksFlow: StateFlow<List<Network>> = _networksFlow.asStateFlow()
    
    val networks: List<Network>
        get() = _networksFlow.value
    
    val activeNetwork: Network
        get() = getNetwork(_activeNetworkId)

    fun getNetwork(id: String) = networks.find { it.id == id } ?: networks.first()
    
    fun setActiveNetwork(id: String) {
        _activeNetworkId = id
    }
    
    fun addNetwork(network: Network) {
        _networksFlow.update { current ->
            if (current.any { it.id == network.id || it.chainId == network.chainId }) {
                current 
            } else {
                current + network
            }
        }
    }
}
