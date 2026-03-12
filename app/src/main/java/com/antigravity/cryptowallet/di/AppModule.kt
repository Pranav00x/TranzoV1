package com.antigravity.cryptowallet.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.room.Room
import com.antigravity.cryptowallet.data.api.CoinGeckoApi
import com.antigravity.cryptowallet.data.api.ExplorerApi
import com.antigravity.cryptowallet.data.db.AppDatabase
import com.antigravity.cryptowallet.data.db.TokenDao
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.antigravity.cryptowallet.BuildConfig

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "secure_wallet_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "wallet_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideTokenDao(database: AppDatabase): TokenDao {
        return database.tokenDao()
    }

    @Provides
    fun provideTransactionDao(database: AppDatabase): com.antigravity.cryptowallet.data.db.TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideCoinGeckoApi(): CoinGeckoApi {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE)
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CoinGeckoApi::class.java)
    }

    @Provides
    @Singleton
    fun provideExplorerApi(): ExplorerApi {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE)
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.etherscan.io/") // Default to Etherscan; in production this would be dynamic per chain
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExplorerApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAggregatorApi(): com.antigravity.cryptowallet.data.api.AggregatorApi {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE)
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://price-feed-backend.vercel.app/") // Production Vercel URL
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.antigravity.cryptowallet.data.api.AggregatorApi::class.java)
    }
}
