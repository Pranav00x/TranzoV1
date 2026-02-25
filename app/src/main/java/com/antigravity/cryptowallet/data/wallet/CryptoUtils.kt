package com.antigravity.cryptowallet.data.wallet

import org.bitcoinj.core.ECKey
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.params.MainNetParams
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.MnemonicUtils
import java.security.MessageDigest

object CryptoUtils {

    fun getBtcAddress(mnemonic: String): String {
        try {
            val seed = MnemonicUtils.generateSeed(mnemonic, null)
            val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
            // BTC path: m/44'/0'/0'/0/0
            val path = intArrayOf(
                44 or Bip32ECKeyPair.HARDENED_BIT, 
                0 or Bip32ECKeyPair.HARDENED_BIT, 
                0 or Bip32ECKeyPair.HARDENED_BIT, 
                0, 
                0
            )
            val derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)
            
            // To ECKey
            val ecKey = ECKey.fromPrivate(derivedKeyPair.privateKeyBytes33)
            return LegacyAddress.fromKey(MainNetParams.get(), ecKey).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun getTrxAddressFromEth(ethAddress: String): String {
        try {
            val cleanEth = ethAddress.removePrefix("0x")
            if (cleanEth.length != 40) return ""
            
            val ethBytes = org.web3j.utils.Numeric.hexStringToByteArray(cleanEth)
            
            val trxBytes = ByteArray(21)
            trxBytes[0] = 0x41.toByte()
            System.arraycopy(ethBytes, 0, trxBytes, 1, 20)
            
            val sha256 = MessageDigest.getInstance("SHA-256")
            val hash1 = sha256.digest(trxBytes)
            val hash2 = sha256.digest(hash1)
            
            val checkSum = ByteArray(4)
            System.arraycopy(hash2, 0, checkSum, 0, 4)
            
            val finalBytes = ByteArray(25)
            System.arraycopy(trxBytes, 0, finalBytes, 0, 21)
            System.arraycopy(checkSum, 0, finalBytes, 21, 4)
            
            return org.bitcoinj.core.Base58.encode(finalBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun getTrxAddress(mnemonic: String): String {
        try {
            val seed = MnemonicUtils.generateSeed(mnemonic, null)
            val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
            // TRX path: m/44'/195'/0'/0/0
            val path = intArrayOf(
                44 or Bip32ECKeyPair.HARDENED_BIT, 
                195 or Bip32ECKeyPair.HARDENED_BIT, 
                0 or Bip32ECKeyPair.HARDENED_BIT, 
                0, 
                0
            )
            val derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)
            val credentials = org.web3j.crypto.Credentials.create(derivedKeyPair)
            return getTrxAddressFromEth(credentials.address)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}
