package com.antigravity.cryptowallet.ui.browser

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import org.json.JSONObject

class Web3Bridge(
    private val webView: WebView,
    private val address: String,
    private val chainIdProvider: () -> Long,
    private val rpcUrlProvider: () -> String,
    private val onActionRequest: (Web3Request) -> Unit
) {
    private val gson = Gson()

    data class Web3Request(
        val id: String,
        val method: String,
        val params: String
    )

    fun getInjectionJs(): String {
        val chainId = chainIdProvider()
        return """
            (function() {
                if (window.ethereum) return;
                
                const address = '$address'.toLowerCase();
                const chainId = '0x${chainId.toString(16)}';
                
                window.ethereum = {
                    isAntigravity: true,
                    isMetaMask: true,
                    networkVersion: '${chainId}',
                    chainId: chainId,
                    selectedAddress: address,
                    
                    request: async function(payload) {
                        const readMethods = ['eth_call', 'eth_estimateGas', 'eth_gasPrice', 'eth_blockNumber', 'eth_getBalance', 'eth_getCode', 'eth_getTransactionCount', 'eth_getTransactionReceipt', 'eth_getTransactionByHash', 'eth_getLogs', 'eth_feeHistory'];
                        if (readMethods.includes(payload.method)) {
                            try {
                                const response = await fetch('${rpcUrlProvider()}', {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/json' },
                                    body: JSON.stringify({
                                        jsonrpc: '2.0',
                                        id: payload.id || Math.floor(Math.random() * 10000),
                                        method: payload.method,
                                        params: payload.params || []
                                    })
                                });
                                const data = await response.json();
                                if (data.error) throw data.error;
                                return data.result;
                            } catch (e) {
                                console.error('RPC Error:', e);
                                throw e;
                            }
                        }
                        
                        return new Promise((resolve, reject) => {
                            const id = payload.id || Math.floor(Math.random() * 1000000);
                            window.callbacks[id] = { resolve, reject };
                            window.androidWallet.postMessage(JSON.stringify({
                                method: payload.method,
                                params: payload.params,
                                id: id
                            }));
                        });
                    },
                    
                    enable: async function() {
                        return this.request({ method: 'eth_requestAccounts' });
                    },
                    
                    sendAsync: function(payload, callback) {
                        this.request(payload).then(result => {
                            callback(null, { id: payload.id, jsonrpc: '2.0', result: result });
                        }).catch(error => {
                            callback(error, null);
                        });
                    },
                    
                    send: function(payload, callback) {
                        if (typeof callback === 'function') {
                            this.sendAsync(payload, callback);
                        } else if (typeof payload === 'string') {
                            return this.request({ method: payload, params: callback || [] });
                        } else {
                            // Sync methods fallback
                            if (payload.method === 'eth_accounts') return { id: payload.id, jsonrpc: '2.0', result: [address] };
                            if (payload.method === 'eth_chainId') return { id: payload.id, jsonrpc: '2.0', result: chainId };
                            return { id: payload.id, jsonrpc: '2.0', result: null };
                        }
                    },
                    
                    on: function(event, callback) {
                        if (!this._listeners) this._listeners = {};
                        if (!this._listeners[event]) this._listeners[event] = [];
                        this._listeners[event].push(callback);
                    },
                    
                    removeListener: function(event, callback) {
                        if (this._listeners && this._listeners[event]) {
                            this._listeners[event] = this._listeners[event].filter(cb => cb !== callback);
                        }
                    },

                    emit: function(event, data) {
                        if (this._listeners && this._listeners[event]) {
                            this._listeners[event].forEach(cb => cb(data));
                        }
                    }
                };
                
                window.callbacks = {};
                
                window.onRpcResponse = function(id, result, error) {
                    if (window.callbacks[id]) {
                        if (error) window.callbacks[id].reject(error);
                        else window.callbacks[id].resolve(result);
                        delete window.callbacks[id];
                    }
                };
                
                // EIP-6963 Announce Provider
                const announceProvider = () => {
                    const info = {
                        uuid: '3506709E-7589-4D86-BB79-CE5F528D6FBD',
                        name: 'Antigravity Wallet',
                        icon: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZmlsbD0iIzAwZmYwMCIgZD0iTTEyIDJMMiAyMmgyMGwtMTAtMjB6Ii8+PC9zdmc+',
                        rdns: 'com.antigravity.wallet'
                    };
                    window.dispatchEvent(new CustomEvent('eip6963:announceProvider', {
                        detail: Object.freeze({ info: info, provider: window.ethereum })
                    }));
                };
                
                window.addEventListener('eip6963:requestProvider', announceProvider);

                setTimeout(() => {
                    announceProvider();
                    window.dispatchEvent(new Event('ethereum#initialized'));
                }, 100);
            })();
        """.trimIndent()
    }

    @JavascriptInterface
    fun postMessage(json: String) {
        try {
            val obj = JSONObject(json)
            val method = obj.getString("method")
            val id = if (!obj.isNull("id")) obj.get("id").toString() else Math.floor(Math.random() * 1000000).toLong().toString()
            val params = obj.optJSONArray("params")?.toString() ?: obj.optString("params", "[]")
            
            webView.post {
                when (method) {
                    "eth_requestAccounts", "wallet_switchEthereumChain", "eth_sendTransaction", "personal_sign", "eth_sign", "eth_signTypedData", "eth_signTypedData_v3", "eth_signTypedData_v4" -> {
                        onActionRequest(Web3Request(id, method, params))
                    }
                    "eth_accounts" -> {
                        sendResponse(id, "[\"$address\"]")
                    }
                    "eth_chainId" -> {
                        sendResponse(id, "\"0x${chainIdProvider().toString(16)}\"")
                    }
                    "net_version" -> {
                        sendResponse(id, "\"${chainIdProvider()}\"")
                    }
                    else -> {
                        // Not throwing error to avoid breaking some noisy dapps
                        sendResponse(id, "null")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendResponse(id: String, resultJson: String) {
        val safeId = if (id.toLongOrNull() != null || id.toDoubleOrNull() != null) id else "\"$id\""
        val js = "javascript:window.onRpcResponse($safeId, $resultJson, null)"
        webView.post {
            webView.evaluateJavascript(js, null)
        }
    }

    fun emitEvent(event: String, dataJson: String) {
        val js = "javascript:if(window.ethereum && window.ethereum.emit) { window.ethereum.emit('$event', $dataJson); }"
        webView.post {
            webView.evaluateJavascript(js, null)
        }
    }

    fun sendError(id: String, message: String) {
        val safeId = if (id.toLongOrNull() != null || id.toDoubleOrNull() != null) id else "\"$id\""
        val errorJson = "{\"message\": \"$message\", \"code\": 4001}"
        val js = "javascript:window.onRpcResponse($safeId, null, $errorJson)"
        webView.post {
            webView.evaluateJavascript(js, null)
        }
    }
}
