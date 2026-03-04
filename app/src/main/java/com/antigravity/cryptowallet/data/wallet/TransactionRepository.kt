package com.antigravity.cryptowallet.data.wallet

import com.antigravity.cryptowallet.data.api.ExplorerApi
import com.antigravity.cryptowallet.data.blockchain.BlockchainService
import com.antigravity.cryptowallet.data.blockchain.Network
import com.antigravity.cryptowallet.data.db.TransactionDao
import com.antigravity.cryptowallet.data.db.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val explorerApi: ExplorerApi,
    private val blockchainService: BlockchainService
) {
    val transactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    suspend fun refreshTransactions(address: String, network: Network, contractAddress: String? = null, action: String? = null) = withContext(Dispatchers.IO) {
        if (address.isBlank()) return@withContext
        try {
            val maxBlock = transactionDao.getMaxBlockNumber(network.name) ?: 0L
            val startBlock = if (maxBlock > 0) String.format("%d", maxBlock + 1) else "0"
            
            val response = when (network.id) {
                "btc" -> {
                    // https://mempool.space/api/address/ADDRESS/txs
                    explorerApi.getTransactionList(
                        url = "${network.explorerApiUrl}/address/$address/txs",
                        address = address // Dummy, but required by method signature
                    )
                }
                "trx" -> {
                    // https://apilist.tronscan.org/api/transaction?address=ADDRESS&limit=50
                    explorerApi.getTransactionList(
                        url = "${network.explorerApiUrl}/transaction",
                        address = address,
                        offset = 50
                    )
                }
                else -> {
                    val isRoutescan = network.id == "base" || network.id == "op"
                    val querySort = if (isRoutescan && startBlock.toInt() > 0) "asc" else "desc"
                    val queryOffset = if (isRoutescan && startBlock.toInt() > 0) 100 else 50
                    
                    explorerApi.getTransactionList(
                        url = network.explorerApiUrl,
                        action = action ?: (if (contractAddress != null) "tokentx" else "txlist"),
                        address = address,
                        contractaddress = contractAddress,
                        apikey = network.explorerApiKey.takeIf { it.isNotBlank() },
                        chainId = network.chainId,
                        offset = queryOffset,
                        sort = querySort,
                        startblock = startBlock.toInt()
                    )
                }
            }
            
            if (response.isSuccessful) {
                val body = response.body() ?: return@withContext
                val entities = mutableListOf<TransactionEntity>()
                
                when {
                    network.id == "trx" -> {
                        // TronScan parsing
                        val obj = body.asJsonObject
                        val dataArray = obj.getAsJsonArray("data")
                        dataArray?.forEach { element ->
                            val tx = element.asJsonObject
                            val value = tx.get("amount")?.asString ?: "0"
                            val timestamp = tx.get("timestamp")?.asLong ?: System.currentTimeMillis()
                            entities.add(TransactionEntity(
                                hash = tx.get("hash").asString,
                                fromAddress = tx.get("ownerAddress").asString,
                                toAddress = tx.get("toAddress").asString,
                                value = BigDecimal(value).divide(BigDecimal.TEN.pow(network.decimals)).toPlainString(),
                                symbol = network.symbol,
                                timestamp = timestamp,
                                type = if (tx.get("ownerAddress").asString.lowercase() == address.lowercase()) "send" else "receive",
                                status = if (tx.get("confirmed")?.asBoolean == true) "success" else "pending",
                                network = network.name,
                                blockNumber = tx.get("block")?.asLong ?: 0L
                            ))
                        }
                    }
                    network.id == "btc" -> {
                        // Mempool.space is an array of transactions
                        if (body.isJsonArray) {
                            val array = body.asJsonArray
                            array.forEach { element ->
                                val tx = element.asJsonObject
                                val vin = tx.getAsJsonArray("vin")
                                val vout = tx.getAsJsonArray("vout")
                                
                                val isSend = vin?.any { it.asJsonObject.get("prevout")?.isJsonObject == true && it.asJsonObject.get("prevout")?.asJsonObject?.get("scriptpubkey_address")?.asString == address } ?: false
                                val valueSats = if (isSend) {
                                    vout.firstOrNull()?.asJsonObject?.get("value")?.asLong ?: 0L
                                } else {
                                    vout.find { it.asJsonObject.get("scriptpubkey_address")?.asString == address }?.asJsonObject?.get("value")?.asLong ?: 0L
                                }
                                
                                val status = tx.get("status")?.asJsonObject
                                val timestamp = status?.get("block_time")?.asLong?.let { it * 1000 } ?: System.currentTimeMillis()
                                val blockHeight = status?.get("block_height")?.asLong ?: 0L
                                
                                entities.add(TransactionEntity(
                                    hash = tx.get("txid")?.asString ?: "",
                                    fromAddress = if (isSend) address else "Multiple",
                                    toAddress = if (!isSend) address else "Multiple",
                                    value = BigDecimal(valueSats).divide(BigDecimal.TEN.pow(8)).toPlainString(),
                                    symbol = "BTC",
                                    timestamp = timestamp,
                                    type = if (isSend) "send" else "receive",
                                    status = "success",
                                    network = "Bitcoin",
                                    blockNumber = blockHeight
                                ))
                            }
                        }
                    }
                    else -> {
                        // EVM / Etherscan style (Object with "result" array)
                        val obj = body.asJsonObject
                        if (obj.get("status")?.asString == "1") {
                            val resultArray = obj.getAsJsonArray("result")
                            resultArray?.forEach { element ->
                                if (element.isJsonObject) {
                                    val tx = element.asJsonObject
                                    val value = tx.get("value")?.asString ?: "0"
                                    val timestamp = (tx.get("timeStamp")?.asLong ?: 0L) * 1000L
                                    val tokenSymbol = tx.get("tokenSymbol")?.asString ?: network.symbol
                                    val tokenDecimal = tx.get("tokenDecimal")?.asInt ?: network.decimals
                                    entities.add(TransactionEntity(
                                        hash = tx.get("hash")?.asString ?: "",
                                        fromAddress = tx.get("from")?.asString ?: "",
                                        toAddress = tx.get("to")?.asString ?: "",
                                        value = BigDecimal(value).divide(BigDecimal.TEN.pow(tokenDecimal)).toPlainString(),
                                        symbol = tokenSymbol,
                                        timestamp = timestamp,
                                        type = if (tx.get("from")?.asString?.lowercase() == address.lowercase()) "send" else "receive",
                                        status = "success",
                                        network = network.name,
                                        blockNumber = tx.get("blockNumber")?.asLong ?: 0L
                                    ))
                                }
                            }
                        }
                    }
                }
                
                if (entities.isNotEmpty()) {
                    transactionDao.insertTransactions(entities)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun checkPendingTransactions(rpcUrl: String) = withContext(Dispatchers.IO) {
        val pendingTransactions = transactionDao.getPendingTransactions()
        for (tx in pendingTransactions) {
            try {
                val receipt = blockchainService.getTransactionReceipt(rpcUrl, tx.hash)
                if (receipt != null) {
                    val status = if (receipt.isStatusOK) "success" else "failed"
                    val blockNumber = receipt.blockNumber?.toLong() ?: 0L
                    transactionDao.updateBlockNumberAndStatus(tx.hash, blockNumber, status)
                } else {
                    // Check for timeout or dropped tx logic could go here
                    val age = System.currentTimeMillis() - tx.timestamp
                    if (age > 3600_000) { // 1 hour
                         // Optional: mark failed or dropped
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun addTransaction(
        hash: String,
        from: String,
        to: String,
        value: String,
        symbol: String,
        type: String,
        status: String,
        network: String,
        blockNumber: Long = 0
    ) {
        val transaction = TransactionEntity(
            hash = hash,
            fromAddress = from,
            toAddress = to,
            value = value,
            symbol = symbol,
            timestamp = System.currentTimeMillis(),
            type = type,
            status = status,
            network = network,
            blockNumber = blockNumber
        )
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateStatus(hash: String, status: String) {
        transactionDao.updateStatus(hash, status)
    }
}
