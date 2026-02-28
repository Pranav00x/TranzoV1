package com.antigravity.cryptowallet.ui.wallet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.cryptowallet.data.repository.CoinRepository
import com.antigravity.cryptowallet.data.wallet.TransactionRepository
import com.antigravity.cryptowallet.data.wallet.WalletRepository
import com.antigravity.cryptowallet.data.db.TokenDao
import com.antigravity.cryptowallet.data.blockchain.BlockchainService
import com.antigravity.cryptowallet.data.blockchain.NetworkRepository
import com.antigravity.cryptowallet.data.db.TransactionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.math.BigDecimal

@HiltViewModel
class TokenDetailViewModel @Inject constructor(
    private val coinRepository: CoinRepository,
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository,
    private val tokenDao: TokenDao,
    private val blockchainService: BlockchainService,
    private val networkRepository: NetworkRepository
) : ViewModel() {
    private var loadJob: kotlinx.coroutines.Job? = null

    var balance by mutableStateOf("0.0")
        private set

    var description by mutableStateOf("Loading...")
        private set
    
    var price by mutableStateOf("Loading...")
        private set

    var contractAddress by mutableStateOf("")
        private set

    var graphPoints by mutableStateOf<List<Double>>(emptyList())
        private set
    
    var ohlcData by mutableStateOf<List<List<Double>>>(emptyList())
        private set

    // Graph timeframe support
    var selectedTimeframe by mutableStateOf("1D") 
        private set

    private var currentAssetId: String = ""

    var transactions by mutableStateOf<List<TransactionEntity>>(emptyList())
        private set

    private var currentSymbol: String = ""

    val address: String
        get() = walletRepository.getAddress(currentNetId)
    
    val walletAddress: String
        get() = walletRepository.getAddress(currentNetId)

    private var currentNetId: String = "eth"

    fun setTimeframeAndReload(days: String) {
        selectedTimeframe = days
        if (currentAssetId.isNotEmpty()) {
            loadTokenData(currentAssetId)
        }
    }

    var symbol by mutableStateOf("")
        private set

    var networkName by mutableStateOf("")
        private set

    fun loadTokenData(assetId: String) {
        currentAssetId = assetId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val isNative = assetId.startsWith("native-")
                val tokenEntity = if (!isNative) {
                    val tokenId = assetId.removePrefix("token-").toLongOrNull()
                    tokenId?.let { tokenDao.getTokenById(it) }
                } else null
                
                val netId = if (isNative) {
                    assetId.removePrefix("native-")
                } else {
                    tokenEntity?.chainId ?: "eth"
                }

                val network = networkRepository.getNetwork(netId)
                currentNetId = network.id
                symbol = if (isNative) network.symbol else (tokenEntity?.symbol ?: "")
                currentSymbol = symbol
                networkName = network.name

                // Trigger Transaction Refresh and Balance Fetch
                val targetAddress = walletRepository.getAddress(network.id)
                
                // 1. Transactions
                launch {
                    transactionRepository.transactions.collect { allTxs ->
                        transactions = allTxs.filter { it.symbol.equals(symbol, ignoreCase = true) && it.network == network.name }
                    }
                }
                
                launch {
                    try {
                        transactionRepository.refreshTransactions(targetAddress, network, tokenEntity?.contractAddress)
                    } catch (e: Exception) { e.printStackTrace() }
                }
                
                // 2. Balance
                launch {
                    try {
                        val rawBalance = if (tokenEntity?.contractAddress != null) {
                            blockchainService.getTokenBalance(network.rpcUrl, tokenEntity.contractAddress, targetAddress, network.id)
                        } else {
                            blockchainService.getBalance(network.rpcUrl, targetAddress, network.id)
                        }
                        
                        val decimals = tokenEntity?.decimals ?: network.decimals
                        val ethBalance = BigDecimal(rawBalance).divide(BigDecimal.TEN.pow(decimals))
                        balance = if (ethBalance.compareTo(BigDecimal.ZERO) == 0) {
                            "0.00 $symbol"
                        } else if (ethBalance < BigDecimal("0.01")) {
                            val formatted = ethBalance.stripTrailingZeros().toPlainString()
                            if (formatted.length > 10) String.format("%.8f %s", ethBalance, symbol) else "$formatted $symbol"
                        } else {
                            String.format("%.4f %s", ethBalance, symbol)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }

                // 3. Price & Info
                val cgId = tokenEntity?.coingeckoId ?: when(symbol.uppercase()) {
                    "ETH" -> "ethereum"
                    "BNB" -> "binancecoin"
                    "BTC" -> "bitcoin"
                    "MATIC", "POL" -> "matic-network"
                    "ARB" -> "arbitrum"
                    "OP" -> "optimism"
                    "BASE" -> "base"
                    "TRX" -> "tron"
                    "SOL" -> "solana"
                    else -> ""
                }

                if (cgId.isNotEmpty()) {
                    launch {
                        try {
                            val priceMap = coinRepository.getSimplePrice(cgId)
                            val simplePrice = priceMap[cgId]?.get("usd") ?: 0.0
                            if (simplePrice > 0) price = String.format("$%.2f", simplePrice)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                } else {
                    launch {
                        try {
                            val p = coinRepository.getPrices(listOf(symbol))[symbol] ?: 0.0
                            if (p > 0) price = String.format("$%.2f", p)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }

                // 4. Graph (Wait for identity match)
                try {
                    var intervalResult = "hour"
                    val limit = when(selectedTimeframe.uppercase()) {
                        "1D" -> 24
                        "7D" -> 168
                        "1M" -> 720
                        "1Y" -> {
                            intervalResult = "day"
                            365
                        }
                        "ALL" -> {
                            intervalResult = "day"
                            2000
                        }
                        else -> 24
                    }
                    
                    val history = coinRepository.getHistory(currentSymbol, limit, intervalResult)
                    if (history.isNotEmpty()) {
                        ohlcData = history.map { listOf(it.time.toDouble(), it.open, it.high, it.low, it.close) }
                        graphPoints = history.map { it.close }
                        if (price == "Loading...") {
                            price = String.format("$%.2f", history.last().close)
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
