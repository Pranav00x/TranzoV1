package com.antigravity.cryptowallet.data.wallet

import com.antigravity.cryptowallet.data.api.CoinGeckoApi
import com.antigravity.cryptowallet.data.blockchain.BlockchainService
import com.antigravity.cryptowallet.data.blockchain.NetworkRepository
import com.antigravity.cryptowallet.data.db.TokenDao
import com.antigravity.cryptowallet.data.db.TokenEntity
import com.antigravity.cryptowallet.data.models.AssetUiModel
import com.antigravity.cryptowallet.data.models.CoinMarketItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetRepository @Inject constructor(
    private val walletRepository: WalletRepository,
    private val networkRepository: NetworkRepository,
    private val blockchainService: BlockchainService,
    private val tokenDao: TokenDao,
    private val coinRepository: com.antigravity.cryptowallet.data.repository.CoinRepository,
    private val transactionRepository: TransactionRepository
) {
    private val _assets = kotlinx.coroutines.flow.MutableStateFlow<List<AssetUiModel>>(emptyList())
    val assets: kotlinx.coroutines.flow.StateFlow<List<AssetUiModel>> = _assets.asStateFlow()

    suspend fun refreshAssets() = withContext(Dispatchers.IO) {
        if (!walletRepository.isWalletCreated()) return@withContext

        // 1. Ensure Defaults
        val savedTokens = tokenDao.getAllTokens().first()
        if (savedTokens.isEmpty()) {
             val defaults = listOf(
                 TokenEntity(symbol = "USDT", name = "Tether", contractAddress = "0xdac17f958d2ee523a2206206994597c13d831ec7", decimals = 6, chainId = "eth", coingeckoId = "tether"),
                 TokenEntity(symbol = "USDC", name = "USD Coin", contractAddress = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", decimals = 6, chainId = "eth", coingeckoId = "usd-coin"),
                 TokenEntity(symbol = "SHIB", name = "Shiba Inu", contractAddress = "0x95ad61b0a150d79219dcf64e1e6cc01f0b64c4ce", decimals = 18, chainId = "eth", coingeckoId = "shiba-inu"),
                 TokenEntity(symbol = "LINK", name = "Chainlink", contractAddress = "0x514910771af9ca656af840dff83e8264ecf986ca", decimals = 18, chainId = "eth", coingeckoId = "chainlink"),
                 TokenEntity(symbol = "PEPE", name = "Pepe", contractAddress = "0x6982508145454ce325ddbe47a25d4ec3d2311933", decimals = 18, chainId = "eth", coingeckoId = "pepe"),
                 TokenEntity(symbol = "DAI", name = "Dai", contractAddress = "0x6b175474e89094c44da98b954eedeac495271d0f", decimals = 18, chainId = "eth", coingeckoId = "dai"),
                 TokenEntity(symbol = "UNI", name = "Uniswap", contractAddress = "0x1f9840a85d5af5bf1d1762f925bdaddc4201f984", decimals = 18, chainId = "eth", coingeckoId = "uniswap"),
                 TokenEntity(symbol = "AAVE", name = "Aave", contractAddress = "0x7fc66500c84a76ad7e9c93437bfc5ac33e2ddae9", decimals = 18, chainId = "eth", coingeckoId = "aave"),
                 TokenEntity(symbol = "LDO", name = "Lido DAO", contractAddress = "0x5a98fcbea516cf06857215779fd812ca3bef1b32", decimals = 18, chainId = "eth", coingeckoId = "lido-dao"),
                 TokenEntity(symbol = "WETH", name = "WETH", contractAddress = "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2", decimals = 18, chainId = "eth", coingeckoId = "weth"),
                 TokenEntity(symbol = "WBTC", name = "Wrapped BTC", contractAddress = "0x2260fac5e5542a773aa44fbcfedf7c193bc2c599", decimals = 8, chainId = "eth", coingeckoId = "wrapped-bitcoin"),
                 TokenEntity(symbol = "USDT", name = "Tether", contractAddress = "0x55d398326f99059ff775485246999027b3197955", decimals = 18, chainId = "bsc", coingeckoId = "tether"),
                 TokenEntity(symbol = "USDC", name = "USD Coin", contractAddress = "0x8ac76a51cc950d9822d68b83fe1ad97b32cd580d", decimals = 18, chainId = "bsc", coingeckoId = "usd-coin"),
                 TokenEntity(symbol = "CAKE", name = "PancakeSwap", contractAddress = "0x0e09fabb73bd3ade0a17ecc321fd13a19e81ce82", decimals = 18, chainId = "bsc", coingeckoId = "pancakeswap-token"),
                 TokenEntity(symbol = "BUSD", name = "Binance USD", contractAddress = "0xe9e7cea3dedca5984780bafc599bd69add087d56", decimals = 18, chainId = "bsc", coingeckoId = "binance-usd"),
                 TokenEntity(symbol = "TWT", name = "Trust Wallet Token", contractAddress = "0x4b0f1812e5df2a09796481ff14017e6005508003", decimals = 18, chainId = "bsc", coingeckoId = "trust-wallet-token"),
                 TokenEntity(symbol = "USDT", name = "Tether", contractAddress = "0xc2132d05d31c914a87c6611c10748aeb04b58e8f", decimals = 6, chainId = "matic", coingeckoId = "tether"),
                 TokenEntity(symbol = "USDC", name = "USD Coin", contractAddress = "0x2791bca1f2de4661ed88a30c99a7a9449aa84174", decimals = 6, chainId = "matic", coingeckoId = "usd-coin"),
                 TokenEntity(symbol = "ARB", name = "Arbitrum", contractAddress = "0x912ce59144191c1204e64559fe8253a0e49e6548", decimals = 18, chainId = "arb", coingeckoId = "arbitrum"),
                 TokenEntity(symbol = "OP", name = "Optimism", contractAddress = "0x4200000000000000000000000000000000000042", decimals = 18, chainId = "op", coingeckoId = "optimism")
             )
             defaults.forEach { tokenDao.insertToken(it) }
        }
        val allTokens = tokenDao.getAllTokens().first()

        // --- Fast UI Launch Placeholder Load ---
        if (_assets.value.isEmpty()) {
            val placeholders = networkRepository.networks.map { net ->
                AssetUiModel(
                    id = "native-${net.id}",
                    symbol = net.symbol,
                    name = net.name,
                    balance = "0.00 ${net.symbol}",
                    balanceUsd = "Loading",
                    iconUrl = when(net.id) {
                        "base" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/base/info/logo.png"
                        "arb" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/arbitrum/info/logo.png"
                        "op" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/optimism/info/logo.png"
                        "bsc" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/smartchain/info/logo.png"
                        "matic" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/polygon/info/logo.png"
                        "trx" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/tron/info/logo.png"
                        "eth" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/ethereum/info/logo.png"
                        "btc" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/bitcoin/info/logo.png"
                        else -> "https://assets.coincap.io/assets/icons/${net.symbol.lowercase()}@2x.png"
                    },
                    networkName = net.name,
                    rawBalance = 0.0,
                    price = 0.0
                )
            }
            _assets.value = placeholders
        }

        // 2. Fetch Aggregated Prices from our Backend
        val currentSymbols = (networkRepository.networks.map { it.symbol } + allTokens.map { it.symbol }).distinct()
        val priceMap = coinRepository.getPrices(currentSymbols)

        // 3. Fetch Native Balances in Parallel
        val nativeAssetsDeferred = networkRepository.networks.map { net ->
            async {
                try {
                    val address = walletRepository.getAddress(net.id)
                    val balance = blockchainService.getBalance(net.rpcUrl, address, net.id)
                    
                    val decimals = when (net.id) {
                        "btc" -> 8
                        "trx" -> 6
                        else -> 18
                    }
                    val ethBalance = BigDecimal(balance).divide(BigDecimal.TEN.pow(decimals))
                    
                    val price = priceMap[net.symbol] ?: 0.0
                    val balanceUsd = ethBalance.multiply(BigDecimal(price))
                    val imageUrl = when(net.id) {
                        "base" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/base/info/logo.png"
                        "arb" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/arbitrum/info/logo.png"
                        "op" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/optimism/info/logo.png"
                        "bsc" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/smartchain/info/logo.png"
                        "matic" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/polygon/info/logo.png"
                        "trx" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/tron/info/logo.png"
                        "eth" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/ethereum/info/logo.png"
                        "btc" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/bitcoin/info/logo.png"
                        else -> "https://assets.coincap.io/assets/icons/${net.symbol.lowercase()}@2x.png"
                    }

                    val balanceStr = if (ethBalance.compareTo(BigDecimal.ZERO) == 0) {
                        "0.00 ${net.symbol}"
                    } else if (ethBalance < BigDecimal("0.0001")) {
                        String.format("%.6f %s", ethBalance, net.symbol)
                    } else {
                        String.format("%.4f %s", ethBalance, net.symbol)
                    }

                    AssetUiModel(
                        id = "native-${net.id}",
                        symbol = net.symbol,
                        name = net.name,
                        balance = balanceStr,
                        balanceUsd = String.format("$%.2f", balanceUsd),
                        iconUrl = imageUrl,
                        networkName = net.name,
                        rawBalance = ethBalance.toDouble(),
                        price = price
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }

        // 4. Fetch Token Balances in Parallel
        val tokenAssetsDeferred = allTokens.map { token ->
            async {
                try {
                    val net = networkRepository.getNetwork(token.chainId)
                    if (token.contractAddress != null) {
                        val address = walletRepository.getAddress(net.id)
                        val balance = blockchainService.getTokenBalance(net.rpcUrl, token.contractAddress, address, net.id)
                        val tokenBalance = BigDecimal(balance).divide(BigDecimal.TEN.pow(token.decimals))
                        
                        val price = priceMap[token.symbol] ?: 0.0
                        val balanceUsd = tokenBalance.multiply(BigDecimal(price))
                        val imageUrl = "https://static.coinpaprika.com/coin/${token.symbol.lowercase()}-${token.name.lowercase().replace(" ","-")}/logo.png"
    
                        val balanceStr = if (tokenBalance.compareTo(BigDecimal.ZERO) == 0) {
                            "0.00 ${token.symbol}"
                        } else if (tokenBalance < BigDecimal("0.0001")) {
                            String.format("%.6f %s", tokenBalance, token.symbol)
                        } else {
                            String.format("%.4f %s", tokenBalance, token.symbol)
                        }
    
                        AssetUiModel(
                            id = "token-${token.id}",
                            symbol = token.symbol,
                            name = token.name,
                            balance = balanceStr,
                            balanceUsd = String.format("$%.2f", balanceUsd),
                            iconUrl = imageUrl,
                            networkName = net.name,
                            rawBalance = tokenBalance.toDouble(),
                            price = price
                        )
                    } else null
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            }


        // 5. Update Pending Transactions
        networkRepository.networks.forEach { net ->
             try {
                 transactionRepository.checkPendingTransactions(net.rpcUrl)
             } catch (e: Exception) {
                 e.printStackTrace()
             }
        }

        // Wait for all
        val nativeAssets = nativeAssetsDeferred.awaitAll().filterNotNull()
        val tokenAssets = tokenAssetsDeferred.awaitAll().filterNotNull()
        
        _assets.value = nativeAssets + tokenAssets
    }

    suspend fun addToken(address: String, symbol: String, decimals: Int, chainId: String, name: String) {
        tokenDao.insertToken(
            TokenEntity(
                symbol = symbol,
                name = name,
                contractAddress = address,
                decimals = decimals,
                chainId = chainId,
                isCustom = true
            )
        )
        refreshAssets()
    }

    suspend fun sendAsset(asset: AssetUiModel, toAddress: String, amount: String): String = withContext(Dispatchers.IO) {
        val credentials = walletRepository.activeCredentials ?: throw Exception("Wallet not loaded")
        val amountValue = BigDecimal(amount)
        
        // Find network
        val netId = if (asset.id.startsWith("native-")) asset.id.removePrefix("native-") else {
             // For tokens, we need to find which chain they belong to. 
             // In this simple app, we can extract from metadata or assume.
             // Let's look up the token.
             val tokenId = asset.id.removePrefix("token-").toLongOrNull()
             val token = tokenId?.let { tokenDao.getTokenById(it) }
             token?.chainId ?: "eth"
        }
        val net = networkRepository.getNetwork(netId)
        
        val txHash = if (asset.id.startsWith("native-")) {
            val amountWei = amountValue.multiply(BigDecimal.TEN.pow(18)).toBigInteger()
            blockchainService.sendEth(net.rpcUrl, net.id, credentials, toAddress, amountWei)
        } else {
            // Token
            val tokenId = asset.id.removePrefix("token-").toLongOrNull()
            val token = tokenId?.let { tokenDao.getTokenById(it) } ?: throw Exception("Token info not found")
            val amountRaw = amountValue.multiply(BigDecimal.TEN.pow(token.decimals)).toBigInteger()
            blockchainService.sendToken(net.rpcUrl, net.id, credentials, token.contractAddress!!, toAddress, amountRaw)
        }
        
        // Add to history
        transactionRepository.addTransaction(
            hash = txHash,
            from = credentials.address,
            to = toAddress,
            value = amount,
            symbol = asset.symbol,
            type = "send",
            status = "pending",
            network = net.name
        )
        
        // Trigger generic check (best effort)
        try {
            transactionRepository.checkPendingTransactions(net.rpcUrl)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        refreshAssets()
        txHash
    }

    suspend fun cancelTransaction(originalTxHash: String, chainId: String): String = withContext(Dispatchers.IO) {
        val credentials = walletRepository.activeCredentials ?: throw Exception("Wallet not loaded")
        val net = networkRepository.getNetwork(chainId)
        
        val newHash = blockchainService.cancelTransaction(net.rpcUrl, net.id, credentials, originalTxHash)
        
        // Mark old as dropped/replaced (best effort)
        try {
             transactionRepository.checkPendingTransactions(net.rpcUrl)
             transactionRepository.updateStatus(originalTxHash, "replaced")
        } catch (e: Exception) { e.printStackTrace() }

        // Add new cancellation tx
        transactionRepository.addTransaction(
            hash = newHash,
            from = credentials.address,
            to = credentials.address, 
            value = "0",
            symbol = net.symbol,
            type = "cancel",
            status = "pending",
            network = net.name
        )
         refreshAssets()
         newHash
    }

    suspend fun speedUpTransaction(originalTxHash: String, chainId: String): String = withContext(Dispatchers.IO) {
        val credentials = walletRepository.activeCredentials ?: throw Exception("Wallet not loaded")
        val net = networkRepository.getNetwork(chainId)

        val newHash = blockchainService.speedUpTransaction(net.rpcUrl, net.id, credentials, originalTxHash)

        try {
            transactionRepository.updateStatus(originalTxHash, "replaced")
        } catch (e: Exception) { e.printStackTrace() }
        
        transactionRepository.addTransaction(
            hash = newHash,
            from = credentials.address,
            to = "", 
            value = "0", 
            symbol = net.symbol,
            type = "speed_up",
            status = "pending",
            network = net.name
        )
        refreshAssets()
        newHash
    }
}
