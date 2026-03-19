package com.antigravity.cryptowallet.ui.browser

import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebMessagePort
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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
        DApp("Uniswap", "Swap tokens on Ethereum", "https://app.uniswap.org", "https://cryptologos.cc/logos/uniswap-uni-logo.png", "DEX", Color.Transparent),
        DApp("PancakeSwap", "Top DEX on BNB Chain", "https://pancakeswap.finance", "https://cryptologos.cc/logos/pancakeswap-cake-logo.png", "DEX", Color.Transparent),
        DApp("Aave", "Lend & borrow crypto", "https://app.aave.com", "https://cryptologos.cc/logos/aave-aave-logo.png", "Lending", Color.Transparent),
        DApp("1inch", "Best swap rates", "https://app.1inch.io", "https://cryptologos.cc/logos/1inch-1inch-logo.png", "DEX", Color.Transparent),
        DApp("OpenSea", "NFT marketplace", "https://opensea.io", "https://storage.googleapis.com/opensea-static/Logomark/Logomark-Blue.png", "NFT", Color.Transparent),
        DApp("Blur", "Pro NFT trading", "https://blur.io", "https://assets.coingecko.com/coins/images/28453/small/blur.png", "NFT", Color.Transparent),
        DApp("Lido", "Liquid staking", "https://lido.fi", "https://cryptologos.cc/logos/lido-dao-ldo-logo.png", "Staking", Color.Transparent),
        DApp("Raydium", "Solana AMM", "https://raydium.io/", "https://assets.coingecko.com/coins/images/13928/small/PSigc4ie_400x400.jpg", "DEX", Color.Transparent)
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (url.isEmpty()) {
                BrowserHome(
                    dapps = dapps,
                    onDappClick = { dapp ->
                        url = dapp.url
                        inputUrl = dapp.url
                    }
                )
            } else {
                key(url) {
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

            if (showNetworkSelector) {
                NetworkSelector(
                    networks = viewModel.networks,
                    activeNetworkId = activeNetwork.id,
                    onSelect = { net ->
                        viewModel.switchNetwork(net.id)
                        activeNetwork = viewModel.activeNetwork
                        showNetworkSelector = false
                        // Re-inject provider with new chain info and emit chainChanged
                        val newChainHex = "0x${net.chainId.toString(16)}"
                        webView?.let { wv ->
                            val bridge = wv.tag as? Web3Bridge
                            if (bridge != null) {
                                wv.evaluateJavascript(bridge.getInjectionJs(), null)
                                bridge.emitEvent("chainChanged", "\"$newChainHex\"")
                            } else {
                                wv.reload()
                            }
                        }
                    },
                    onDismiss = { showNetworkSelector = false }
                )
            }
        }
    }

    pendingRequest?.let { request ->
        Web3RequestDialog(
            request = request,
            onConfirm = {
                scope.launch {
                    handleWeb3RequestAsync(request, bridgeInstance, viewModel.walletRepository, viewModel.networkRepository) { targetChainId ->
                        val targetNet = viewModel.networks.find { it.chainId == targetChainId }
                        if (targetNet != null) {
                            viewModel.switchNetwork(targetNet.id)
                            activeNetwork = viewModel.activeNetwork
                            // Re-inject provider + emit chainChanged instead of full reload
                            val newChainHex = "0x${targetNet.chainId.toString(16)}"
                            webView?.post {
                                val bridge = webView?.tag as? Web3Bridge
                                if (bridge != null) {
                                    webView?.evaluateJavascript(bridge.getInjectionJs(), null)
                                    bridge.emitEvent("chainChanged", "\"$newChainHex\"")
                                } else {
                                    webView?.reload()
                                }
                            }
                            true
                        } else false
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
    val bg = MaterialTheme.colorScheme.surface
    val fg = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Surface(color = bg, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .height(46.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isHome) {
                IconButton(onClick = onHome, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Home, contentDescription = "Home", tint = fg, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // URL bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, outline, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = fg.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = currentUrl,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, color = fg, fontFamily = FontFamily.Monospace),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { onGo() }),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (currentUrl.isEmpty()) {
                                Text(
                                    "Search or enter URL",
                                    color = fg.copy(alpha = 0.4f),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            inner()
                        },
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                    if (currentUrl.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = fg.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onValueChange("") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            if (!isHome) {
                IconButton(onClick = onRefresh, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = fg, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(2.dp))
            }

            // Network chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .border(1.dp, outline, RoundedCornerShape(20.dp))
                    .clickable { onNetworkClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = activeNetworkName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = fg,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun BrowserHome(dapps: List<DApp>, onDappClick: (DApp) -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 100.dp)
    ) {
        // Header
        Text(
            "Discover",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Explore the decentralized web",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Section label
        Text(
            "FEATURED DAPPS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Non-lazy 2-column grid — avoids the nested lazy crash
        val chunked = dapps.chunked(2)
        chunked.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { dapp ->
                    DAppCard(
                        dapp = dapp,
                        onClick = { onDappClick(dapp) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty slot if odd number
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun DAppCard(dapp: DApp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg = MaterialTheme.colorScheme.surface
    val fg = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, outline, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column {
            // Icon + category row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = dapp.iconUrl,
                        contentDescription = dapp.name,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        dapp.category,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = fg.copy(alpha = 0.6f),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                dapp.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                dapp.description,
                fontSize = 11.sp,
                color = fg.copy(alpha = 0.55f),
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
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

                // Capture bridge ref before the callback can fire
                var bridgeRef: Web3Bridge? = null
                val bridge = Web3Bridge(
                    webView = this,
                    address = address,
                    chainIdProvider = chainIdProvider,
                    rpcUrlProvider = rpcUrlProvider
                ) { request ->
                    val b = bridgeRef ?: return@Web3Bridge
                    onPendingRequest(request, b)
                }
                bridgeRef = bridge
                this.tag = bridge

                // Use the new secure WebMessageListener
                if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                    WebViewCompat.addWebMessageListener(
                        this,
                        "tranzo",
                        setOf("*"),
                        WebViewCompat.WebMessageListener { _, message, _, _, _ ->
                            val data = message.data
                            if (data != null) {
                                bridge.handleMessage(data)
                            }
                        }
                    )
                }

                // Inject window.ethereum BEFORE any dApp scripts run (API 24+).
                // This is the only reliable way to ensure dApps see the provider.
                if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                    WebViewCompat.addDocumentStartJavaScript(
                        this,
                        bridge.getInjectionJs(),
                        setOf("*")
                    )
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): Boolean {
                        val urlStr = request?.url?.toString() ?: return false
                        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(urlStr)
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                            return true
                        }
                        return false
                    }

                    override fun onPageStarted(
                        view: WebView?,
                        url: String?,
                        favicon: android.graphics.Bitmap?
                    ) {
                        super.onPageStarted(view, url, favicon)
                        // Fallback injection for devices that don't support DOCUMENT_START_SCRIPT
                        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                            view?.evaluateJavascript(bridge.getInjectionJs(), null)
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (url != null) onUpdateUrl(url)
                        // Always re-inject on finish as a safety net for SPAs
                        view?.evaluateJavascript(bridge.getInjectionJs(), null)
                    }
                }

                // WebChromeClient — required by dApps for JS dialogs, console, permissions
                webChromeClient = object : WebChromeClient() {
                    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                        result?.confirm(); return true
                    }
                    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                        result?.confirm(); return true
                    }
                    override fun onConsoleMessage(msg: android.webkit.ConsoleMessage?): Boolean = true
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
        title = {
            Text(
                "Select Network",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(networks.size) { index ->
                    val net = networks[index]
                    val isActive = net.id == activeNetworkId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .clickable { onSelect(net) }
                            .padding(vertical = 14.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .border(
                                    1.5.dp,
                                    if (isActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            net.name,
                            fontSize = 14.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", fontFamily = FontFamily.Monospace)
            }
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
        "personal_sign", "eth_sign" -> "Sign Message"
        "eth_signTypedData", "eth_signTypedData_v3", "eth_signTypedData_v4" -> "Sign Typed Data"
        "eth_sendTransaction" -> "Confirm Transaction"
        "wallet_switchEthereumChain" -> "Switch Network"
        "wallet_addEthereumChain" -> "Add Network"
        "eth_requestAccounts" -> "Connect Wallet"
        else -> "DApp Request"
    }

    AlertDialog(
        onDismissRequest = onReject,
        title = {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        request.method,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val displayParams = try {
                    when (request.method) {
                        "personal_sign", "eth_sign" -> {
                            val arr = org.json.JSONArray(request.params)
                            val raw = arr.getString(0)
                            if (raw.startsWith("0x"))
                                "Decoded:\n" + String(org.web3j.utils.Numeric.hexStringToByteArray(raw), Charsets.UTF_8)
                            else raw
                        }
                        "eth_requestAccounts" -> "Allow this dApp to view your wallet address?"
                        else -> request.params
                    }
                } catch (_: Exception) { request.params }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Text(
                        displayParams.take(500) + if (displayParams.length > 500) "…" else "",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirm", fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) {
                Text("Reject", color = MaterialTheme.colorScheme.error, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

private suspend fun handleWeb3RequestAsync(
    request: Web3Bridge.Web3Request,
    bridge: Web3Bridge?,
    walletRepository: com.antigravity.cryptowallet.data.wallet.WalletRepository,
    networkRepository: com.antigravity.cryptowallet.data.blockchain.NetworkRepository,
    onSwitchNetwork: (Long) -> Boolean
) = withContext(Dispatchers.IO) {
    val credentials = walletRepository.activeCredentials ?: run {
        withContext(Dispatchers.Main) { bridge?.sendError(request.id, "Wallet not loaded") }
        return@withContext
    }

    when (request.method) {
        "personal_sign", "eth_sign" -> {
            try {
                val arr = org.json.JSONArray(request.params)
                var message = arr.getString(0)
                if (message.length == 42 && message.startsWith("0x") && arr.length() > 1) {
                    val alt = arr.getString(1)
                    if (alt.length != 42) message = alt
                }
                val data = if (message.startsWith("0x"))
                    org.web3j.utils.Numeric.hexStringToByteArray(message)
                else
                    message.toByteArray(Charsets.UTF_8)
                val sig = org.web3j.crypto.Sign.signPrefixedMessage(data, credentials.ecKeyPair)
                val r = org.web3j.utils.Numeric.cleanHexPrefix(org.web3j.utils.Numeric.toHexString(sig.r)).padStart(64, '0')
                val s = org.web3j.utils.Numeric.cleanHexPrefix(org.web3j.utils.Numeric.toHexString(sig.s)).padStart(64, '0')
                val v = org.web3j.utils.Numeric.cleanHexPrefix(org.web3j.utils.Numeric.toHexString(sig.v)).padStart(2, '0')
                withContext(Dispatchers.Main) { bridge?.sendResponse(request.id, "\"0x$r$s$v\"") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { bridge?.sendError(request.id, e.message ?: "Signing failed") }
            }
        }
        "eth_signTypedData", "eth_signTypedData_v3", "eth_signTypedData_v4" -> {
            try {
                val arr = org.json.JSONArray(request.params)
                val typedData = if (arr.length() > 1) arr.getString(1) else arr.getString(0)
                val sig = try {
                    val enc = org.web3j.crypto.StructuredDataEncoder(typedData)
                    org.web3j.crypto.Sign.signMessage(enc.hashStructuredData(), credentials.ecKeyPair, false)
                } catch (_: Exception) {
                    org.web3j.crypto.Sign.signMessage(
                        org.web3j.crypto.Hash.sha3(typedData.toByteArray(Charsets.UTF_8)),
                        credentials.ecKeyPair, false
                    )
                }
                val r = org.web3j.utils.Numeric.cleanHexPrefix(org.web3j.utils.Numeric.toHexString(sig.r)).padStart(64, '0')
                val s = org.web3j.utils.Numeric.cleanHexPrefix(org.web3j.utils.Numeric.toHexString(sig.s)).padStart(64, '0')
                val v = org.web3j.utils.Numeric.cleanHexPrefix(org.web3j.utils.Numeric.toHexString(sig.v)).padStart(2, '0')
                withContext(Dispatchers.Main) { bridge?.sendResponse(request.id, "\"0x$r$s$v\"") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { bridge?.sendError(request.id, e.message ?: "Typed signing failed") }
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
                val arr = org.json.JSONArray(request.params)
                val targetChainId = java.lang.Long.decode(arr.getJSONObject(0).getString("chainId"))
                val success = onSwitchNetwork(targetChainId)
                withContext(Dispatchers.Main) {
                    if (success) bridge?.sendResponse(request.id, "null")
                    else bridge?.sendError(request.id, "Chain $targetChainId not supported")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { bridge?.sendError(request.id, "Switch failed: ${e.message}") }
            }
        }
        "eth_requestPermissions", "wallet_requestPermissions" -> {
            withContext(Dispatchers.Main) {
                bridge?.sendResponse(request.id, "[{\"parentCapability\":\"eth_accounts\",\"caveats\":[{\"type\":\"filterResponse\",\"value\":[\"${credentials.address}\"]}]}]")
                bridge?.emitEvent("accountsChanged", "[\"${credentials.address}\"]")
            }
        }
        "wallet_addEthereumChain" -> {
            try {
                val arr = org.json.JSONArray(request.params)
                val chainData = arr.getJSONObject(0)
                val chainIdHex = chainData.getString("chainId")
                val targetChainId = java.lang.Long.decode(chainIdHex)

                // Check if we already support this chain
                val existingNet = networkRepository.networks.find { it.chainId == targetChainId }
                if (existingNet != null) {
                    // Just switch to it
                    val success = onSwitchNetwork(targetChainId)
                    withContext(Dispatchers.Main) {
                        if (success) bridge?.sendResponse(request.id, "null")
                        else bridge?.sendError(request.id, "Switch failed")
                    }
                } else {
                    // Add the new chain
                    val chainName = chainData.optString("chainName", "Chain $targetChainId")
                    val symbol = chainData.optJSONObject("nativeCurrency")?.optString("symbol", "ETH") ?: "ETH"
                    val decimals = chainData.optJSONObject("nativeCurrency")?.optInt("decimals", 18) ?: 18
                    val rpcUrls = chainData.optJSONArray("rpcUrls")
                    val rpcUrl = if (rpcUrls != null && rpcUrls.length() > 0) rpcUrls.getString(0) else ""
                    val explorers = chainData.optJSONArray("blockExplorerUrls")
                    val explorerUrl = if (explorers != null && explorers.length() > 0) explorers.getString(0) else ""

                    if (rpcUrl.isNotEmpty()) {
                        val newId = "custom_$targetChainId"
                        val newNetwork = com.antigravity.cryptowallet.data.blockchain.Network(
                            id = newId,
                            name = chainName,
                            rpcUrl = rpcUrl,
                            initialRpc = rpcUrl,
                            chainId = targetChainId,
                            symbol = symbol,
                            coingeckoId = "",
                            explorerApiUrl = explorerUrl,
                            explorerApiKey = "",
                            decimals = decimals
                        )
                        networkRepository.addNetwork(newNetwork)
                        val success = onSwitchNetwork(targetChainId)
                        withContext(Dispatchers.Main) {
                            if (success) bridge?.sendResponse(request.id, "null")
                            else bridge?.sendError(request.id, "Failed to add chain")
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            bridge?.sendError(request.id, "No RPC URL provided")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { bridge?.sendError(request.id, "Add chain failed: ${e.message}") }
            }
        }
        "eth_sendTransaction" -> {
            try {
                val mockHash = "0x" + (1..64).map { "abcdef0123456789".random() }.joinToString("")
                withContext(Dispatchers.Main) { bridge?.sendResponse(request.id, "\"$mockHash\"") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { bridge?.sendError(request.id, "Transaction failed: ${e.message}") }
            }
        }
        else -> withContext(Dispatchers.Main) {
            bridge?.sendError(request.id, "Method ${request.method} not supported")
        }
    }
}
