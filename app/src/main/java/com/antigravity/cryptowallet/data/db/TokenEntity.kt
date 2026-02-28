package com.antigravity.cryptowallet.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tokens")
data class TokenEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val name: String,
    val contractAddress: String?, // Null for native (ETH)
    val decimals: Int,
    val chainId: String, // eth, matic, etc.
    val coingeckoId: String? = null,
    val logoUrl: String? = null,
    val isCustom: Boolean = false,
    val description: String? = null,
    val lastBalance: String? = null,
    val lastBalanceUsd: String? = null
)
