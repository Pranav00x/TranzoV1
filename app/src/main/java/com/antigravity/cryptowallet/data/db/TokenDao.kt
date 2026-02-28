package com.antigravity.cryptowallet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TokenDao {
    @Query("SELECT * FROM tokens")
    fun getAllTokens(): Flow<List<TokenEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToken(token: TokenEntity)

    @Query("SELECT * FROM tokens WHERE id = :id")
    suspend fun getTokenById(id: Long): TokenEntity?

    @Query("SELECT * FROM tokens WHERE symbol = :symbol LIMIT 1")
    suspend fun getTokenBySymbol(symbol: String): TokenEntity?

    @Query("UPDATE tokens SET lastBalance = :balance, lastBalanceUsd = :balanceUsd WHERE id = :id")
    suspend fun updateBalances(id: Int, balance: String, balanceUsd: String)

    @Query("UPDATE tokens SET description = :description WHERE id = :id")
    suspend fun updateDescription(id: Int, description: String)
}
