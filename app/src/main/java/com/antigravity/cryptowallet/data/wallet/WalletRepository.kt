package com.antigravity.cryptowallet.data.wallet

import com.antigravity.cryptowallet.data.models.WalletInfo
import com.antigravity.cryptowallet.data.models.WalletType
import com.antigravity.cryptowallet.data.security.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Credentials
import org.web3j.crypto.MnemonicUtils
import org.web3j.utils.Numeric
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepository @Inject constructor(
    private val secureStorage: SecureStorage
) {
    init {
        // Initialization is now handled asynchronously via loadWallet()
    }

    // In-memory cache of credentials (cleared on lock)
    var activeCredentials: Credentials? = null
        private set

    private val _wallets = MutableStateFlow<List<WalletInfo>>(emptyList())
    val wallets = _wallets.asStateFlow()

    private val _activeWallet = MutableStateFlow<WalletInfo?>(null)
    val activeWallet = _activeWallet.asStateFlow()

    fun refreshWallets() {
        _wallets.value = secureStorage.getWallets()
        _activeWallet.value = secureStorage.getActiveWallet()
    }

    suspend fun createWallet(name: String = "Wallet"): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val initialEntropy = ByteArray(16)
        SecureRandom().nextBytes(initialEntropy)
        val mnemonic = MnemonicUtils.generateMnemonic(initialEntropy)
        
        // Derive address from mnemonic
        val seed = MnemonicUtils.generateSeed(mnemonic, null)
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
        val path = intArrayOf(44 or Bip32ECKeyPair.HARDENED_BIT, 60 or Bip32ECKeyPair.HARDENED_BIT, 0 or Bip32ECKeyPair.HARDENED_BIT, 0, 0)
        val derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)
        val credentials = Credentials.create(derivedKeyPair)
        
        // Generate wallet name if not provided
        val walletCount = secureStorage.getWallets().size
        val walletName = if (name.isBlank()) "Wallet ${walletCount + 1}" else name
        
        // Add wallet to storage
        val walletId = secureStorage.addWallet(walletName, credentials.address, WalletType.MNEMONIC)
        secureStorage.saveMnemonicForWallet(walletId, mnemonic)
        
        // Also save as legacy for backward compatibility
        secureStorage.saveMnemonic(mnemonic)
        
        // Load the new wallet
        activeCredentials = credentials
        refreshWallets()
        
        mnemonic
    }

    suspend fun importWallet(mnemonic: String, name: String = "Imported Wallet"): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            if (!MnemonicUtils.validateMnemonic(mnemonic)) return@withContext false
            
            val seed = MnemonicUtils.generateSeed(mnemonic, null)
            val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
            val path = intArrayOf(44 or Bip32ECKeyPair.HARDENED_BIT, 60 or Bip32ECKeyPair.HARDENED_BIT, 0 or Bip32ECKeyPair.HARDENED_BIT, 0, 0)
            val derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)
            val credentials = Credentials.create(derivedKeyPair)
            
            val walletCount = secureStorage.getWallets().size
            val walletName = if (name.isBlank()) "Imported ${walletCount + 1}" else name
            
            val walletId = secureStorage.addWallet(walletName, credentials.address, WalletType.MNEMONIC)
            secureStorage.saveMnemonicForWallet(walletId, mnemonic)
            secureStorage.saveMnemonic(mnemonic)
            
            activeCredentials = credentials
            refreshWallets()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importPrivateKey(privateKey: String, name: String = "Imported Wallet"): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val cleanKey = if (privateKey.startsWith("0x")) privateKey.substring(2) else privateKey
            if (cleanKey.length != 64) return@withContext false
            
            val credentials = Credentials.create(cleanKey)
            
            val walletCount = secureStorage.getWallets().size
            val walletName = if (name.isBlank()) "Imported ${walletCount + 1}" else name
            
            val walletId = secureStorage.addWallet(walletName, credentials.address, WalletType.PRIVATE_KEY)
            secureStorage.savePrivateKeyForWallet(walletId, cleanKey)
            secureStorage.savePrivateKey(cleanKey)
            
            activeCredentials = credentials
            refreshWallets()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun switchWallet(walletId: String) {
        secureStorage.setActiveWallet(walletId)
        val wallet = secureStorage.getWallets().find { it.id == walletId } ?: return
        loadWalletCredentials(wallet)
        refreshWallets()
    }

    suspend fun deleteWallet(walletId: String) {
        secureStorage.deleteWallet(walletId)
        refreshWallets()
        
        // If we deleted the active wallet, load a new one
        val newActive = secureStorage.getActiveWallet()
        if (newActive != null) {
            loadWalletCredentials(newActive)
        } else {
            activeCredentials = null
        }
    }

    fun renameWallet(walletId: String, newName: String) {
        secureStorage.renameWallet(walletId, newName)
        refreshWallets()
    }

    private suspend fun loadWalletCredentials(wallet: WalletInfo) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        activeCredentials = when (wallet.type) {
            WalletType.MNEMONIC -> {
                val mnemonic = secureStorage.getMnemonicForWallet(wallet.id)
                if (mnemonic != null) {
                    val seed = MnemonicUtils.generateSeed(mnemonic, null)
                    val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
                    val path = intArrayOf(44 or Bip32ECKeyPair.HARDENED_BIT, 60 or Bip32ECKeyPair.HARDENED_BIT, 0 or Bip32ECKeyPair.HARDENED_BIT, 0, 0)
                    val derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)
                    Credentials.create(derivedKeyPair)
                } else null
            }
            WalletType.PRIVATE_KEY -> {
                val key = secureStorage.getPrivateKeyForWallet(wallet.id)
                if (key != null) Credentials.create(key) else null
            }
        }
    }

    suspend fun loadWallet(mnemonic: String? = null, privateKey: String? = null) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            // Check for multi-wallet first
            val activeWallet = secureStorage.getActiveWallet()
            if (activeWallet != null) {
                loadWalletCredentials(activeWallet)
                refreshWallets()
                return@withContext
            }
            
            // Legacy single wallet support
            val seedMnemonic = mnemonic ?: secureStorage.getMnemonic()
            val storedPrivateKey = privateKey ?: secureStorage.getPrivateKey()

            activeCredentials = when {
                seedMnemonic != null -> {
                    val seed = MnemonicUtils.generateSeed(seedMnemonic, null)
                    val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
                    val path = intArrayOf(44 or Bip32ECKeyPair.HARDENED_BIT, 60 or Bip32ECKeyPair.HARDENED_BIT, 0 or Bip32ECKeyPair.HARDENED_BIT, 0, 0)
                    val derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)
                    Credentials.create(derivedKeyPair)
                }
                storedPrivateKey != null -> {
                    Credentials.create(storedPrivateKey)
                }
                else -> null
            }
            
            refreshWallets()
        } catch (e: Exception) {
            e.printStackTrace()
            activeCredentials = null
        }
    }

    fun hasMnemonic(): Boolean {
        val activeWallet = secureStorage.getActiveWallet()
        return if (activeWallet != null) {
            activeWallet.type == WalletType.MNEMONIC
        } else {
            secureStorage.getMnemonic() != null
        }
    }

    fun getMnemonicForActiveWallet(): String? {
        val activeWallet = secureStorage.getActiveWallet()
        return if (activeWallet != null) {
            secureStorage.getMnemonicForWallet(activeWallet.id)
        } else {
            secureStorage.getMnemonic()
        }
    }

    fun getAddress(networkId: String = "eth"): String {
        return when (networkId) {
            "btc" -> {
                val mnemonic = getMnemonicForActiveWallet()
                if (mnemonic != null) CryptoUtils.getBtcAddress(mnemonic) else ""
            }
            "trx" -> {
                val mnemonic = getMnemonicForActiveWallet()
                if (mnemonic != null) {
                    CryptoUtils.getTrxAddress(mnemonic)
                } else {
                    CryptoUtils.getTrxAddressFromEth(activeCredentials?.address ?: "")
                }
            }
            else -> activeCredentials?.address ?: ""
        }
    }
    
    fun getPrivateKey(): String {
        // Only for viewing, highly sensitive
        return activeCredentials?.ecKeyPair?.privateKey?.toString(16) ?: ""
    }

    fun isWalletCreated(): Boolean = secureStorage.hasWallet()
}
