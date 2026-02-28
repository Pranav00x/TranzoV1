package com.antigravity.cryptowallet.data.wallet

import com.antigravity.cryptowallet.data.api.ExplorerApi
import com.antigravity.cryptowallet.data.blockchain.BlockchainService
import com.antigravity.cryptowallet.data.blockchain.Network
import com.antigravity.cryptowallet.data.db.TransactionDao
import com.antigravity.cryptowallet.data.db.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
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

    suspend fun refreshTransactions(address: String, network: Network) = withContext(Dispatchers.IO) {
        try {
            val response = explorerApi.getTransactionList(
                url = network.explorerApiUrl,
                address = address,
                apikey = network.explorerApiKey.takeIf { it.isNotBlank() },
                chainId = if (network.id != "trx" && network.id != "btc") network.chainId else null
            )
            if (response.status == "1") {
                val entities = response.result.map { tx ->
                    val valueEth = BigDecimal(tx.value).divide(BigDecimal.TEN.pow(18)).toPlainString()
                    val type = if (tx.from.lowercase() == address.lowercase()) "send" else "receive"
                    TransactionEntity(
                        hash = tx.hash,
                        fromAddress = tx.from,
                        toAddress = tx.to,
                        value = valueEth,
                        symbol = network.symbol,
                        timestamp = tx.timeStamp.toLong() * 1000,
                        type = type,
                        status = "success",
                        network = network.name
                    )
                }
                transactionDao.insertTransactions(entities)
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
                    transactionDao.updateStatus(tx.hash, status)
                } else {
                    // Check for timeout or dropped tx logic could go here
                    // specific for re-orgs: if tx is very old (e.g. > 1 hour) and no receipt, track as failed?
                    // For now, keep pending.
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
        network: String
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
            network = network
        )
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateStatus(hash: String, status: String) {
        transactionDao.updateStatus(hash, status)
    }
}
