
```
    ______ __               _____ __        __    __   
   / ____// /____  _      _ / ___// /______ / /_  / /__ 
  / /_   / // __ \| | /| / /\__ \/ __/ __ ` // __ \/ // _ \
 / __/  / // /_/ /| |/ |/ /___/ / /_/ /_/ // /_/ / //  __/
/_/    /_/ \____/ |__/|__//____/\__/\__,_//_.___/_/ \___/ 
```

```bash
>> KERNEL_ID        :: FS-ANDROID-PROTOTYPE-V1
>> CRYPTO_CORE      :: WEB3J_LIGHTWEIGHT_EVM
>> ENCRYPTION_STD   :: AES-256-GCM + ANDROID_KEYSTORE_TEE
>> COMPILE_TARGET   :: SDK_34 [UBUNTU_CI_RUNNER]
```

### `// SYSTEM_MANIFESTO`

_FlowStable represents a paradigm shift in decentralized interface architecture. We discard the superfluous. We embrace the raw. It is a strictly non-custodial, high-performance EVM terminal designed for operators who demand zero-latency interaction with the distributed ledger._

---

### `// COMPONENT_ARCHITECTURE`

#### `[0x01] KEY_MANAGEMENT_SYSTEM`
_The cryptographic core leverages the **BIP-39** standard for deterministic entropy generation. Private key vectors are secured within the device's **Safezone (TEE)**, accessible only via biometric signature. No keys ever transmit over network protocols._

*   **`_algorithm`**: *ECDSA (secp256k1)*
*   **`_storage`**: *Hardware-backed Keystore containers*
*   **`_recovery`**: *12/24-word Mnemonic Phrase injection*

#### `[0x02] INTERFACE_LAYER`
_The visual stack is rendered purely in **Jetpack Compose**, bypassing legacy XML inflation for optimal frame-timing. The design language—**Neo-Brutalism**—utilizes high-contrast primitives and hard shadows to eliminate cognitive load and maximize data legibility._

*   **`_ui_framework`**: *Compose BOM 2023.08.00*
*   **`_theming`**: *Material3 dynamic color extraction*
*   **`_navigation`**: *Single-Activity architecture with Compose Navigation*

#### `[0x03] BROWSER_MODULE`
_Embedded dApp browser with Web3 injection, redesigned for clarity and safety._

*   **`_entry_point`**: *Bottom-tab `Browser` route in `MainScreen`*
*   **`_engine`**: *Android `WebView` + custom `Web3Bridge` (EIP-1193-like API)*
*   **`_features`**:
    * Home \"Discover\" grid of curated dApps (Uniswap, PancakeSwap, OpenSea, etc.)
    * Network chip with live chain switching (Ethereum, BSC, Polygon, Base, Arbitrum, Optimism, Tron, BTC)
    * Modal confirmations for `personal_sign`, `eth_signTypedData`, `eth_sendTransaction`, `wallet_switchEthereumChain`, `eth_requestAccounts`
*   **`_security`**:
    * WalletConnect and Web3 requests always require explicit in‑app approval
    * No private keys or mnemonics are injected into JS; only addresses / signatures

#### `[0x04] NETWORK_IO`
_Blockchain state synchronization is achieved through asynchronous `OkHttp` channels and robust `Retrofit` adapters. We support EIP-1193 injection for seamless dApp compatibility via a custom-optimized WebView client._

---

### `// SECURITY_POSTURE`

*   **`_key_storage`**: `EncryptedSharedPreferences` backed by `MasterKey` (Android Keystore / TEE)
*   **`_seed_handling`**:
    * Mnemonics and private keys never leave the device
    * Private-key reveal screen no longer supports clipboard copy
*   **`_ui_hardening`**:
    * `FLAG_SECURE` on `MainActivity` — prevents screenshots, screen recording, and recent‑apps previews
*   **`_secrets`**:
    * No API keys or WalletConnect IDs are baked into the repo; user must provide them via `local.properties` (see below)

---

### `// BUILD_PIPELINE`

_The deployment sequence requires a precise environment configuration. Deviations may result in compilation anomalies._

| PARAMETER | REQUIREMENT |
| :--- | :--- |
| **`JDK_VERSION`** | `17` _(Temurin Distribution Recommended)_ |
| **`ANDROID_SDK`** | `API 34` _(UpsideDownCake)_ |
| **`GRADLE`** | `8.4` _(Kotlin DSL)_ |

#### `> INITIATE_BUILD_SEQUENCE`

```bash
# 1. CLONE_REPOSITORY
$ git clone https://github.com/FlowStablee/flowstable-android-test-wallet.git

# 2. SYNC_GRADLE
$ ./gradlew --refresh-dependencies

# 3. CONFIGURE_SECRETS (LOCAL ONLY)
#    Copy template and fill in your own explorer + WalletConnect keys.
$ cp local.properties.example local.properties
#    Edit `local.properties` (do NOT commit real keys).

# 4. COMPILE_DEBUG_ARTIFACT (LOCAL)
$ ./gradlew clean assembleDebug

# CI PIPELINE (GITHUB_ACTIONS)
# GitHub Actions runs on every push to `master`:
#   - gradle clean assembleDebug --no-daemon --info --stacktrace
#   - ships debug APK to Telegram + GitHub Release (`latest`)
```

---

### `// LICENSE_PROTOCOL`

_This codebase is distributed under the **MIT License**. Permission is explicitly granted to modify, merge, publish, and distribute copies of the Software without restriction, subject to the inclusion of the original copyright notice._

```
// END_OF_TRANSMISSION
// FLOWSTABLE_LABS_SIGNATURE_VERIFIED
```
