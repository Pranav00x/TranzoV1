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
                    "TRX" -> "trx"
                    "BTC" -> "btc"
                    "ARB" -> "arb"
                    "OP" -> "op"
                    "BASE" -> "base"
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
                "TRX" -> "tron"
                "ARB" -> "arbitrum"
                "OP" -> "optimism"
                "BASE" -> "base"
                "SHIB" -> "shiba-inu"
                "PEPE" -> "pepe"
                "DAI" -> "dai"
                "UNI" -> "uniswap"
                "AAVE" -> "aave"
                "LDO" -> "lido-dao"
                "WETH" -> "weth"
                "BUSD" -> "binance-usd"
                else -> "" // Don't fallback to ethereum for custom tokens
            }

            // Immediately show cached description
            if (tokenEntity?.description != null) {
                description = tokenEntity.description
            }

            // Execute fetching sequentially with delays to avoid CoinGecko 429 Rate Limits
            try {
                // 1. Fast Price Fetch
                if (id.isNotEmpty()) {
                    val priceMap = coinRepository.getSimplePrice(id)
                    val simplePrice = priceMap[id]?.get("usd") ?: 0.0
                    if (simplePrice > 0) {
                         price = String.format("$%.2f", simplePrice)
                    }
                } else {
                    // Custom token price from our aggregator?
                    val simplePrice = coinRepository.getPrices(listOf(symbol))[symbol] ?: 0.0
                    if (simplePrice > 0) {
                        price = String.format("$%.2f", simplePrice)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            kotlinx.coroutines.delay(600) // Rate limit bumper

            try {
                // 2. Fetch Info
                if (id.isNotEmpty()) {
                    val info = coinRepository.getCoinInfo(id)
                    val rawDescription = info.description.en
                    val formattedDescription = rawDescription.replace(Regex("<.*?>"), "") 
                        .take(300) + (if (rawDescription.length > 300) "..." else "")
                    
                    description = formattedDescription
                    // Save to DB
                    tokenEntity?.let { tokenDao.updateDescription(it.id, formattedDescription) }
                    
                    val rawAddr = info.platforms?.entries?.firstOrNull()?.value
                    contractAddress = if (!rawAddr.isNullOrEmpty()) rawAddr else "Native Token"
                } else {
                    contractAddress = if (symbol.uppercase() in listOf("ETH", "BNB", "TRX", "BTC", "ARB", "OP", "BASE")) "Native Token" else "Custom Token"
                    if (description == "Loading..." || description.startsWith("Failed")) {
                         description = "Advanced $symbol token on ${currentNetId.uppercase()} network."
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (description == "Loading...") {
                    description = when(symbol.uppercase()) {
                        "ETH" -> "Ethereum is a decentralized, open-source blockchain with smart contract functionality. Ether (ETH) is the native cryptocurrency of the platform."
                        "BNB" -> "BNB is the native cryptocurrency of the Binance ecosystem and powers the Binance Smart Chain."
                        "MATIC", "POL" -> "Polygon is a protocol and a framework for building and connecting Ethereum-compatible blockchain networks."
                        "TRX" -> "TRON is a blockchain-based decentralized operating system much like Ethereum that aims to advance the decentralization of the Internet and its infrastructure."
                        "BTC" -> "Bitcoin is a decentralized cryptocurrency originally described in a 2008 whitepaper by a person, or group of people, using the alias Satoshi Nakamoto."
                        "ARB" -> "Arbitrum is a suite of Ethereum scaling solutions that enable high-throughput, low cost smart contracts while remaining trustlessly secure."
                        "OP" -> "Optimism is a low-cost and lightning-fast Ethereum L2 blockchain."
                        "BASE" -> "Base is a secure, low-cost, builder-friendly Ethereum L2 built to bring the next billion users onchain."
                        else -> "Failed to load detailed info for $symbol."
                    }
                }
                contractAddress = if (symbol.uppercase() in listOf("ETH", "BNB", "TRX", "BTC", "ARB", "OP", "BASE")) "Native Token" else ""
            }
            
            kotlinx.coroutines.delay(600) // Rate limit bumper

            try {
                // 3. Fetch Chart, OHLC from our Backend (CryptoCompare)
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
                    
                    if (price == "Loading..." || price == "Error") {
                        price = String.format("$%.2f", history.last().close)
                    }
                } else {
                     throw Exception("Empty History")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
