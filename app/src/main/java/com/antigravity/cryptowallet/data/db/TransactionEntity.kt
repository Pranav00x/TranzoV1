package com.antigravity.cryptowallet.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [androidx.room.Index(value = ["hash"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hash: String, // Transaction hash is a unique identifier (indexed)
    val fromAddress: String,
    val toAddress: String,
    val value: String, // Stored as string to handle BigIntegers/Decimals safely
    val symbol: String,
    val timestamp: Long,
    val type: String, // "send", "receive", "swap"
    val status: String, // "pending", "success", "failed"
    val network: String // "Ethereum", "Arbitrum One", etc.
)
