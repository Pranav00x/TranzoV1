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
        val chainIdHex = "0x${chainId.toString(16)}"
        val rpcUrl = rpcUrlProvider()
        val safeAddress = address.lowercase()

        // language=JavaScript
        return """
(function() {
    'use strict';

    // Always ensure the callback registry and RPC handler exist,
    // even if ethereum was already injected (e.g. re-injection after SPA nav).
    if (!window.__tranzoCallbacks) window.__tranzoCallbacks = {};

    window.__tranzoOnRpcResponse = function(id, result, error) {
        var cb = window.__tranzoCallbacks[id];
        if (!cb) return;
        delete window.__tranzoCallbacks[id];
        if (error) cb.reject(typeof error === 'string' ? new Error(error) : error);
        else cb.resolve(result);
    };

    // If already injected, just update the chain/address and re-announce.
    if (window.ethereum && window.ethereum.isTranzo) {
        window.ethereum.chainId = '$chainIdHex';
        window.ethereum.networkVersion = String($chainId);
        window.ethereum.selectedAddress = '$safeAddress';
        window.ethereum._chainIdDec = $chainId;
        return;
    }

    var _listeners = {};
    var _address = '$safeAddress';
    var _chainId = '$chainIdHex';
    var _chainIdDec = $chainId;

    var provider = {
        isTranzo: true,
        isMetaMask: true,           // broad dApp compat
        isTrust: true,
        isTrustWallet: true,
        networkVersion: String(_chainIdDec),
        chainId: _chainId,
        selectedAddress: _address,
        _listeners: _listeners,

        request: function(payload) {
            var self = this;
            var method = payload.method;
            var params = payload.params || [];

            // ── Read-only RPC passthrough ──────────────────────────────────
            var readMethods = [
                'eth_call','eth_estimateGas','eth_gasPrice','eth_maxPriorityFeePerGas',
                'eth_blockNumber','eth_getBalance','eth_getCode','eth_getStorageAt',
                'eth_getTransactionCount','eth_getTransactionReceipt',
                'eth_getTransactionByHash','eth_getLogs','eth_feeHistory',
                'eth_getBlockByNumber','eth_getBlockByHash','eth_getBlockReceipts',
                'eth_getProof','eth_syncing','eth_protocolVersion'
            ];
            if (readMethods.indexOf(method) !== -1) {
                return fetch('$rpcUrl', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        jsonrpc: '2.0',
                        id: Math.floor(Math.random() * 1e9),
                        method: method,
                        params: params
                    })
                }).then(function(r) { return r.json(); }).then(function(d) {
                    if (d.error) { var e = new Error(d.error.message || 'RPC error'); e.code = d.error.code; throw e; }
                    return d.result;
                });
            }

            // ── Synchronous answers ───────────────────────────────────────
            if (method === 'eth_chainId')   return Promise.resolve(_chainId);
            if (method === 'net_version')   return Promise.resolve(String(_chainIdDec));
            if (method === 'eth_accounts')  return Promise.resolve(_address ? [_address] : []);
            if (method === 'eth_coinbase')  return Promise.resolve(_address || null);

            // ── Wallet action – route to Android ─────────────────────────
            return new Promise(function(resolve, reject) {
                var id = String(Math.floor(Math.random() * 1e9));
                window.__tranzoCallbacks[id] = { resolve: resolve, reject: reject };
                try {
                    window.androidWallet.postMessage(JSON.stringify({
                        method: method,
                        params: params,
                        id: id
                    }));
                } catch(e) {
                    delete window.__tranzoCallbacks[id];
                    reject(new Error('Bridge unavailable: ' + e.message));
                }
            });
        },

        // Legacy enable()
        enable: function() {
            return this.request({ method: 'eth_requestAccounts' });
        },

        // Legacy sendAsync / send
        sendAsync: function(payload, callback) {
            this.request(payload)
                .then(function(r) { callback(null, { id: payload.id, jsonrpc: '2.0', result: r }); })
                .catch(function(e) { callback(e, null); });
        },
        send: function(payload, callback) {
            if (typeof callback === 'function') {
                this.sendAsync(payload, callback);
                return;
            }
            if (typeof payload === 'string') {
                return this.request({ method: payload, params: callback || [] });
            }
            // Synchronous subset
            if (payload.method === 'eth_accounts')  return { id: payload.id, jsonrpc:'2.0', result: _address ? [_address] : [] };
            if (payload.method === 'eth_chainId')   return { id: payload.id, jsonrpc:'2.0', result: _chainId };
            if (payload.method === 'net_version')   return { id: payload.id, jsonrpc:'2.0', result: String(_chainIdDec) };
            return { id: payload.id, jsonrpc:'2.0', result: null };
        },

        on: function(event, cb) {
            if (!_listeners[event]) _listeners[event] = [];
            _listeners[event].push(cb);
            return this;
        },
        removeListener: function(event, cb) {
            if (_listeners[event]) _listeners[event] = _listeners[event].filter(function(f) { return f !== cb; });
            return this;
        },
        off: function(event, cb) { return this.removeListener(event, cb); },
        emit: function(event, data) {
            // Keep internal state in sync when events fire
            if (event === 'chainChanged' && typeof data === 'string') {
                _chainId = data;
                _chainIdDec = parseInt(data, 16);
                provider.chainId = _chainId;
                provider.networkVersion = String(_chainIdDec);
            }
            if (event === 'accountsChanged' && Array.isArray(data) && data.length > 0) {
                _address = data[0];
                provider.selectedAddress = _address;
            }
            if (_listeners[event]) _listeners[event].forEach(function(cb) { try { cb(data); } catch(_) {} });
        },
        once: function(event, cb) {
            var self = this;
            var wrapper = function(data) { self.removeListener(event, wrapper); cb(data); };
            return this.on(event, wrapper);
        },
        removeAllListeners: function(event) {
            if (event) delete _listeners[event]; else Object.keys(_listeners).forEach(function(k) { delete _listeners[k]; });
            return this;
        }
    };

    // Expose as window.ethereum
    try {
        Object.defineProperty(window, 'ethereum', {
            value: provider,
            writable: false,
            configurable: true
        });
    } catch(e) {
        window.ethereum = provider;
    }

    // Legacy window.web3 shim
    window.web3 = {
        currentProvider: provider,
        eth: {
            accounts: _address ? [_address] : [],
            defaultAccount: _address || null,
            getAccounts: function(cb) { cb(null, _address ? [_address] : []); }
        }
    };

    // EIP-6963 provider announcement
    function announceProvider() {
        var detail = Object.freeze({
            info: Object.freeze({
                uuid: '3506709E-7589-4D86-BB79-CE5F528D6FBD',
                name: 'Tranzo Wallet',
                icon: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZmlsbD0iI2ZmZiIgZD0iTTEyIDJMMiAyMmgyMGwtMTAtMjB6Ii8+PC9zdmc+',
                rdns: 'io.tranzo.wallet'
            }),
            provider: provider
        });
        window.dispatchEvent(new CustomEvent('eip6963:announceProvider', { detail: detail }));
    }

    window.addEventListener('eip6963:requestProvider', announceProvider);
    announceProvider();
    window.dispatchEvent(new Event('ethereum#initialized'));

})();
        """.trimIndent()
    }

    @JavascriptInterface
    fun postMessage(json: String) {
        try {
            val obj = JSONObject(json)
            val method = obj.getString("method")
            val rawId = if (!obj.isNull("id")) obj.get("id").toString() else System.currentTimeMillis().toString()
            val params = obj.optJSONArray("params")?.toString() ?: obj.optString("params", "[]")

            webView.post {
                when (method) {
                    "eth_requestAccounts",
                    "eth_requestPermissions",
                    "wallet_requestPermissions",
                    "wallet_switchEthereumChain",
                    "wallet_addEthereumChain",
                    "eth_sendTransaction",
                    "eth_sendRawTransaction",
                    "personal_sign",
                    "eth_sign",
                    "eth_signTypedData",
                    "eth_signTypedData_v3",
                    "eth_signTypedData_v4" -> {
                        onActionRequest(Web3Request(rawId, method, params))
                    }
                    "eth_accounts" -> sendResponse(rawId, "[\"$address\"]")
                    "eth_chainId"  -> sendResponse(rawId, "\"0x${chainIdProvider().toString(16)}\"")
                    "net_version"  -> sendResponse(rawId, "\"${chainIdProvider()}\"")
                    "eth_coinbase" -> sendResponse(rawId, "\"$address\"")
                    else           -> sendResponse(rawId, "null")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendResponse(id: String, resultJson: String) {
        val safeId = if (id.toLongOrNull() != null || id.toDoubleOrNull() != null) id else "\"$id\""
        val js = "javascript:if(window.__tranzoOnRpcResponse){window.__tranzoOnRpcResponse($safeId,$resultJson,null);}"
        webView.post { webView.evaluateJavascript(js, null) }
    }

    fun emitEvent(event: String, dataJson: String) {
        val safeEvent = event.replace("'", "\\'")
        val js = "javascript:if(window.ethereum&&window.ethereum.emit){window.ethereum.emit('$safeEvent',$dataJson);}"
        webView.post { webView.evaluateJavascript(js, null) }
    }

    fun sendError(id: String, message: String) {
        val safeId = if (id.toLongOrNull() != null || id.toDoubleOrNull() != null) id else "\"$id\""
        val safeMsg = message.replace("\"", "\\\"")
        val errorJson = "{\"message\":\"$safeMsg\",\"code\":4001}"
        val js = "javascript:if(window.__tranzoOnRpcResponse){window.__tranzoOnRpcResponse($safeId,null,$errorJson);}"
        webView.post { webView.evaluateJavascript(js, null) }
    }
}
