package com.antigravity.cryptowallet.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface ExplorerApi {
    @GET
    suspend fun getTransactionList(
        @retrofit2.http.Url url: String,
        @Query("module") module: String = "account",
        @Query("action") action: String = "txlist",
        @Query("address") address: String,
        @Query("startblock") startblock: Int = 0,
        @Query("endblock") endblock: Int = 99999999,
        @Query("page") page: Int = 1,
        @Query("offset") offset: Int = 20,
        @Query("sort") sort: String = "desc",
        @Query("apikey") apikey: String? = null,
        @Query("chainid") chainId: Long? = null
    ): ExplorerResponse
}

data class ExplorerResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("result") val result: List<ExplorerTransaction>
)

data class ExplorerTransaction(
    @SerializedName("hash") val hash: String,
    @SerializedName("from") val from: String,
    @SerializedName("to") val to: String,
    @SerializedName("value") val value: String,
    @SerializedName("timeStamp") val timeStamp: String,
    @SerializedName("isError") val isError: String,
    @SerializedName("txreceipt_status") val txReceiptStatus: String?
)
