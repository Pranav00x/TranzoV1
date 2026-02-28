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
    val explorerApiUrl: String,
    val explorerApiKey: String = ""
)

@Singleton
class NetworkRepository @Inject constructor() {
    private var _activeNetworkId = "eth"
    
    // Convert to MutableStateFlow so the UI can react to added custom chains
    private val _networksFlow = MutableStateFlow(listOf(
        Network("eth", "Ethereum", "https://mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", "https://mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", 1, "ETH", "ethereum", "https://api.etherscan.io/v2/api", com.antigravity.cryptowallet.BuildConfig.ETHERSCAN_API_KEY),
        Network("bsc", "BNB Chain", "https://bsc-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", "https://bsc-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", 56, "BNB", "binancecoin", "https://api.bscscan.com/v2/api", com.antigravity.cryptowallet.BuildConfig.BSCSCAN_API_KEY),
        Network("matic", "Polygon", "https://polygon-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", "https://polygon-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", 137, "POL", "matic-network", "https://api.polygonscan.com/v2/api", com.antigravity.cryptowallet.BuildConfig.POLYGONSCAN_API_KEY),
        Network("base", "Base", "https://base-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", "https://base-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", 8453, "ETH", "ethereum", "https://api.basescan.org/v2/api", com.antigravity.cryptowallet.BuildConfig.BASESCAN_API_KEY),
        Network("arb", "Arbitrum One", "https://arbitrum-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", "https://arbitrum-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", 42161, "ETH", "ethereum", "https://api.arbiscan.io/v2/api", com.antigravity.cryptowallet.BuildConfig.ARBISCAN_API_KEY),
        Network("op", "Optimism", "https://optimism-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", "https://optimism-mainnet.infura.io/v3/2e73eb0da821430d818d929e16963fc3", 10, "ETH", "ethereum", "https://api-optimistic.etherscan.io/v2/api", com.antigravity.cryptowallet.BuildConfig.OPTIMISMSCAN_API_KEY),
        Network("trx", "Tron", "https://api.trongrid.io", "https://api.trongrid.io", 728126428, "TRX", "tron", "https://apilist.tronscan.org/api", ""),
        Network("btc", "Bitcoin", "https://rpc.ankr.com/http/btc_api_version_missing", "https://rpc.ankr.com/http/btc_api_version_missing", 0, "BTC", "bitcoin", "https://mempool.space/api", "")
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
