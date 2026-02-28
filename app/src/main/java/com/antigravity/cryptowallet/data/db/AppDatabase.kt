package com.antigravity.cryptowallet.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.antigravity.cryptowallet.data.db.TokenDao
import com.antigravity.cryptowallet.data.db.TokenEntity

@Database(entities = [TokenEntity::class, TransactionEntity::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tokenDao(): TokenDao
    abstract fun transactionDao(): TransactionDao
}
