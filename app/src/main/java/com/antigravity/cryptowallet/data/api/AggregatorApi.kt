package com.antigravity.cryptowallet.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface AggregatorApi {
    @GET("v1/prices")
    suspend fun getPrices(
        @Query("symbols") symbols: String? = null
    ): AggregatedPriceResponse

    @GET("v1/history")
    suspend fun getHistory(
        @Query("symbol") symbol: String,
        @Query("limit") limit: Int = 24,
        @Query("interval") interval: String = "hour"
    ): List<HistoryItem>
}

data class AggregatedPriceResponse(
    val source: String,
    val prices: Map<String, Double>
)

data class HistoryItem(
    val time: Long,
    val high: Double,
    val low: Double,
    val open: Double,
    val close: Double,
    val volumefrom: Double,
    val volumeto: Double
)
