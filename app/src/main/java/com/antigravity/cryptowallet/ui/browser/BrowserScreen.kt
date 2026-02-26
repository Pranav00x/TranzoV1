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
import coil.compose.AsyncImage

data class DApp(
    val name: String,
    val description: String,
    val url: String,
    val iconUrl: String,
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
    
    var activeNetwork by remember { mutableStateOf(viewModel.activeNetwork) }
    val address = walletRepository.getAddress(activeNetwork.id)
    
    var pendingRequest by remember { mutableStateOf<Web3Bridge.Web3Request?>(null) }
    var bridgeInstance by remember { mutableStateOf<Web3Bridge?>(null) }
    var showNetworkSelector by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    val dapps = listOf(
        DApp("PancakeSwap", "Top DEX on BNB", "https://pancakeswap.finance", "https://cryptologos.cc/logos/pancakeswap-cake-logo.png", "DEFI", Color.Transparent),
        DApp("Uniswap", "Swap anytime, anywhere", "https://app.uniswap.org", "https://cryptologos.cc/logos/uniswap-uni-logo.png", "DEFI", Color.Transparent),
        DApp("OpenSea", "NFT Marketplace", "https://opensea.io", "https://storage.googleapis.com/opensea-static/Logomark/Logomark-Blue.png", "NFT", Color.Transparent),
        DApp("1inch", "DeFi Aggregator", "https://app.1inch.io", "https://cryptologos.cc/logos/1inch-1inch-logo.png", "DEFI", Color.Transparent),
        DApp("Aave", "Liquidity Protocol", "https://app.aave.com", "https://cryptologos.cc/logos/aave-aave-logo.png", "DEFI", Color.Transparent),
        DApp("Blur", "NFT Exchange", "https://blur.io", "https://assets.coingecko.com/coins/images/28453/small/blur.png", "NFT", Color.Transparent),
        DApp("Raydium", "Solana AMM", "https://raydium.io/", "https://assets.coingecko.com/coins/images/13928/small/PSigc4ie_400x400.jpg", "DEFI", Color.Transparent),
        DApp("Lido", "Liquid Staking", "https://lido.fi", "https://cryptologos.cc/logos/lido-dao-ldo-logo.png", "DEFI", Color.Transparent)
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
                    rpcUrlProvider = { activeNetwork.rpcUrl },
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
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isHome) {
                    Surface(
                        shape = RoundedCornerShape(0.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.clickable { onHome() }.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Home, contentDescription = "Home", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Surface(
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(0.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("HTTP://", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp), color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f))
                        Spacer(modifier = Modifier.width(4.dp))
                        BasicTextField(
                            value = currentUrl,
                            onValueChange = onValueChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { onGo() }),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (currentUrl.isEmpty()) {
                                    Text(
                                        "SEARCH_OR_ENTER_URL", 
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                    )
                                }
                                innerTextField()
                            },
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                        )
                        if (currentUrl.isNotEmpty()) {
                             IconButton(
                                 onClick = { onValueChange("") },
                                 modifier = Modifier.size(24.dp)
                             ) {
                                 Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onBackground)
                             }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(0.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .clickable { onNetworkClick() }
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFF00FF00)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            activeNetworkName.take(3).uppercase(), 
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            Divider(color = MaterialTheme.colorScheme.onBackground, thickness = 2.dp)
        }
    }
}

@Composable
fun BrowserHome(dapps: List<DApp>, onDappClick: (DApp) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onBackground)
                    .padding(24.dp)
            ) {
                Text(
                    "DAPP BROWSER",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-2).sp
                    ),
                    color = MaterialTheme.colorScheme.background
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "ACCESS THE UNRESTRICTED DECENTRALIZED WEB. NO TRACKING. PURE EXECUTION.",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                )
            }
            Divider(color = MaterialTheme.colorScheme.onBackground, thickness = 4.dp)
        }
        
        item {
            Text(
                "// FEATURED_PROTOCOLS",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 24.dp, top = 32.dp, bottom = 24.dp)
            )
        }
        
        items((dapps.size + 1) / 2) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val idx1 = rowIndex * 2
                val idx2 = idx1 + 1
                
                DAppBrutalistCard(dapp = dapps[idx1], onClick = { onDappClick(dapps[idx1]) }, modifier = Modifier.weight(1f))
                
                if (idx2 < dapps.size) {
                    DAppBrutalistCard(dapp = dapps[idx2], onClick = { onDappClick(dapps[idx2]) }, modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f)) 
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DAppBrutalistCard(dapp: DApp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(0.dp),
        color = MaterialTheme.colorScheme.background,
        border = androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onBackground),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.onBackground),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = dapp.iconUrl,
                    contentDescription = dapp.name,
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(0.dp)).background(MaterialTheme.colorScheme.background)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                dapp.name.uppercase(),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                dapp.description.uppercase(),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LAUNCH",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "->",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
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
    rpcUrlProvider: () -> String,
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
                
                val bridge = Web3Bridge(
                    webView = this, 
                    address = address, 
                    chainIdProvider = chainIdProvider, 
                    rpcUrlProvider = rpcUrlProvider
                ) { request ->
                    onPendingRequest(request, this.tag as? Web3Bridge ?: return@Web3Bridge)
                }
                this.tag = bridge
                addJavascriptInterface(bridge, "androidWallet")
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                        val urlStr = request?.url?.toString() ?: return false
                        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(urlStr))
                                context.startActivity(intent)
                                return true
                            } catch (e: Exception) {
                                e.printStackTrace()
                                return true // Prevent WebView from showing an error for unsupported custom URL schemes
                            }
                        }
                        return false // Let WebView load HTTP/HTTPS URLs
                    }

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
            shape = RoundedCornerShape(0.dp),
            border = androidx.compose.foundation.BorderStroke(4.dp, MaterialTheme.colorScheme.onBackground),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "// SELECT_NETWORK",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(networks.size) { index ->
                        val net = networks[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(net) }
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(0.dp),
                                color = if (net.id == activeNetworkId) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier.size(24.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                net.name.uppercase(), 
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = if (net.id == activeNetworkId) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f)
                            )
                        }
                        if (index < networks.size - 1) {
                            Divider(color = MaterialTheme.colorScheme.onBackground, thickness = 2.dp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                BrutalistButton(
                    text = "CANCEL",
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
                    "eth_signTypedData", "eth_signTypedData_v3", "eth_signTypedData_v4" -> "Sign Typed Data"
                    "eth_sendTransaction" -> "Confirm Transaction"
                    "wallet_switchEthereumChain" -> "Switch Network"
                    "eth_requestAccounts" -> "Connection Request"
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
                
                val displayParams = try {
                    if (request.method == "personal_sign" || request.method == "eth_sign") {
                        val paramsArray = org.json.JSONArray(request.params)
                        val rawMsg = paramsArray.getString(0)
                        if (rawMsg.startsWith("0x")) {
                            "Decoded Message:\n" + String(org.web3j.utils.Numeric.hexStringToByteArray(rawMsg), Charsets.UTF_8)
                        } else {
                            "Message:\n$rawMsg"
                        }
                    } else if (request.method == "eth_requestAccounts") {
                        "Allow this DApp to connect to your wallet?"
                    } else {
                        request.params
                    }
                } catch (e: Exception) {
                    request.params
                }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        displayParams.take(500) + if (displayParams.length > 500) "..." else "",
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
                var message = paramsArray.getString(0)
                
                // Handle out-of-spec DApps that pass address first instead of challenge
                if (message.length == 42 && message.startsWith("0x") && paramsArray.length() > 1) {
                    val potentialMessage = paramsArray.getString(1)
                    if (potentialMessage.length != 42) {
                         message = potentialMessage
                    }
                }
                
                val data = if (message.startsWith("0x")) {
                    org.web3j.utils.Numeric.hexStringToByteArray(message)
                } else {
                    message.toByteArray(Charsets.UTF_8)
                }
                val signatureData = org.web3j.crypto.Sign.signPrefixedMessage(data, credentials.ecKeyPair)
                
                // Guarantee strictly exactly 64 chars of padding for r and s elements, and 2 for v
                val r = signatureData.r.joinToString("") { "%02x".format(it) }
                val s = signatureData.s.joinToString("") { "%02x".format(it) }
                val v = signatureData.v.joinToString("") { "%02x".format(it) }
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
        "eth_signTypedData", "eth_signTypedData_v3", "eth_signTypedData_v4" -> {
            try {
                // For typed data, we need the data param (second param typically)
                val paramsArray = org.json.JSONArray(request.params)
                val typedData = if (paramsArray.length() > 1) paramsArray.getString(1) else paramsArray.getString(0)
                
                // Hash the typed data and sign
                val dataHash = org.web3j.crypto.Hash.sha3(typedData.toByteArray(Charsets.UTF_8))
                val signatureData = org.web3j.crypto.Sign.signMessage(dataHash, credentials.ecKeyPair, false)
                
                val r = signatureData.r.joinToString("") { "%02x".format(it) }
                val s = signatureData.s.joinToString("") { "%02x".format(it) }
                val v = signatureData.v.joinToString("") { "%02x".format(it) }
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
        "eth_requestAccounts" -> {
            withContext(Dispatchers.Main) {
                bridge?.sendResponse(request.id, "[\"${credentials.address}\"]")
                bridge?.emitEvent("accountsChanged", "[\"${credentials.address}\"]")
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
