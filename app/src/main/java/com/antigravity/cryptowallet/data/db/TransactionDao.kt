package com.antigravity.cryptowallet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = 'pending'")
    suspend fun getPendingTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET status = :status WHERE hash = :hash")
    suspend fun updateStatus(hash: String, status: String)

    @Query("UPDATE transactions SET blockNumber = :blockNumber, status = :status WHERE hash = :hash")
    suspend fun updateBlockNumberAndStatus(hash: String, blockNumber: Long, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("SELECT MAX(blockNumber) FROM transactions WHERE network = :network")
    suspend fun getMaxBlockNumber(network: String): Long?
}
