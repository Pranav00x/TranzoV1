package com.antigravity.cryptowallet.ui.browser

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var url by remember { mutableStateOf("") }
    var inputUrl by remember { mutableStateOf("") }
    var webView: WebView? by remember { mutableStateOf(null) }

    var activeNetwork by remember { mutableStateOf(viewModel.activeNetwork) }
    val address = viewModel.walletRepository.getAddress(activeNetwork.id)

    var pendingRequest by remember { mutableStateOf<Web3Bridge.Web3Request?>(null) }
    var bridgeInstance by remember { mutableStateOf<Web3Bridge?>(null) }
    var showNetworkSelector by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val dapps = listOf(
        DApp("PancakeSwap", "Top DEX on BNB", "https://pancakeswap.finance", "https://cryptologos.cc/logos/pancakeswap-cake-logo.png", "DeFi", Color.Transparent),
        DApp("Uniswap", "Swap anytime, anywhere", "https://app.uniswap.org", "https://cryptologos.cc/logos/uniswap-uni-logo.png", "DeFi", Color.Transparent),
        DApp("OpenSea", "NFT Marketplace", "https://opensea.io", "https://storage.googleapis.com/opensea-static/Logomark/Logomark-Blue.png", "NFT", Color.Transparent),
        DApp("1inch", "DeFi Aggregator", "https://app.1inch.io", "https://cryptologos.cc/logos/1inch-1inch-logo.png", "DeFi", Color.Transparent),
        DApp("Aave", "Liquidity Protocol", "https://app.aave.com", "https://cryptologos.cc/logos/aave-aave-logo.png", "DeFi", Color.Transparent),
        DApp("Blur", "NFT Exchange", "https://blur.io", "https://assets.coingecko.com/coins/images/28453/small/blur.png", "NFT", Color.Transparent),
        DApp("Raydium", "Solana AMM", "https://raydium.io/", "https://assets.coingecko.com/coins/images/13928/small/PSigc4ie_400x400.jpg", "DeFi", Color.Transparent),
        DApp("Lido", "Liquid Staking", "https://lido.fi", "https://cryptologos.cc/logos/lido-dao-ldo-logo.png", "DeFi", Color.Transparent)
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
                onRefresh = { webView?.reload() },
                isHome = url.isEmpty(),
                activeNetworkName = activeNetwork.name,
                onNetworkClick = { showNetworkSelector = true }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
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
                    onUpdateUrl = { newUrl -> inputUrl = newUrl },
                    onWebViewCreated = { wv -> webView = wv },
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
                    handleWeb3RequestAsync(request, bridgeInstance, viewModel.walletRepository) { targetChainId ->
                        val targetNet = viewModel.networks.find { it.chainId == targetChainId }
                        if (targetNet != null) {
                            viewModel.switchNetwork(targetNet.id)
                            activeNetwork = viewModel.activeNetwork
                            webView?.post { webView?.reload() }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserTopBar(
    currentUrl: String,
    onValueChange: (String) -> Unit,
    onGo: () -> Unit,
    onHome: () -> Unit,
    onRefresh: () -> Unit,
    isHome: Boolean,
    activeNetworkName: String,
    onNetworkClick: () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surface
    val onContainer = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    Surface(
        color = containerColor,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isHome) {
                IconButton(
                    onClick = onHome,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Home",
                        tint = onContainer
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, outline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = onContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = currentUrl,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = onContainer
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { onGo() }),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (currentUrl.isEmpty()) {
                                Text(
                                    "Search or enter URL",
                                    color = onContainer.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            }
                            inner()
                        },
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                    if (currentUrl.isNotEmpty()) {
                        IconButton(
                            onClick = { onValueChange("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = onContainer.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!isHome) {
                IconButton(onClick = onRefresh, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = onContainer)
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            FilterChip(
                selected = false,
                onClick = onNetworkClick,
                label = {
                    Text(
                        activeNetworkName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = onContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = outline,
                    borderWidth = 1.dp
                )
            )
        }
    }
}

@Composable
fun BrowserHome(dapps: List<DApp>, onDappClick: (DApp) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(24.dp, 16.dp, 24.dp, 100.dp)
    ) {
        item {
            Text(
                "Discover",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Explore dApps and the decentralized web",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                "Featured dApps",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(dapps) { dapp ->
                    DAppCard(dapp = dapp, onClick = { onDappClick(dapp) })
                }
            }
        }
    }
}

@Composable
fun DAppCard(dapp: DApp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = dapp.iconUrl,
                    contentDescription = dapp.name,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                dapp.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                dapp.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2
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
                                return true
                            }
                        }
                        return false
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select network", fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(networks.size) { index ->
                    val net = networks[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(net) }
                            .padding(vertical = 14.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (net.id == activeNetworkId) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .border(
                                    1.5.dp,
                                    if (net.id == activeNetworkId) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            net.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (net.id == activeNetworkId) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (net.id == activeNetworkId) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun Web3RequestDialog(
    request: Web3Bridge.Web3Request,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    val title = when (request.method) {
        "personal_sign", "eth_sign" -> "Sign message"
        "eth_signTypedData", "eth_signTypedData_v3", "eth_signTypedData_v4" -> "Sign typed data"
        "eth_sendTransaction" -> "Confirm transaction"
        "wallet_switchEthereumChain" -> "Switch network"
        "eth_requestAccounts" -> "Connect wallet"
        else -> "DApp request"
    }

    AlertDialog(
        onDismissRequest = onReject,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text("Method: ${request.method}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                val displayParams = try {
                    if (request.method == "personal_sign" || request.method == "eth_sign") {
                        val paramsArray = org.json.JSONArray(request.params)
                        val rawMsg = paramsArray.getString(0)
                        if (rawMsg.startsWith("0x")) {
                            "Decoded:\n" + String(org.web3j.utils.Numeric.hexStringToByteArray(rawMsg), Charsets.UTF_8)
                        } else {
                            rawMsg
                        }
                    } else if (request.method == "eth_requestAccounts") {
                        "Allow this dApp to connect to your wallet?"
                    } else {
                        request.params
                    }
                } catch (e: Exception) {
                    request.params
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        displayParams.take(500) + if (displayParams.length > 500) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onReject) { Text("Reject", color = MaterialTheme.colorScheme.error) }
        }
    )
}

private suspend fun handleWeb3RequestAsync(
    request: Web3Bridge.Web3Request,
    bridge: Web3Bridge?,
    walletRepository: com.antigravity.cryptowallet.data.wallet.WalletRepository,
    onSwitchNetwork: (Long) -> Boolean
) = withContext(Dispatchers.IO) {
    val credentials = walletRepository.activeCredentials ?: run {
        withContext(Dispatchers.Main) { bridge?.sendError(request.id, "Wallet not loaded") }
        return@withContext
    }

    when (request.method) {
        "personal_sign", "eth_sign" -> {
            try {
                val paramsArray = org.json.JSONArray(request.params)
                var message = paramsArray.getString(0)
                if (message.length == 42 && message.startsWith("0x") && paramsArray.length() > 1) {
                    val potentialMessage = paramsArray.getString(1)
                    if (potentialMessage.length != 42) message = potentialMessage
                }
                val data = if (message.startsWith("0x")) org.web3j.utils.Numeric.hexStringToByteArray(message) else message.toByteArray(Charsets.UTF_8)
                val signatureData = org.web3j.crypto.Sign.signPrefixedMessage(data, credentials.ecKeyPair)
                val r = org.web3j.utils.Numeric.cleanHexPrefix(org.web3j.utils.Numeric.toHexString(signatureData.r)).padStart(64, '0')
                val s = org.web3j.utils.Numeric.cleanHexPrefix(org.web3j.utils.Numeric.toHexString(signatureData.s)).padStart(64, '0')
                val v = org.web3j.utils.Numeric.cleanHexPrefix(org.web3j.utils.Numeric.toHexString(signatureData.v)).padStart(2, '0')
                withContext(Dispatchers.Main) { bridge?.sendResponse(request.id, "\"0x$r$s$v\"") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { bridge?.sendError(request.id, e.message ?: "Signing failed") }
            }
        }
        "eth_signTypedData", "eth_signTypedData_v3", "eth_signTypedData_v4" -> {
            try {
                val paramsArray = org.json.JSONArray(request.params)
                val typedData = if (paramsArray.length() > 1) paramsArray.getString(1) else paramsArray.getString(0)
                var signatureData: org.web3j.crypto.Sign.SignatureData? = null
                try {
                    val encoder = org.web3j.crypto.StructuredDataEncoder(typedData)
                    signatureData = org.web3j.crypto.Sign.signMessage(encoder.hashStructuredData(), credentials.ecKeyPair, false)
                } catch (ex: Exception) {
                    signatureData = org.web3j.crypto.Sign.signMessage(org.web3j.crypto.Hash.sha3(typedData.toByteArray(Charsets.UTF_8)), credentials.ecKeyPair, false)
                }
                requireNotNull(signatureData)
                val r = org.web3j.utils.Numeric.cleanHexPrefix(org.web3j.utils.Numeric.toHexString(signatureData.r)).padStart(64, '0')
                val s = org.web3j.utils.Numeric.cleanHexPrefix(org.web3j.utils.Numeric.toHexString(signatureData.s)).padStart(64, '0')
                val v = org.web3j.utils.Numeric.cleanHexPrefix(org.web3j.utils.Numeric.toHexString(signatureData.v)).padStart(2, '0')
                withContext(Dispatchers.Main) { bridge?.sendResponse(request.id, "\"0x$r$s$v\"") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { bridge?.sendError(request.id, e.message ?: "Typed data signing failed") }
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
                val targetChainId = java.lang.Long.decode(paramObj.getString("chainId"))
                val success = onSwitchNetwork(targetChainId)
                withContext(Dispatchers.Main) {
                    if (success) bridge?.sendResponse(request.id, "null")
                    else bridge?.sendError(request.id, "Chain ID $targetChainId not supported")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { bridge?.sendError(request.id, "Switch failed: ${e.message}") }
            }
        }
        "eth_requestPermissions" -> {
            withContext(Dispatchers.Main) { bridge?.sendResponse(request.id, "[{\"parentCapability\": \"eth_accounts\"}]") }
        }
        "eth_sendTransaction" -> {
            try {
                val paramsArray = org.json.JSONArray(request.params)
                val txObj = paramsArray.getJSONObject(0)
                val mockHash = "0x" + (1..64).map { "abcdef0123456789".random() }.joinToString("")
                withContext(Dispatchers.Main) { bridge?.sendResponse(request.id, "\"$mockHash\"") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { bridge?.sendError(request.id, "Transaction failed: ${e.message}") }
            }
        }
        else -> withContext(Dispatchers.Main) { bridge?.sendError(request.id, "Method ${request.method} not supported") }
    }
}
