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
    var selectedTimeframe by mutableStateOf("30") // default to 30 days
        private set

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
        if (currentSymbol.isNotEmpty()) {
            loadTokenData(currentSymbol)
        }
    }

    fun loadTokenData(symbol: String) {
        currentSymbol = symbol
        
        // 1. Observe transactions locally filtered by symbol
        viewModelScope.launch {
            transactionRepository.transactions.collect { allTxs ->
                transactions = allTxs.filter { it.symbol.equals(symbol, ignoreCase = true) }
            }
        }

        // 2. Fetch Balance and Refresh Transactions
        viewModelScope.launch {
            try {
                val tokenEntity = tokenDao.getTokenBySymbol(symbol)
                val netId = tokenEntity?.chainId ?: when(symbol.uppercase()) {
                    "BNB" -> "bsc"
                    "MATIC", "POL" -> "matic"
                    "ETH" -> "eth"
                    else -> "eth"
                }
                val network = networkRepository.getNetwork(netId)
                currentNetId = network.id
                
                // Trigger Transaction Refresh
                val targetAddress = walletRepository.getAddress(network.id)
                try {
                    transactionRepository.refreshTransactions(targetAddress, network)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Get Balance
                val rawBalance = if (tokenEntity?.contractAddress != null) {
                    blockchainService.getTokenBalance(network.rpcUrl, tokenEntity.contractAddress, targetAddress, network.id)
                } else {
                    blockchainService.getBalance(network.rpcUrl, targetAddress, network.id)
                }
                
                val decimals = tokenEntity?.decimals ?: when(network.id) {
                    "btc" -> 8
                    "trx" -> 6
                    else -> 18
                }
                val ethBalance = BigDecimal(rawBalance).divide(BigDecimal.TEN.pow(decimals), 4, BigDecimal.ROUND_HALF_UP)
                balance = String.format("%.4f %s", ethBalance, symbol)
            } catch (e: Exception) {
                e.printStackTrace()
                balance = "0.0000 $symbol"
            }
        }

        // 3. Fetch Coin Info and Price with separate Error Handling
        viewModelScope.launch {
            val tokenEntity = tokenDao.getTokenBySymbol(symbol)
            val id = tokenEntity?.coingeckoId ?: when(symbol.uppercase()) {
                "ETH" -> "ethereum"
                "BNB" -> "binancecoin"
                "BTC" -> "bitcoin"
                "USDT" -> "tether"
                "USDC" -> "usd-coin"
                "LINK" -> "chainlink"
                "CAKE" -> "pancakeswap-token"
                "MATIC", "POL" -> "matic-network"
                "SOL" -> "solana"
                else -> "ethereum"
            }

            // Execute fetching sequentially with delays to avoid CoinGecko 429 Rate Limits
            try {
                // 1. Fast Price Fetch
                val priceMap = coinRepository.getSimplePrice(id)
                val simplePrice = priceMap[id]?.get("usd") ?: 0.0
                if (simplePrice > 0) {
                     price = String.format("$%.2f", simplePrice)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            kotlinx.coroutines.delay(600) // Rate limit bumper

            try {
                // 2. Fetch Info
                val info = coinRepository.getCoinInfo(id)
                val rawDescription = info.description.en
                description = rawDescription.replace(Regex("<.*?>"), "") 
                    .take(300) + (if (rawDescription.length > 300) "..." else "")
                
                val rawAddr = info.platforms?.entries?.firstOrNull()?.value
                contractAddress = if (!rawAddr.isNullOrEmpty()) rawAddr else "Native Token"
            } catch (e: Exception) {
                e.printStackTrace()
                description = when(symbol.uppercase()) {
                    "ETH" -> "Ethereum is a decentralized, open-source blockchain with smart contract functionality. Ether (ETH) is the native cryptocurrency of the platform."
                    "BNB" -> "BNB is the native cryptocurrency of the Binance ecosystem and powers the Binance Smart Chain."
                    "MATIC", "POL" -> "Polygon is a protocol and a framework for building and connecting Ethereum-compatible blockchain networks."
                    else -> "Failed to load detailed info for $symbol."
                }
                contractAddress = if (symbol.uppercase() == "ETH" || symbol.uppercase() == "BNB") "Native Token" else ""
            }
            
            kotlinx.coroutines.delay(600) // Rate limit bumper

            try {
                // 3. Fetch Chart, OHLC
                val apiTimeframe = when(selectedTimeframe) {
                    "1D" -> "1"
                    "7D" -> "7"
                    "1M" -> "30"
                    "1Y" -> "365"
                    "ALL" -> "max"
                    else -> "30"
                }
                ohlcData = coinRepository.getOHLC(id, apiTimeframe)
                
                if (ohlcData.isNotEmpty()) {
                    val lastCandle = ohlcData.last()
                    graphPoints = ohlcData.map { it[4] } // Use close prices
                     if (price == "Loading..." || price == "Error") {
                         price = String.format("$%.2f", lastCandle[4])
                    }
                } else {
                     throw Exception("Empty OHLC")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to regular chart
                try {
                    val apiTimeframe = when(selectedTimeframe) {
                        "1D" -> "1"
                        "7D" -> "7"
                        "1M" -> "30"
                        "1Y" -> "365"
                        "ALL" -> "max"
                        else -> "30"
                    }
                    val chart = coinRepository.getMarketChart(id, apiTimeframe)
                    graphPoints = chart.prices.map { it[1] }
                    if (graphPoints.isNotEmpty()) {
                        val currentPrice = graphPoints.lastOrNull() ?: 0.0
                        if (price == "Loading..." || price == "Error") {
                            price = String.format("$%.2f", currentPrice)
                        }
                    }
                } catch (e2: Exception) {
                     if (price == "Loading...") {
                         price = "Error"
                     }
                }
                
                if (ohlcData.isEmpty()) {
                    ohlcData = List(30) { i ->
                        val base = if (price.replace("$","").replace(",","").toDoubleOrNull() != null) price.replace("$","").replace(",","").toDouble() else 100.0
                        listOf(i.toDouble(), base, base + 5, base - 5, base + 2)
                    }
                }
            }
        }
    }
}
