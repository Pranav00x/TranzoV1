package com.antigravity.cryptowallet.data.blockchain

import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Int256
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint80
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChainlinkService @Inject constructor() {

    /**
     * Fetch the latest price from a Chainlink Price Feed contract.
     * ABI: function latestRoundData() external view returns (uint80 roundId, int256 answer, uint256 startedAt, uint256 updatedAt, uint80 answeredInRound)
     */
    suspend fun getLatestPrice(rpcUrl: String, feedAddress: String): BigDecimal? {
        return try {
            val web3j = Web3j.build(HttpService(rpcUrl))
            
            val function = Function(
                "latestRoundData",
                emptyList(),
                listOf(
                    object : TypeReference<Uint80>() {},
                    object : TypeReference<Int256>() {},
                    object : TypeReference<Uint256>() {},
                    object : TypeReference<Uint256>() {},
                    object : TypeReference<Uint80>() {}
                )
            )

            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, feedAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).sendAsync().get()

            val results = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            if (results.isNotEmpty()) {
                val answer = (results[1] as Int256).value
                val decimals = getDecimals(web3j, feedAddress)
                BigDecimal(answer).divide(BigDecimal.TEN.pow(decimals))
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getDecimals(web3j: Web3j, feedAddress: String): Int {
        return try {
            val function = Function(
                "decimals",
                emptyList(),
                listOf(object : TypeReference<org.web3j.abi.datatypes.generated.Uint8>() {})
            )
            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, feedAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).sendAsync().get()
            
            val results = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            if (results.isNotEmpty()) {
                (results[0].value as BigInteger).toInt()
            } else 8 // Chainlink default
        } catch (e: Exception) {
            8
        }
    }
}
