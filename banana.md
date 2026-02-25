# Wallet UI Redesign Proposals 

Based on your request for a minimalist, non-kiddish, dark-themed UI with smaller fonts and no "boxes", here are a few design directions we can take. 

## Core Aesthetic Changes (Applying to all options)
- **Default Dark Theme:** `MaterialTheme.colorScheme.background` will be set to pure black (`#000000`).
- **Typography:** Scale down all fonts. E.g., Headers go from 32sp to 24sp. Normal text goes from 16sp/14sp down to 14sp/12sp. Font weights will be reduced from `Black` to `Medium` or `Regular` for a cleaner look.
- **Borderless (No Boxes):** Remove the thick 2dp borders (`Modifier.border`) and hard drop shadows from the token list and action buttons. Tokens will be displayed as a clean, continuous list separated by subtle dividers or just white space.

---

### Design Option 1: The "Invisible" Interface (Recommended)
This is the ultimate minimalist approach. No visible containers, just raw data floating on a pure black canvas.

- **Header:** "Dashboard" in 14sp, Gray, upper-left.
- **Total Balance:** Large, thin white text (e.g., "$12,450.00" in 36sp, `FontWeight.Light`) directly on the black background. No surrounding card.
- **Actions (Send/Receive):** Plain text buttons or simple thin-lined icons floating below the balance, rather than bulky brutalist buttons.
- **Asset List:** 
  - Just rows of text. 
  - Left: "ETH" (14sp, White) and "Ethereum" (12sp, Gray).
  - Right: "$3,240.50" (14sp, White) and "1.24 ETH" (12sp, Gray).
  - Separator: A very faint, 1dp dark gray line (`#1A1A1A`) between assets.will

---

### Design Option 2: The "Glass" Interface
Still dark and minimalist, but uses subtle background shades instead of completely removing all containers.

- **Header/Balance:** Similar to Option 1, but total balance has a very slight glow or gradient.
- **Actions (Send/Receive):** Soft pill-shaped buttons with an almost transparent background (`White.copy(alpha = 0.05f)`) and no borders.
- **Asset List:** 
  - No borders, but each row has a slight padding. When tapping an asset, it highlights softly. 
  - Token icons are small (24dp) circles with a muted background.

---

### Design Option 3: Terminal / Hacker Lite
A nod to the previous style but highly refined and subdued. 

- **Colors:** Deep black background with off-white text and a single accent color (like a muted cyan or dark green).
- **Typography:** Exclusively monospace (like the splash screen), but much smaller sizes (10sp-12sp).
- **Asset List:** Formatted like a table or command-line output.
  `[ETH]   Ethereum      $3,240.50    1.24`
  No boxes at all. Just raw data alignment.

---

Let me know which direction you prefer (1, 2, or 3), or if you want to mix and match elements! Once you confirm, I will apply these changes to the `WalletScreen.kt` and `Theme.kt` files.
