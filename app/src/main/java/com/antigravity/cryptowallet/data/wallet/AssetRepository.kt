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

        // 1. Load from DB first (immediate UI)
        val initialTokens = tokenDao.getAllTokens().first()
        val initialNetworks = networkRepository.networks
        
        val dbState = initialNetworks.map { net ->
            val token = initialTokens.find { it.symbol == net.symbol && it.chainId == net.id && !it.isCustom }
            AssetUiModel(
                id = "native-${net.id}",
                symbol = net.symbol,
                name = net.name,
                balance = token?.lastBalance ?: "0.00 ${net.symbol}",
                balanceUsd = token?.lastBalanceUsd ?: "---",
                iconUrl = getNativeIcon(net.id, net.symbol),
                networkName = net.name,
                rawBalance = 0.0,
                price = 0.0
            )
        } + initialTokens.filter { it.isCustom || (it.contractAddress != null) }.map { token ->
            AssetUiModel(
                id = "token-${token.id}",
                symbol = token.symbol,
                name = token.name,
                balance = token.lastBalance ?: "0.00 ${token.symbol}",
                balanceUsd = token.lastBalanceUsd ?: "---",
                iconUrl = "https://static.coinpaprika.com/coin/${token.symbol.lowercase()}-${token.name.lowercase().replace(" ","-")}/logo.png",
                networkName = token.chainId.uppercase(),
                rawBalance = 0.0,
                price = 0.0
            )
        }

        if (_assets.value.isEmpty() || _assets.value.size != dbState.size) {
            _assets.value = dbState
        }

        // 2. Fetch Aggregated Prices from our Backend
        val allTokens = tokenDao.getAllTokens().first()
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
                    val imageUrl = getNativeIcon(net.id, net.symbol)

                    val balanceStr = formatBalance(ethBalance, net.symbol)
                    val balanceUsdStr = String.format("$%.2f", balanceUsd)

                    // Cache to DB (native tokens are also in tokens table)
                    val existingToken = tokenDao.getTokenBySymbol(net.symbol)
                    if (existingToken != null && existingToken.chainId == net.id) {
                         tokenDao.updateBalances(existingToken.id, balanceStr, balanceUsdStr)
                    }

                    AssetUiModel(
                        id = "native-${net.id}",
                        symbol = net.symbol,
                        name = net.name,
                        balance = balanceStr,
                        balanceUsd = balanceUsdStr,
                        iconUrl = imageUrl,
                        networkName = net.name,
                        rawBalance = ethBalance.toDouble(),
                        price = price
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }

        // 4. Fetch Token Balances in Parallel
        val tokenAssetsDeferred = allTokens.filter { it.contractAddress != null }.map { token ->
            async {
                try {
                    val net = networkRepository.getNetwork(token.chainId)
                    val address = walletRepository.getAddress(net.id)
                    val balance = blockchainService.getTokenBalance(net.rpcUrl, token.contractAddress!!, address, net.id)
                    val tokenBalance = BigDecimal(balance).divide(BigDecimal.TEN.pow(token.decimals))
                    
                    val price = priceMap[token.symbol] ?: 0.0
                    val balanceUsd = tokenBalance.multiply(BigDecimal(price))
                    val imageUrl = "https://static.coinpaprika.com/coin/${token.symbol.lowercase()}-${token.name.lowercase().replace(" ","-")}/logo.png"

                    val balanceStr = formatBalance(tokenBalance, token.symbol)
                    val balanceUsdStr = String.format("$%.2f", balanceUsd)

                    // Cache to DB
                    tokenDao.updateBalances(token.id, balanceStr, balanceUsdStr)

                    AssetUiModel(
                        id = "token-${token.id}",
                        symbol = token.symbol,
                        name = token.name,
                        balance = balanceStr,
                        balanceUsd = balanceUsdStr,
                        iconUrl = imageUrl,
                        networkName = net.name,
                        rawBalance = tokenBalance.toDouble(),
                        price = price
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }

        // 5. Update Pending Transactions
        networkRepository.networks.forEach { net ->
             try {
                 transactionRepository.checkPendingTransactions(net.rpcUrl)
             } catch (e: Exception) { e.printStackTrace() }
        }

        // Wait for all
        val nativeAssets = nativeAssetsDeferred.awaitAll().filterNotNull()
        val tokenAssets = tokenAssetsDeferred.awaitAll().filterNotNull()
        
        _assets.value = nativeAssets + tokenAssets
    }

    private fun getNativeIcon(id: String, symbol: String): String {
        return when(id) {
            "base" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/base/info/logo.png"
            "arb" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/arbitrum/info/logo.png"
            "op" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/optimism/info/logo.png"
            "bsc" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/smartchain/info/logo.png"
            "matic" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/polygon/info/logo.png"
            "trx" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/tron/info/logo.png"
            "eth" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/ethereum/info/logo.png"
            "btc" -> "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/bitcoin/info/logo.png"
            else -> "https://assets.coincap.io/assets/icons/${symbol.lowercase()}@2x.png"
        }
    }

    private fun formatBalance(balance: BigDecimal, symbol: String): String {
        return if (balance.compareTo(BigDecimal.ZERO) == 0) {
            "0.00 $symbol"
        } else if (balance < BigDecimal("0.0001")) {
            String.format("%.6f %s", balance, symbol)
        } else {
            String.format("%.4f %s", balance, symbol)
        }
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
