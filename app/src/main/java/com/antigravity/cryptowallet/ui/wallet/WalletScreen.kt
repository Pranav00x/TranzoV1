package com.antigravity.cryptowallet.ui.wallet

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.antigravity.cryptowallet.ui.components.*
import com.antigravity.cryptowallet.ui.theme.PrimaryVariant
import com.antigravity.cryptowallet.ui.theme.SecondaryVariant
import com.antigravity.cryptowallet.utils.QrCodeGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.cryptowallet.data.blockchain.NetworkRepository
import com.antigravity.cryptowallet.data.wallet.AssetRepository
import com.antigravity.cryptowallet.data.wallet.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val assetRepository: AssetRepository,
    private val networkRepository: NetworkRepository
) : ViewModel() {
    val address: String get() = walletRepository.getAddress()
    val networks = networkRepository.networks
    var activeNetwork by mutableStateOf(networkRepository.activeNetwork)
        private set

    var totalBalanceUsd by mutableStateOf("$0.00")
    var assets by mutableStateOf<List<com.antigravity.cryptowallet.data.models.AssetUiModel>>(emptyList())
    var isRefreshing by mutableStateOf(false)
    var selectedTab by mutableStateOf(0)

    private var allAssets = listOf<com.antigravity.cryptowallet.data.models.AssetUiModel>()

    init {
        loadData()
    }

    fun switchNetwork(networkId: String) {
        networkRepository.setActiveNetwork(networkId)
        activeNetwork = networkRepository.activeNetwork
        updateDisplayedAssets()
        refresh()
    }
    
    fun addToken(address: String, symbol: String, decimals: Int) {
        viewModelScope.launch {
            assetRepository.addToken(address, symbol, decimals, activeNetwork.id, symbol)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing = true
            assetRepository.refreshAssets()
            isRefreshing = false
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            if (!walletRepository.isWalletCreated()) return@launch
            assetRepository.assets.collect { assetList ->
                allAssets = assetList
                updateDisplayedAssets()
            }
        }
        refresh()
    }
    
    private fun updateDisplayedAssets() {
        assets = allAssets
        val total = allAssets.sumOf { it.rawBalance * it.price }
        totalBalanceUsd = String.format("$%.2f", total)
    }
    
    suspend fun sendAsset(asset: com.antigravity.cryptowallet.data.models.AssetUiModel, toAddress: String, amount: String): String? {
        return try {
            assetRepository.sendAsset(asset, toAddress, amount)
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun WalletScreen(
    viewModel: WalletViewModel = hiltViewModel(),
    onSetupSecurity: () -> Unit = {},
    onNavigateToSend: () -> Unit = {},
    onNavigateToTokenDetail: (String) -> Unit = {}
) {
    var showReceiveDialog by remember { mutableStateOf(false) }
    var showAddTokenDialog by remember { mutableStateOf(false) }
    var showNetworkSelector by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.clickable { showNetworkSelector = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = viewModel.activeNetwork.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Connected • ${viewModel.address.take(6)}...${viewModel.address.takeLast(4)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row {
                IconButton(
                    onClick = { viewModel.refresh() }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onSetupSecurity
                ) {
                    Icon(Icons.Default.Shield, contentDescription = "Security", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // BALANCE CARD
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Balance",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = viewModel.totalBalanceUsd,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ACTIONS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FluidButton(
                        text = "Send",
                        onClick = onNavigateToSend,
                        icon = Icons.Default.ArrowOutward,
                        modifier = Modifier.weight(1f)
                    )
                    FluidButton(
                        text = "Receive",
                        onClick = { showReceiveDialog = true },
                        icon = Icons.Default.FileDownload,
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        textColor = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(32.dp))
            }

            // ASSETS HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Assets",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = { showAddTokenDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Token", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            }

            // ASSET LIST
            items(viewModel.assets) { asset ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTokenDetail(asset.symbol) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Coin Icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                asset.symbol.take(1),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(Modifier.weight(1f)) {
                            Text(asset.symbol, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                            Text(asset.networkName.uppercase(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(asset.balanceUsd, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                            Text(asset.balance, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                }
            }
        }
    }

    // DIALOGS
    if (showReceiveDialog) {
        Dialog(onDismissRequest = { showReceiveDialog = false }) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Receive", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                    Text(viewModel.activeNetwork.name, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                    
                    Spacer(Modifier.height(24.dp))
                    
                    val qrBitmap = remember(viewModel.address) { QrCodeGenerator.generateQrCode(viewModel.address) }
                    Surface(
                        modifier = Modifier.padding(8.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White
                    ) {
                        Image(
                            painter = BitmapPainter(qrBitmap.asImageBitmap()),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(200.dp).padding(16.dp)
                        )
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    val clipboard = LocalClipboardManager.current
                    Surface(
                        onClick = { clipboard.setText(AnnotatedString(viewModel.address)) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = viewModel.address,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (showNetworkSelector) {
        Dialog(onDismissRequest = { showNetworkSelector = false }) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Switch Network", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    viewModel.networks.forEach { network ->
                        Surface(
                            onClick = { 
                                viewModel.switchNetwork(network.id)
                                showNetworkSelector = false
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (viewModel.activeNetwork.id == network.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(network.name, fontWeight = FontWeight.Bold)
                                if (viewModel.activeNetwork.id == network.id) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
