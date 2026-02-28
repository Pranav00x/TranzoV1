package com.antigravity.cryptowallet.data.blockchain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockchainService @Inject constructor() {

    private fun getChainId(networkId: String): Byte {
        return when (networkId) {
            "eth" -> 1.toByte()
            "bsc" -> 56.toByte()
            "matic" -> 137.toByte()
            "base" -> 8453.toByte()
            "arb" -> 42161.toByte()
            "op" -> 10.toByte()
            else -> 1.toByte() // fallback
        }
    }

    // Gas buffers are now dynamic based on transaction type

    // 1. Reusable OkHttpClient with increased timeouts
    private val okHttpClient: okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS) // Keep connections alive
        .build()

    // 2. Cache Web3j instances per RPC URL to avoid heavy object creation
    private val web3jCache = java.util.concurrent.ConcurrentHashMap<String, Web3j>()

    private fun getWeb3j(rpcUrl: String): Web3j {
        return web3jCache.computeIfAbsent(rpcUrl) { url ->
            // Pass the shared OkHttpClient to HttpService
            Web3j.build(HttpService(url, okHttpClient, false))
        }
    }

    suspend fun getBalance(rpcUrl: String, address: String, networkId: String = "eth"): BigInteger = withContext(Dispatchers.IO) {
        if (networkId == "btc" || networkId == "trx") {
            // These chains do not support eth_getBalance via Web3J (Return 0 for now)
            return@withContext BigInteger.ZERO
        }
    
        try {
            // Guard with timeout to prevent infinite hanging
            kotlinx.coroutines.withTimeout(15_000L) {
                val web3j = getWeb3j(rpcUrl)
                val ethGetBalance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send()
                ethGetBalance.balance
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Graceful degradation
            BigInteger.ZERO
        }
    }

    // --- L2 Fee Helpers ---

    private suspend fun fetchGasPrice(web3j: Web3j, networkId: String): BigInteger {
        val rawPrice = web3j.ethGasPrice().send().gasPrice
        return when (networkId) {
            "matic" -> {
                // Polygon: +25% buffer for priority
                BigDecimal(rawPrice).multiply(BigDecimal("1.25")).toBigInteger()
            }
            else -> rawPrice // Standard
        }
    }

    private suspend fun fetchGasLimit(
        web3j: Web3j,
        transaction: org.web3j.protocol.core.methods.request.Transaction,
        networkId: String,
        isContract: Boolean = false,
        isTokenTransfer: Boolean = false
    ): BigInteger {
        val estimated = try {
            val result = web3j.ethEstimateGas(transaction).send()
            if (result.hasError()) {
                if (isTokenTransfer || isContract) BigInteger.valueOf(100_000) else BigInteger.valueOf(21_000)
            } else {
                result.amountUsed
            }
        } catch (e: Exception) {
             if (isTokenTransfer || isContract) BigInteger.valueOf(100_000) else BigInteger.valueOf(21_000)
        }

        // Apply dynamic buffers
        return when (networkId) {
            "arb" -> {
                // Arbitrum: +100% buffer (2.0x) to cover L1 calldata costs missing from simple estimate
                BigDecimal(estimated).multiply(BigDecimal("2.0")).toBigInteger()
            }
            else -> {
                // Standard: +30% for contracts/tokens, +10% for simple ETH
                val buffer = if (isContract || isTokenTransfer) 1.30 else 1.10
                BigDecimal(estimated).multiply(BigDecimal(buffer)).toBigInteger()
            }
        }
    }

    // -----------------------

    suspend fun sendEth(rpcUrl: String, networkId: String, credentials: Credentials, toAddress: String, amountWei: BigInteger): String = withContext(Dispatchers.IO) {
        try {
            // Longer timeout for transactions
            kotlinx.coroutines.withTimeout(60_000L) {
                val web3j = getWeb3j(rpcUrl)

                // 1. Get Nonce
                val ethGetTransactionCount = web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.LATEST).send()
                val nonce = ethGetTransactionCount.transactionCount

                // 2. Get Gas Price (L2 logic)
                val gasPrice = fetchGasPrice(web3j, networkId)

                // 3. Estimate Gas (L2 logic)
                // Check if recipient is a contract
                val ethGetCode = web3j.ethGetCode(toAddress, DefaultBlockParameterName.LATEST).send()
                val isContract = ethGetCode.code != "0x"

                val estimateTransaction = org.web3j.protocol.core.methods.request.Transaction.createEtherTransaction(
                    credentials.address,
                    nonce,
                    gasPrice,
                    null,
                    toAddress,
                    amountWei
                )
                
                val gasLimit = fetchGasLimit(web3j, estimateTransaction, networkId, isContract = isContract)

                // 4. Pre-flight Balance Check (Total Cost = Gas * Price + Value)
                val totalCost = (gasLimit * gasPrice) + amountWei
                val balance = web3j.ethGetBalance(credentials.address, DefaultBlockParameterName.LATEST).send().balance
                if (balance < totalCost) {
                    throw Exception("Insufficient balance to cover transfer + gas fees. Required: $totalCost, Available: $balance")
                }

                // 5. Sign & Send (EIP-155)
                val rawTransaction = RawTransaction.createEtherTransaction(
                    nonce, gasPrice, gasLimit, toAddress, amountWei
                )

                val chainId = getChainId(networkId)
                val signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
                val hexValue = Numeric.toHexString(signedMessage)

                val ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send()
                ethSendTransaction.transactionHash ?: throw Exception(ethSendTransaction.error?.message ?: "Transaction failed without hash")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getTokenBalance(rpcUrl: String, tokenAddress: String, walletAddress: String, networkId: String = "eth"): BigInteger = withContext(Dispatchers.IO) {
        if (networkId == "btc" || networkId == "trx") return@withContext BigInteger.ZERO
        
        try {
            kotlinx.coroutines.withTimeout(15_000L) {
                val web3j = getWeb3j(rpcUrl)
                val functionCode = "0x70a08231" // balanceOf(address)
                val paddedAddress = "000000000000000000000000" + walletAddress.removePrefix("0x")
                val data = functionCode + paddedAddress
                
                val ethCall = web3j.ethCall(
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(walletAddress, tokenAddress, data),
                    DefaultBlockParameterName.LATEST
                ).send()
                
                if (ethCall.value == "0x" || ethCall.value == null) return@withTimeout BigInteger.ZERO
                
                Numeric.toBigInt(ethCall.value)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            BigInteger.ZERO
        }
    }

    suspend fun sendToken(rpcUrl: String, networkId: String, credentials: Credentials, tokenAddress: String, toAddress: String, amount: BigInteger): String = withContext(Dispatchers.IO) {
        try {
            kotlinx.coroutines.withTimeout(60_000L) {
                val web3j = getWeb3j(rpcUrl)
                
                // 1. Get Nonce
                val ethGetTransactionCount = web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.LATEST).send()
                val nonce = ethGetTransactionCount.transactionCount

                // 2. Get Gas Price (L2 logic used)
                val gasPrice = fetchGasPrice(web3j, networkId)

                // 3. Prepare Data & Estimate
                val functionCode = "0xa9059cbb" // transfer(address,uint256)
                val paddedTo = toAddress.removePrefix("0x").padStart(64, '0')
                val paddedAmount = amount.toString(16).padStart(64, '0')
                val data = functionCode + paddedTo + paddedAmount

                val estimateTransaction = org.web3j.protocol.core.methods.request.Transaction.createFunctionCallTransaction(
                    credentials.address,
                    nonce,
                    gasPrice,
                    null,
                    tokenAddress,
                    data
                )

                val gasLimit = fetchGasLimit(web3j, estimateTransaction, networkId, isTokenTransfer = true)

                 // 4. Check Gas Money (ETH Balance > Gas Limit * Gas Price)
                val gasCost = gasLimit * gasPrice
                val ethBalance = web3j.ethGetBalance(credentials.address, DefaultBlockParameterName.LATEST).send().balance
                if (ethBalance < gasCost) {
                    throw Exception("Insufficient ETH for gas fees. Required: $gasCost, Available: $ethBalance")
                }

                // 5. Sign & Send (EIP-155)
                val rawTransaction = RawTransaction.createTransaction(
                    nonce, gasPrice, gasLimit, tokenAddress, data
                )

                val chainId = getChainId(networkId)
                val signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
                val hexValue = Numeric.toHexString(signedMessage)

                val ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send()
                ethSendTransaction.transactionHash ?: throw Exception(ethSendTransaction.error?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
    
    suspend fun getTransactionReceipt(rpcUrl: String, txHash: String): org.web3j.protocol.core.methods.response.TransactionReceipt? = withContext(Dispatchers.IO) {
        try {
            val web3j = getWeb3j(rpcUrl)
            web3j.ethGetTransactionReceipt(txHash).send().transactionReceipt.orElse(null)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun cancelTransaction(rpcUrl: String, networkId: String, credentials: Credentials, originalTxHash: String): String = withContext(Dispatchers.IO) {
        try {
            kotlinx.coroutines.withTimeout(60_000L) {
                val web3j = getWeb3j(rpcUrl)

                // 1. Fetch Original Transaction
                val transaction = web3j.ethGetTransactionByHash(originalTxHash).send().transaction.orElse(null)
                    ?: throw Exception("Original transaction not found or dropped")

                if (transaction.blockNumber != null) {
                    throw Exception("Transaction already mined in block ${transaction.blockNumber}")
                }

                // 2. Prepare Cancellation (Self-transfer 0 ETH)
                val nonce = transaction.nonce
                
                // Increase Gas Price by 15% (min 1.15x)
                // Use fetched price if it's higher than replacement requirement? 
                // Standard logic: Max(Original * 1.15, CurrentNetworkPrice)
                val currentGasPrice = fetchGasPrice(web3j, networkId)
                val minReplacementPrice = BigDecimal(transaction.gasPrice).multiply(BigDecimal("1.15")).toBigInteger()
                
                val newGasPrice = if (currentGasPrice > minReplacementPrice) currentGasPrice else minReplacementPrice
                
                val gasLimit = BigInteger.valueOf(21000)

                // 3. Sign & Send (EIP-155)
                val rawTransaction = RawTransaction.createEtherTransaction(
                    nonce, newGasPrice, gasLimit, credentials.address, BigInteger.ZERO
                )

                val chainId = getChainId(networkId)
                val signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
                val hexValue = Numeric.toHexString(signedMessage)

                val ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send()
                ethSendTransaction.transactionHash ?: throw Exception(ethSendTransaction.error?.message ?: "Cancellation failed")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun speedUpTransaction(rpcUrl: String, networkId: String, credentials: Credentials, originalTxHash: String): String = withContext(Dispatchers.IO) {
        try {
            kotlinx.coroutines.withTimeout(60_000L) {
                val web3j = getWeb3j(rpcUrl)

                // 1. Fetch Original Transaction
                val transaction = web3j.ethGetTransactionByHash(originalTxHash).send().transaction.orElse(null)
                    ?: throw Exception("Original transaction not found or dropped")

                if (transaction.blockNumber != null) {
                    throw Exception("Transaction already mined in block ${transaction.blockNumber}")
                }

                // 2. Prepare Replacement
                val nonce = transaction.nonce
                
                // Increase Gas Price by 15%
                val currentGasPrice = fetchGasPrice(web3j, networkId)
                val minReplacementPrice = BigDecimal(transaction.gasPrice).multiply(BigDecimal("1.15")).toBigInteger()
                val newGasPrice = if (currentGasPrice > minReplacementPrice) currentGasPrice else minReplacementPrice

                val toAddress = transaction.to
                val value = transaction.value
                val data = transaction.input
                
                // New logic: Use fetchGasLimit to get L2 buffered limit
                val estimateTx = org.web3j.protocol.core.methods.request.Transaction.createFunctionCallTransaction(
                     transaction.from, nonce, newGasPrice, null, toAddress, value, data
                )
                
                val isTokenOrContract = data != null && data != "0x" && data.isNotEmpty()
                val gasLimit = fetchGasLimit(web3j, estimateTx, networkId, isContract = isTokenOrContract)

                // 3. Sign & Send (EIP-155)
                val rawTransaction = RawTransaction.createTransaction(
                    nonce, newGasPrice, gasLimit, toAddress, value, data
                )

                val chainId = getChainId(networkId)
                val signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
                val hexValue = Numeric.toHexString(signedMessage)

                val ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send()
                ethSendTransaction.transactionHash ?: throw Exception(ethSendTransaction.error?.message ?: "Speedup failed")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
