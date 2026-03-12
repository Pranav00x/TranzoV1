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
    val explorerApiKey: String = "",
    val decimals: Int = 18
)

@Singleton
class NetworkRepository @Inject constructor() {
    private var _activeNetworkId = "eth"
    
    // Convert to MutableStateFlow so the UI can react to added custom chains
    private val _networksFlow = MutableStateFlow(listOf(
        Network("eth", "Ethereum", "https://cloudflare-eth.com", "https://cloudflare-eth.com", 1, "ETH", "ethereum", "https://api.etherscan.io/v2/api", com.antigravity.cryptowallet.BuildConfig.ETHERSCAN_API_KEY, 18),
        Network("bsc", "BNB Chain", "https://bsc-dataseed.binance.org", "https://bsc-dataseed.binance.org", 56, "BNB", "binancecoin", "https://api.etherscan.io/v2/api", com.antigravity.cryptowallet.BuildConfig.BSCSCAN_API_KEY, 18),
        Network("matic", "Polygon", "https://polygon-rpc.com", "https://polygon-rpc.com", 137, "POL", "matic-network", "https://api.etherscan.io/v2/api", com.antigravity.cryptowallet.BuildConfig.POLYGONSCAN_API_KEY, 18),
        Network("base", "Base", "https://mainnet.base.org", "https://mainnet.base.org", 8453, "ETH", "ethereum", "https://api.routescan.io/v2/network/mainnet/evm/8453/etherscan/api", com.antigravity.cryptowallet.BuildConfig.BASESCAN_API_KEY, 18),
        Network("arb", "Arbitrum One", "https://arb1.arbitrum.io/rpc", "https://arb1.arbitrum.io/rpc", 42161, "ETH", "ethereum", "https://api.etherscan.io/v2/api", com.antigravity.cryptowallet.BuildConfig.ARBISCAN_API_KEY, 18),
        Network("op", "Optimism", "https://mainnet.optimism.io", "https://mainnet.optimism.io", 10, "ETH", "ethereum", "https://api.routescan.io/v2/network/mainnet/evm/10/etherscan/api", com.antigravity.cryptowallet.BuildConfig.OPTIMISMSCAN_API_KEY, 18),
        Network("trx", "Tron", "https://api.trongrid.io", "https://api.trongrid.io", 728126428, "TRX", "tron", "https://apilist.tronscan.org/api", "", 6),
        Network("btc", "Bitcoin", "https://rpc.ankr.com/http/btc_api_version_missing", "https://rpc.ankr.com/http/btc_api_version_missing", 0, "BTC", "bitcoin", "https://mempool.space/api", "", 8)
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
