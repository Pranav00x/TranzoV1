## Tranzo Android Wallet

Tranzo is a **non‑custodial, multi‑chain Android wallet** built with Jetpack Compose.

It focuses on:

- **Security first** – hardware‑backed key storage, on‑device signing, hardened UI.
- **Clean UX** – brutalist, high‑contrast design with a simple tabbed layout.
- **dApp support** – built‑in Web3 browser and WalletConnect v2 integration.

---

## Features

- **Multi‑chain wallet**
  - EVM networks: Ethereum, BNB Chain, Polygon, Base, Arbitrum, Optimism.
  - TRX / BTC address derivation for viewing balances.
  - BIP‑39 mnemonics with deterministic wallet generation.

- **Secure key management**
  - Keys and mnemonics stored using `EncryptedSharedPreferences` + `MasterKey`
    (Android Keystore / TEE).
  - Create wallet (new seed) and import via **seed phrase** or **private key**.
  - No keys or mnemonics are ever transmitted to any backend.

- **Brutalist UI**
  - 100% Jetpack Compose (no XML layouts).
  - Single‑activity navigation with Compose Navigation.
  - Bold, high‑contrast brutalist styling designed for clarity.

- **dApp Browser**
  - Bottom‑tab `Browser` route in `MainScreen`.
  - Embedded `WebView` + custom `Web3Bridge` that exposes an EIP‑1193‑style
    `window.ethereum` to dApps.
  - "Discover" screen with a curated grid of popular dApps
    (Uniswap, PancakeSwap, OpenSea, 1inch, Aave, Blur, Raydium, Lido).
  - Network chip to switch between supported chains on the fly.
  - Modal confirmations for:
    - `personal_sign` / `eth_sign`
    - `eth_signTypedData*`
    - `eth_sendTransaction`
    - `wallet_switchEthereumChain`
    - `eth_requestAccounts`

- **WalletConnect (Reown)**
  - WalletConnect v2 support through Reown.
  - Project ID is injected via `local.properties` – not hardcoded in the repo.

---

## Security Model

- **On‑device secrets only**
  - Mnemonics and private keys are encrypted with `EncryptedSharedPreferences`
    and persisted only on the device.
  - Per‑wallet and global mnemonic/private‑key storage with ability to clear.

- **UI hardening**
  - `MainActivity` sets `FLAG_SECURE`:
    - Prevents screenshots and screen recording.
    - Hides sensitive content from the recent‑apps switcher.
  - Private‑key reveal screen:
    - Shows the key on screen only.
    - **Clipboard copy is disabled** to prevent leaks via keyboards/other apps.

- **No committed secrets**
  - `local.properties` is **git‑ignored** and must **not** be committed.
  - Explorer API keys and WalletConnect Project ID are loaded via
    `buildConfigField` from `local.properties`.
  - The repo does **not** ship production keys.

- **Web3 / dApp safety**
  - `Web3Bridge` only exposes the **public address** and responds to calls that
    you explicitly approve via dialogs.
  - Signing and transaction‑sending flows always require in‑app confirmation.

> This is a **test wallet / prototype**, not a production‑audited system.
> Use small amounts and test networks when experimenting.

---

## Architecture

- **Language & stack**
  - Kotlin, Coroutines, Flow.
  - Jetpack Compose + Material3.
  - Hilt for dependency injection.
  - Room for local storage.
  - Retrofit + OkHttp for networking.
  - Web3j + BitcoinJ for crypto primitives and signing.

- **High‑level structure**
  - `data/` – repositories, network services, blockchain services.
  - `ui/` – screens (wallet, history, browser, settings, onboarding, security),
    navigation, theming.
  - `di/` – Hilt modules for databases, APIs, secure storage.

---

## Setup & Local Development

### Prerequisites

| Parameter      | Requirement                               |
| -------------- | ------------------------------------------ |
| **JDK**        | 17 (Temurin recommended)                   |
| **Android SDK**| API 34 (UpsideDownCake)                    |
| **Gradle**     | 8.4 (via wrapper / CI config)             |
| **Studio**     | Latest Android Studio with Compose tools  |

### 1. Clone the repo

```bash
git clone https://github.com/Pranav00x/TranzoV1.git
cd TranzoV1
```

### 2. Configure secrets (local only)

Create your local `local.properties` from the template:

```bash
cp local.properties.example local.properties
```

Then edit `local.properties` and fill in your own (test) keys:

```properties
etherscan.api.key=YOUR_ETHERSCAN_KEY_HERE
arbiscan.api.key=YOUR_ARBISCAN_KEY_HERE
basescan.api.key=YOUR_BASESCAN_KEY_HERE
bscscan.api.key=YOUR_BSCSCAN_KEY_HERE
polygonscan.api.key=YOUR_POLYGONSCAN_KEY_HERE
optimismscan.api.key=YOUR_OPTIMISMSCAN_KEY_HERE
walletconnect.project.id=YOUR_WALLETCONNECT_PROJECT_ID
```

> **Do not commit** this file or reuse sensitive production keys here.

### 3. Sync and build

```bash
./gradlew --refresh-dependencies
./gradlew clean assembleDebug
```

Debug APK location:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install on a device/emulator running Android 8.0 (API 26) or later.

---

## CI / CD (GitHub Actions)

The workflow in `.github/workflows/android.yml`:

- Triggers on every push to `master` and via manual `workflow_dispatch`.
- Sets up JDK 17 and Gradle 8.4.
- Runs:

```bash
./gradlew clean assembleDebug --no-daemon
```

- Copies the generated debug APK to `Tranzo-Wallet-Latest.apk`.
- Sends the APK to Telegram using `appleboy/telegram-action`.
- Uploads the APK as a GitHub Actions artifact.
- On `master`, updates a `latest` GitHub Release with the new APK.
- Sends Telegram messages with:
  - Who pushed.
  - Commit SHA + message.
  - Changed files.
  - Final build status.

---

## Contributing

This repo is currently used as a **test wallet / prototype**.

If you open a PR:

1. Keep secrets out of git (`local.properties`, keystores, real API keys).
2. Run `./gradlew clean assembleDebug` before pushing.
3. Match the existing architectural patterns (Compose, Hilt, brutalist UI).

---

## License

Licensed under the **MIT License**.
See `LICENSE` for the full text.
