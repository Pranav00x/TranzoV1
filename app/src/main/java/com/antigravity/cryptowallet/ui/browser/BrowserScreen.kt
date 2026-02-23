package com.antigravity.cryptowallet.ui.browser

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.antigravity.cryptowallet.ui.components.BrutalistButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DApp(
    val name: String,
    val description: String,
    val url: String,
    val iconChar: Char,
    val category: String,
    val color: Color
)

@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val walletRepository = viewModel.walletRepository
    var url by remember { mutableStateOf("") }
    var inputUrl by remember { mutableStateOf("") }
    var webView: WebView? by remember { mutableStateOf(null) }
    
    val address = walletRepository.getAddress()
    var activeNetwork by remember { mutableStateOf(viewModel.activeNetwork) }
    
    var pendingRequest by remember { mutableStateOf<Web3Bridge.Web3Request?>(null) }
    var bridgeInstance by remember { mutableStateOf<Web3Bridge?>(null) }
    var showNetworkSelector by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    val dapps = listOf(
        DApp("PancakeSwap", "Top DEX on BNB", "https://pancakeswap.finance", 'P', "DEFI", Color(0xFF1FC7D4)),
        DApp("Uniswap", "Swap anytime, anywhere", "https://app.uniswap.org", 'U', "DEFI", Color(0xFFFF007A)),
        DApp("OpenSea", "NFT Marketplace", "https://opensea.io", 'O', "NFT", Color(0xFF2081E2)),
        DApp("1inch", "DeFi Aggregator", "https://app.1inch.io", '1', "DEFI", Color(0xFF0C162D)),
        DApp("Aave", "Liquidity Protocol", "https://app.aave.com", 'A', "DEFI", Color(0xFFB6509E)),
        DApp("Blur", "NFT Exchange", "https://blur.io", 'B', "NFT", Color(0xFFFF5200)),
        DApp("Lens", "Web3 Social", "https://www.lens.xyz", 'L', "SOCIAL", Color(0xFFABFE2C)),
        DApp("Snapshot", "DAO Voting", "https://snapshot.org", 'S', "DAO", Color(0xFFFFB503))
    )

    BackHandler(enabled = url.isNotEmpty()) {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            url = ""
            inputUrl = ""
        }
    }

    Scaffold(
        topBar = {
            BrowserTopBar(
                currentUrl = inputUrl,
                onValueChange = { inputUrl = it },
                onGo = {
                    if (inputUrl.isNotEmpty()) {
                        url = if (!inputUrl.startsWith("http")) "https://$inputUrl" else inputUrl
                    }
                },
                onHome = {
                    url = ""
                    inputUrl = ""
                },
                isHome = url.isEmpty(),
                activeNetworkName = activeNetwork.name,
                onNetworkClick = { showNetworkSelector = true }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (showNetworkSelector) {
                NetworkSelector(
                    networks = viewModel.networks,
                    activeNetworkId = activeNetwork.id,
                    onSelect = { net ->
                        viewModel.switchNetwork(net.id)
                        activeNetwork = viewModel.activeNetwork
                        showNetworkSelector = false
                        webView?.reload()
                    },
                    onDismiss = { showNetworkSelector = false }
                )
            }

            if (url.isEmpty()) {
                BrowserHome(
                    dapps = dapps,
                    onDappClick = { dapp ->
                        url = dapp.url
                        inputUrl = dapp.url
                    }
                )
            } else {
                BrowserWebView(
                    url = url,
                    onUpdateUrl = { newUrl -> 
                        inputUrl = newUrl 
                    },
                    onWebViewCreated = { wv -> 
                        webView = wv 
                    },
                    address = address,
                    chainIdProvider = { activeNetwork.chainId },
                    onPendingRequest = { req, bridge ->
                        pendingRequest = req
                        bridgeInstance = bridge
                    }
                )
            }
        }
    }

    pendingRequest?.let { request ->
        Web3RequestDialog(
            request = request,
            onConfirm = {
                scope.launch {
                    handleWeb3RequestAsync(request, bridgeInstance, walletRepository) { targetChainId ->
                        val targetNet = viewModel.networks.find { it.chainId == targetChainId }
                         if (targetNet != null) {
                            viewModel.switchNetwork(targetNet.id)
                            activeNetwork = viewModel.activeNetwork
                            webView?.post {
                                webView?.reload()
                            }
                            true
                        } else {
                            false
                        }
                    }
                }
                pendingRequest = null
            },
            onReject = {
                 bridgeInstance?.sendError(request.id, "User rejected")
                 pendingRequest = null
            }
        )
    }
}

@Composable
fun BrowserTopBar(
    currentUrl: String,
    onValueChange: (String) -> Unit,
    onGo: () -> Unit,
    onHome: () -> Unit,
    isHome: Boolean,
    activeNetworkName: String,
    onNetworkClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isHome) {
                IconButton(onClick = onHome, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Home, contentDescription = "Home", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = currentUrl,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onGo() }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (currentUrl.isEmpty()) {
                            Text(
                                "Search DApps or Enter URL", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
                if (currentUrl.isNotEmpty()) {
                     IconButton(
                         onClick = { onValueChange("") },
                         modifier = Modifier.size(20.dp)
                     ) {
                         Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                     }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .clickable { onNetworkClick() }
                    .height(36.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF4CAF50), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        activeNetworkName.take(3).uppercase(), 
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BrowserHome(dapps: List<DApp>, onDappClick: (DApp) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                "Favorites",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
             LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(dapps.take(8)) { dapp ->
                    DAppIconItem(dapp, onClick = { onDappClick(dapp) })
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Explore",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(dapps.size) { index ->
            DAppListItem(dapp = dapps[index], onClick = { onDappClick(dapps[index]) })
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun DAppIconItem(dapp: DApp, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(dapp.color, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                dapp.iconChar.toString(),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            dapp.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun DAppListItem(dapp: DApp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(dapp.color, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                dapp.iconChar.toString(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                dapp.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                dapp.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun BrowserWebView(
    url: String,
    onUpdateUrl: (String) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    address: String,
    chainIdProvider: () -> Long,
    onPendingRequest: (Web3Bridge.Web3Request, Web3Bridge) -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }
                
                val bridge = Web3Bridge(this, address, chainIdProvider) { request ->
                    onPendingRequest(request, this.tag as? Web3Bridge ?: return@Web3Bridge)
                }
                this.tag = bridge
                addJavascriptInterface(bridge, "androidWallet")
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                         super.onPageStarted(view, url, favicon)
                         val currentBridge = (view?.tag as? Web3Bridge) ?: bridge
                         view?.evaluateJavascript(currentBridge.getInjectionJs(), null)
                    }
                    
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (url != null) onUpdateUrl(url)
                        val currentBridge = (view?.tag as? Web3Bridge) ?: bridge
                        view?.evaluateJavascript(currentBridge.getInjectionJs(), null)
                    }
                }
                
                loadUrl(url)
                onWebViewCreated(this)
            }
        },
        update = {},
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun NetworkSelector(
    networks: List<com.antigravity.cryptowallet.data.blockchain.Network>,
    activeNetworkId: String,
    onSelect: (com.antigravity.cryptowallet.data.blockchain.Network) -> Unit,
    onDismiss: () -> Unit
) {
     androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Select Network",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn {
                    items(networks.size) { index ->
                        val net = networks[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(net) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = net.id == activeNetworkId,
                                onClick = { onSelect(net) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(net.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (index < networks.size - 1) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                BrutalistButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    inverted = true
                )
            }
        }
    }
}

@Composable
fun Web3RequestDialog(
    request: Web3Bridge.Web3Request,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        title = { 
            Text(
                when(request.method) {
                    "personal_sign", "eth_sign" -> "Sign Message"
                    "eth_signTypedData_v4" -> "Sign Typed Data"
                    "eth_sendTransaction" -> "Confirm Transaction"
                    "wallet_switchEthereumChain" -> "Switch Network"
                    else -> "DApp Request"
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column {
                Text(
                    "Method: ${request.method}", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        request.params.take(200) + if (request.params.length > 200) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { 
                Text("Confirm", fontWeight = FontWeight.Bold) 
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) { 
                Text("Reject", color = MaterialTheme.colorScheme.error) 
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

private suspend fun handleWeb3RequestAsync(
    request: Web3Bridge.Web3Request,
    bridge: Web3Bridge?,
    walletRepository: com.antigravity.cryptowallet.data.wallet.WalletRepository,
    onSwitchNetwork: (Long) -> Boolean
) = withContext(Dispatchers.IO) {
    val credentials = walletRepository.activeCredentials ?: run {
        withContext(Dispatchers.Main) {
            bridge?.sendError(request.id, "Wallet not loaded")
        }
        return@withContext
    }
    
    when (request.method) {
        "personal_sign", "eth_sign" -> {
            try {
                val paramsArray = org.json.JSONArray(request.params)
                val message = paramsArray.getString(0)
                
                val data = if (message.startsWith("0x")) {
                    org.web3j.utils.Numeric.hexStringToByteArray(message)
                } else {
                    message.toByteArray(Charsets.UTF_8)
                }
                val signatureData = org.web3j.crypto.Sign.signPrefixedMessage(data, credentials.ecKeyPair)
                
                val r = org.web3j.utils.Numeric.toHexStringNoPrefix(signatureData.r)
                val s = org.web3j.utils.Numeric.toHexStringNoPrefix(signatureData.s)
                val v = org.web3j.utils.Numeric.toHexStringNoPrefix(signatureData.v)
                val signature = "0x$r$s$v"
                
                withContext(Dispatchers.Main) {
                    bridge?.sendResponse(request.id, "\"$signature\"")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    bridge?.sendError(request.id, e.message ?: "Signing failed")
                }
            }
        }
        "eth_signTypedData_v4" -> {
            try {
                // For typed data, we need the data param (second param typically)
                val paramsArray = org.json.JSONArray(request.params)
                val typedData = if (paramsArray.length() > 1) paramsArray.getString(1) else paramsArray.getString(0)
                
                // Hash the typed data and sign
                val dataHash = org.web3j.crypto.Hash.sha3(typedData.toByteArray(Charsets.UTF_8))
                val signatureData = org.web3j.crypto.Sign.signMessage(dataHash, credentials.ecKeyPair, false)
                
                val r = org.web3j.utils.Numeric.toHexStringNoPrefix(signatureData.r)
                val s = org.web3j.utils.Numeric.toHexStringNoPrefix(signatureData.s)
                val v = org.web3j.utils.Numeric.toHexStringNoPrefix(signatureData.v)
                val signature = "0x$r$s$v"
                
                withContext(Dispatchers.Main) {
                    bridge?.sendResponse(request.id, "\"$signature\"")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    bridge?.sendError(request.id, e.message ?: "Typed data signing failed")
                }
            }
        }
        "wallet_switchEthereumChain" -> {
            try {
                val paramsArray = org.json.JSONArray(request.params)
                val paramObj = paramsArray.getJSONObject(0)
                val targetChainIdHex = paramObj.getString("chainId")
                val targetChainId = java.lang.Long.decode(targetChainIdHex)
                
                val success = onSwitchNetwork(targetChainId)
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        bridge?.sendResponse(request.id, "null")
                    } else {
                        bridge?.sendError(request.id, "Chain ID $targetChainId not supported")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    bridge?.sendError(request.id, "Switch failed: ${e.message}")
                }
            }
        }
        "eth_requestPermissions" -> {
            withContext(Dispatchers.Main) {
                bridge?.sendResponse(request.id, "[{\"parentCapability\": \"eth_accounts\"}]")
            }
        }
        "eth_sendTransaction" -> {
            // For now, mock success - real implementation would build and send the transaction
            // Parse params to get to, value, data etc and call blockchainService
            try {
                val paramsArray = org.json.JSONArray(request.params)
                val txObj = paramsArray.getJSONObject(0)
                
                // In a real implementation:
                // val to = txObj.getString("to")
                // val value = txObj.optString("value", "0x0")
                // val data = txObj.optString("data", "0x")
                // Then call blockchainService.sendRawTransaction(...)
                
                // For now, return mock hash
                val mockHash = "0x" + (1..64).map { "abcdef0123456789".random() }.joinToString("")
                
                withContext(Dispatchers.Main) {
                    bridge?.sendResponse(request.id, "\"$mockHash\"")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    bridge?.sendError(request.id, "Transaction failed: ${e.message}")
                }
            }
        }
        else -> {
            withContext(Dispatchers.Main) {
                bridge?.sendError(request.id, "Method ${request.method} not supported")
            }
        }
    }
}
