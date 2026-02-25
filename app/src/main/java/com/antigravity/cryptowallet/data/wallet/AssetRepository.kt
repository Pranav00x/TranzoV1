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
    private val coinGeckoApi: CoinGeckoApi,
    private val transactionRepository: TransactionRepository
) {
    private val _assets = kotlinx.coroutines.flow.MutableStateFlow<List<AssetUiModel>>(emptyList())
    val assets: kotlinx.coroutines.flow.StateFlow<List<AssetUiModel>> = _assets.asStateFlow()

    suspend fun refreshAssets() = withContext(Dispatchers.IO) {
        if (!walletRepository.isWalletCreated()) return@withContext
        val address = walletRepository.getAddress()

        // 1. Ensure Defaults
        val savedTokens = tokenDao.getAllTokens().first()
        if (savedTokens.isEmpty()) {
             val defaults = listOf(
                 TokenEntity(symbol = "USDT", name = "Tether", contractAddress = "0xdac17f958d2ee523a2206206994597c13d831ec7", decimals = 6, chainId = "eth", coingeckoId = "tether"),
                 TokenEntity(symbol = "USDC", name = "USD Coin", contractAddress = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", decimals = 6, chainId = "eth", coingeckoId = "usd-coin"),
                 TokenEntity(symbol = "USDT", name = "Tether", contractAddress = "0x55d398326f99059ff775485246999027b3197955", decimals = 18, chainId = "bsc", coingeckoId = "tether"),
                 TokenEntity(symbol = "USDC", name = "USD Coin", contractAddress = "0x8ac76a51cc950d9822d68b83fe1ad97b32cd580d", decimals = 18, chainId = "bsc", coingeckoId = "usd-coin"),
                 TokenEntity(symbol = "USDT", name = "Tether", contractAddress = "0xc2132d05d31c914a87c6611c10748aeb04b58e8f", decimals = 6, chainId = "matic", coingeckoId = "tether"),
                 TokenEntity(symbol = "USDC", name = "USD Coin", contractAddress = "0x2791bca1f2de4661ed88a30c99a7a9449aa84174", decimals = 6, chainId = "matic", coingeckoId = "usd-coin")
             )
             defaults.forEach { tokenDao.insertToken(it) }
        }
        val allTokens = tokenDao.getAllTokens().first()

        // 2. Prepare list of CoinGecko IDs
        val networkIds = networkRepository.networks.map { it.coingeckoId }
        val tokenIds = allTokens.mapNotNull { it.coingeckoId }
        val allIds = (networkIds + tokenIds).distinct().joinToString(",")
        
        val marketMap = try {
            val markets = coinGeckoApi.getCoinsMarkets(ids = allIds)
            markets.associateBy { it.id }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }

        // 3. Fetch Native Balances in Parallel
        val nativeAssetsDeferred = networkRepository.networks.map { net ->
            async {
                try {
                    val balance = blockchainService.getBalance(net.rpcUrl, address)
                    val ethBalance = BigDecimal(balance).divide(BigDecimal.TEN.pow(18))
                    
                    val marketData = marketMap[net.coingeckoId]
                    val price = marketData?.currentPrice ?: 0.0
                    val balanceUsd = ethBalance.multiply(BigDecimal(price))
                    val imageUrl = marketData?.image

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
                        val balance = blockchainService.getTokenBalance(net.rpcUrl, token.contractAddress, address)
                        val tokenBalance = BigDecimal(balance).divide(BigDecimal.TEN.pow(token.decimals))
                        
                        val marketData = token.coingeckoId?.let { marketMap[it] }
                        val price = marketData?.currentPrice ?: 0.0
                        val balanceUsd = tokenBalance.multiply(BigDecimal(price))
                        val imageUrl = marketData?.image
    
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
