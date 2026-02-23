package com.antigravity.cryptowallet.ui.wallet

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Connected • ${viewModel.address.take(6)}...${viewModel.address.takeLast(4)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Row {
                IconButton(
                    onClick = { viewModel.refresh() },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).size(40.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                IconButton(
                    onClick = onSetupSecurity,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).size(40.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = "Security", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .shadow(24.dp, RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(listOf(PrimaryVariant, SecondaryVariant)),
                            RoundedCornerShape(28.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(28.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Balance", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = viewModel.totalBalanceUsd,
                                style = MaterialTheme.typography.displayLarge,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        
                        Text(
                            text = "Main Wallet Assets",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Assets",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showAddTokenDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Token", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ASSET LIST
            items(viewModel.assets) { asset ->
                Surface(
                    onClick = { onNavigateToTokenDetail(asset.symbol) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Coin Icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                asset.symbol.take(1),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(Modifier.weight(1f)) {
                            Text(asset.symbol, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(asset.networkName.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(asset.balanceUsd, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(asset.balance, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
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
                        color = Color.White,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
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
