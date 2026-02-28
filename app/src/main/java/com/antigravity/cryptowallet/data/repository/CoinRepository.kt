package com.antigravity.cryptowallet.data.repository

import com.antigravity.cryptowallet.data.api.CoinGeckoApi
import com.antigravity.cryptowallet.data.models.MarketChartResponse
import com.antigravity.cryptowallet.data.models.CoinInfoResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinRepository @Inject constructor(
    private val api: CoinGeckoApi,
    private val aggregatorApi: com.antigravity.cryptowallet.data.api.AggregatorApi
) {
    suspend fun getMarketChart(id: String, days: String = "7"): MarketChartResponse {
        return api.getCoinMarketChart(id, days = days)
    }
    
    suspend fun getOHLC(id: String, days: String = "365"): List<List<Double>> {
        return api.getCoinOHLC(id, days = days)
    }

    suspend fun getSimplePrice(id: String): Map<String, Map<String, Double>> {
        return try {
            // Mapping for Aggregator (it expects symbols like BTC, ETH)
            val symbol = when(id) {
                "bitcoin" -> "BTC"
                "ethereum" -> "ETH"
                "binancecoin" -> "BNB"
                "matic-network" -> "MATIC"
                "tron" -> "TRX"
                "arbitrum" -> "ARB"
                "optimism" -> "OP"
                "base" -> "BASE"
                else -> null
            }
            
            if (symbol != null) {
                val response = aggregatorApi.getPrices(symbol)
                val price = response.prices[symbol] ?: 0.0
                mapOf(id to mapOf("usd" to price))
            } else {
                api.getSimplePrice(id)
            }
        } catch (e: Exception) {
            api.getSimplePrice(id)
        }
    }

    suspend fun getCoinInfo(id: String): CoinInfoResponse {
        return api.getCoinInfo(id)
    }
    
    suspend fun getPrices(symbols: List<String>): Map<String, Double> {
        return try {
            val response = aggregatorApi.getPrices(symbols.joinToString(","))
            response.prices
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun getHistory(symbol: String, limit: Int, interval: String = "hour"): List<com.antigravity.cryptowallet.data.api.HistoryItem> {
        return try {
            aggregatorApi.getHistory(symbol, limit, interval)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
