# Saku V2 — UI/UX Design Specification & Saku V1 Inspiration Audit

> **Scope:** Complete design token reference, screen-by-screen UI breakdown for **Saku V2** (Android Jetpack Compose, Mineral Alabaster / Smoked Obsidian + Kyant Liquid Glass), and visual inspiration audit of **Saku V1** (Buku Tabungan & V1.6.8 Beranda Baru).
> **Canvas Target:** Mobile portrait `390 × 844 dp` (360–430 dp responsive), Indonesian locale (`id-ID`), Rupiah formatting (`Rp 15.680.000`, tabular numerals `tnum`).

---

## 1. Saku V2 Design Tokens

### 1.1 Color Palette — Light ("Mineral Alabaster") vs Dark ("Smoked Obsidian")

| Token | Light Mode (`Mineral Alabaster`) | Dark Mode (`Smoked Obsidian`) | Usage |
| :--- | :--- | :--- | :--- |
| `canvasGradientStart` | `#F5F3EE` | `#101214` | Top of vertical aurora canvas |
| `canvasGradientMid` | `#EDEAE3` | `#16191D` | Mid canvas / segmented track / input fills |
| `canvasGradientEnd` | `#E6EBE7` | `#131817` | Bottom of vertical aurora canvas |
| `poolSage` | `#D8E4DE` @ `42%` | `#1B2D2A` @ `55%` | Aurora radial blob 1: center `(16% W, 14% H)`, radius `72% W` |
| `poolLimestone` | `#EAE0D3` @ `38%` | `#26201B` @ `42%` | Aurora radial blob 2: center `(88% W, 42% H)`, radius `68% W` |
| `poolSlate` | `#DCE3EC` @ `34%` | `#1B232F` @ `45%` | Aurora radial blob 3: center `(24% W, 82% H)`, radius `75% W` |
| `glassSurface` | `#FFFFFF` @ `60%` | `#1E2227` @ `58%` | Live RenderEffect/Lens glass card & dock fill |
| `glassSurfaceFallback` | `#FFFFFF` @ `84%` | `#1E2227` @ `86%` | Non-lens fallback / search field / chips / icon pills |
| `glassTopRim` | `#FFFFFF` @ `82%` | `#FFFFFF` @ `14%` | Top stop of vertical `0.75dp` specular rim gradient |
| `glassBottomHairline` | `#1A1D1F` @ `6%` | `#000000` @ `35%` | Bottom stop of vertical specular rim gradient |
| `sheetSurface` | `#FAF9F6` | `#181B1F` | ModalBottomSheet, AlertDialog, Snackbar, nested tiles |
| `cardBorderSubtle` | `#1A1D1F` @ `8%` | `#FFFFFF` @ `9%` | Dividers, progress tracks, input & pill borders |
| `textPrimary` | `#16181A` | `#F1EFEA` | Primary headings, balances, titles |
| `textSecondary` | `#5A5F66` | `#9CA1AA` | Labels, subtitles, dates, inactive tabs |
| `textMuted` | `#8A8F98` | `#696E78` | Placeholders, timestamps, chevrons, disabled |
| `primary` | `#1E4D45` (Deep Forest Pine) | `#6FB09C` (Lifted Sage) | Primary CTA buttons, active tab tint, active filters |
| `primaryContainer` | `#E2EDE9` | `#1C332D` | Selected filter chips, Quick-Log banner, icon tile bg |
| `onPrimary` | `#FAF9F6` | `#0F1B18` | Text & icons on primary buttons |
| `income` | `#2A6B57` (Muted Jade) | `#6EB396` | Income amounts, savings progress, synced status |
| `incomeContainer` | `#E1EFE9` | `#1B332A` | Positive cashflow pill, linked savings badge, Drive connected |
| `expense` | `#B8563F` (Warm Terracotta) | `#D67B65` | Expense amounts, over-budget bar, destructive actions |
| `expenseContainer` | `#F6E6E2` | `#38231E` | Negative cashflow pill, expense type badge |
| `transfer` | `#3D5473` (Architectural Slate) | `#7D9AC0` | Transfer amounts, Pindah action icon, info icon |
| `transferContainer` | `#E6ECF3` | `#1E2A3A` | Transfer type badge, ledger sync info badge |
| `warning` | `#A87633` (Muted Ochre) | `#CFA262` | 70–89% budget warning, permission needed badge |
| `warningContainer` | `#F4ECE0` | `#332B1D` | Warning status pill, budget icon tile bg |

**Editorial Mineral Swatches (Accounts, Categories, Savings Goals):**
`#1E4D45` (Deep Forest Pine), `#2A6B57` (Muted Jade), `#3D5473` (Architectural Slate), `#B8563F` (Warm Terracotta), `#A87633` (Muted Ochre), `#544B73` (Mineral Plum), `#6B5B4D` (Warm Umber), `#5C6B5E` (Sage Olive).

### 1.2 Typography Scale (`Plus Jakarta Sans`)
All numeric amounts, percentages, dates, and times use `fontFeatureSettings = "tnum"` (tabular numerals).

| Style Token | Weight | Size | Line Height | Tracking | Typical Usage |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `displayLarge` | ExtraBold (800) | `34sp` (`32sp` hero override) | `40sp` | `-0.8sp` | Beranda Hero Total Saldo |
| `displayMedium` | Bold (700) | `28sp` | `34sp` | `-0.5sp` | Nabung Total Tabungan, Detail Tx Nominal |
| `headlineLarge` | Bold/ExtraBold (700/800) | `24sp` | `30sp` | `-0.4sp` | Screen Titles ("Saku", "Transaksi", "Nabung", "Pengaturan") |
| `headlineMedium` | SemiBold (600) | `20sp` | `26sp` | `-0.25sp` | Sheet Titles ("Catat Transaksi"), Nominal Input |
| `titleLarge` | SemiBold (600) | `18sp` | `24sp` | `-0.2sp` | Bento primary figures, Goal saved amount, Sheet headers |
| `titleMedium` | SemiBold (600) | `16sp` | `22sp` | `-0.1sp` | Card section titles, Month switcher |
| `titleSmall` | Medium (500) | `14sp` | `20sp` | `0sp` | Transaction title, Settings row title, Account name |
| `bodyLarge` | Normal (400) | `15sp` | `22sp` | `0sp` | Full note in Detail Sheet |
| `bodyMedium` | Normal (400) | `13.5sp` | `19sp` | `0sp` | Search placeholder, Category row name, Dialog body |
| `bodySmall` | Normal (400) | `12sp` | `16sp` | `0sp` | Subtitles, header date, tx metadata, helper copy |
| `labelLarge` | SemiBold (600) | `13sp` | `18sp` | `+0.1sp` | Button labels, filter pills, form labels, badges |
| `labelMedium` | Medium (500) | `11.5sp` | `15sp` | `+0.2sp` | Eyebrows ("TOTAL SALDO", "TAMPILAN & PRIVASI"), % pills |
| `labelSmall` | Medium (500) | `10.5sp` | `14sp` | `+0.25sp` | Dock tab labels, timestamps, account chip names, footer |

### 1.3 Corner Radii, Spacing & Surfaces
- **Screen horizontal margin:** `20dp` | **Top padding:** `statusBars + 14dp` | **Bottom scroll clearance:** `124dp` (above floating dock).
- **Section vertical gap:** `16dp` (Beranda, Nabung, Pengaturan) / `14dp` (Transaksi).
- **Corner Radii:**
  - Hero Glass Card: `28dp` (`padding: 20dp`)
  - Standard Glass Card / Bento / Goal Card: `24dp` (`padding: 16–18dp`)
  - Compact Glass Card (Month switcher, Date group, Quick-Log banner): `20–22dp`
  - Inputs / Account rows / Segmented containers: `16–18dp`
  - Nested tiles / Filter chips / Quick nominal chips: `12–14dp`
  - Pills / Buttons / Progress bars / Floating Dock: `Capsule (999dp / 50%)`
- **LiquidGlassCard:** `vibrancy()` + `blur(16dp)` + `lens(10dp, 20dp)`, `Highlight(0.75dp, alpha 0.70 light / 0.24 dark)`, `Shadow(radius 18dp, black 6% light / 26% dark)`, `0.75dp` vertical rim border (`glassTopRim → glassBottomHairline`).
- **LiquidGlassButton:** Capsule shape, height `48dp` (or `44dp` compact), `horizontalPadding: 16dp`, icon-text gap `8dp`. Primary fills with `colors.primary` (`alpha 0.78` + Hue blend) & `onPrimary` text; Secondary uses `glassSurface` & `textPrimary` text.
- **LiquidGlassToggle:** Track `64 × 28dp` capsule (`#1A1D1F @ 16%` light / `#FFFFFF @ 18%` dark → `colors.primary` when on), Thumb `40 × 24dp` capsule (`#FFFFFF` light / `#F1EFEA` dark) with `4dp` shadow.

### 1.4 Floating Dock (`LiquidBottomTabs`) Specification
- **Placement:** `Alignment.BottomCenter`, `navigationBarsPadding()`, `padding(horizontal = 20.dp, vertical = 14.dp)` (width `350dp` on 390dp screen).
- **Container:** Full `Capsule`, height `64dp`, inner padding `4dp`. `blur(10dp)` + `lens(20dp, 24dp)`, `Highlight(0.75dp, 0.75 light / 0.25 dark)`, `Shadow(20dp, black 10% light / 34% dark)`, surface `glassSurface`.
- **Active Lens Pill:** `Capsule`, height `56dp`, width `1/4` of dock (`~85.5dp`), tinted `colors.primary` @ `8%` light / `16%` dark (`12%/20%` fallback with `0.75dp` border). Supports horizontal scrub drag with spring physics + chromatic aberration lens on press.
- **4 Tabs (exact Indonesian labels & Material Rounded icons):**
  1. `Beranda` — `Icons.Rounded.Home` (`22dp`)
  2. `Transaksi` — `Icons.AutoMirrored.Rounded.ReceiptLong` (`22dp`)
  3. `Nabung` — `Icons.Rounded.Savings` (`22dp`)
  4. `Pengaturan` — `Icons.Rounded.Settings` (`22dp`)
  - Label: `10.5sp` Plus Jakarta Sans (`SemiBold` active / `Medium` inactive), `2dp` vertical gap from icon. Active tint: `colors.primary`; Inactive: `colors.textSecondary`.

---

## 2. Saku V2 Screen-by-Screen UI Breakdown

**Realistic Sample Data Baseline (Indonesian Rupiah):**
- **Total Saldo:** `Rp 18.450.000` | **Cashflow Bulan Ini:** `+Rp 5,2 jt` (Masuk `Rp 12,5 jt`, Keluar `Rp 7,3 jt`, Budget `Rp 8,5 jt` — 86% warning amber or 68% pine)
- **Active Accounts (3):** `BCA` (`Rp 14.200.000`, Slate `#3D5473`), `GoPay` (`Rp 2.850.000`, Jade `#2A6B57`), `Tunai` (`Rp 1.400.000`, Pine `#1E4D45`)
- **Savings Goals (2):** `Dana Darurat` (`Rp 12.500.000 / Rp 20.000.000` — 63%, `Target 31 Des 2026`), `Liburan Kyoto` (`Rp 4.500.000 / Rp 15.000.000` — 30%, with cover photo)
- **Sample Transactions:**
  - `Kopi Kenangan` (`Makan & Minum • GoPay`, `-Rp 42.000`, `Hari Ini • 09:15`, receipt icon)
  - `Setor Dana Darurat` (`Tabungan • BCA`, `-Rp 1.500.000`, `Hari Ini • 08:00`, linked pill `Dana Darurat`)
  - `Gaji Bulanan` (`Gaji • BCA`, `+Rp 12.500.000`, `25 Nov`, green income)
  - `Top Up GoPay` (`BCA → GoPay`, `Rp 1.000.000`, `24 Nov`, slate transfer)
  - `Belanja Supermarket` (`Belanja • BCA`, `-Rp 485.000`, `23 Nov`, receipt thumb)

### 2.1 Beranda (`BerandaScreen.kt`) — Light & Dark
1. **Editorial Header:** Left column: `Saku` (`24sp` ExtraBold) + date `Jumat, 27 Nov 2026` (`12sp` secondary). Right row (`8dp` gap):
   - **Drive Sync Pill:** Capsule (`glassSurfaceFallback`, `0.75dp` border), `CloudDone` (`income`) + `Tersinkron` (or `CloudOff` + `Drive`).
   - **Privacy Toggle:** `38dp` circle button with `Visibility` / `VisibilityOff` icon.
2. **Hero Liquid Glass Balance Card (`28dp` radius, `20dp` padding):**
   - Top row: `TOTAL SALDO` (`11.5sp`, `0.8sp` tracking) + Cashflow pill (`Cashflow +Rp 5,2 jt`, `incomeContainer` bg / `income` text).
   - Main figure: `Rp 18.450.000` (`32sp` ExtraBold `tnum`; masked: `Rp ••••••••`).
   - `HorizontalDivider` (`cardBorderSubtle`) + horizontal `LazyRow` (`10dp` gap) of **Account Chips** (`16dp` radius, `sheetSurface @ 72%/52%`, `28dp` tinted icon badge + account name `10.5sp` + balance `13sp tnum`).
3. **3 Quick Action Liquid Glass Buttons (`8dp` gap, equal width `weight(1f)`, height `48dp`):**
   - `↑ Keluar` (`expense` arrow), `↓ Masuk` (`income` arrow), `⇄ Pindah` (`transfer` icon). All secondary glass pills with `textPrimary` `12.5sp` label.
4. **Contextual Quick-Log Banner (conditional, `22dp` radius, `primaryContainer` glass tint):**
   - Header: `NotificationsActive` + `Quick-Log • GoPay` + `×` dismiss icon.
   - Body: `Rp 45.000` (`18sp tnum`) + `Pembayaran QRIS Kopi Kenangan` + primary `Catat` glass button.
5. **2-Column Asymmetric Bento Row (`12dp` gap, equal height via `IntrinsicSize.Max`, `24dp` radius, `16dp` padding):**
   - **Left Bento — `Cashflow & Budget`:** Monthly expense `Rp 7,3 jt` (`18sp`), `dari Rp 8,5 jt` (`10.5sp`), `6dp` capsule progress bar (`primary` <70%, `warning` 70–89%, `expense` ≥90%), bottom rows: `Masuk` (`Rp 12,5 jt` green) & `Keluar` (`Rp 7,3 jt` terracotta).
   - **Right Bento — `Progres Nabung`:** Total saved `Rp 17 jt` (`18sp` income green), `2 target aktif`, featured goal name `Dana Darurat` + `63%`, `6dp` colored progress bar, `Rp 12,5 jt / Rp 20 jt`.
6. **Transaksi Terbaru Glass Card (`24dp` radius, `18dp` padding):**
   - Header: `Transaksi Terbaru` (`16sp` SemiBold) + `Semua` pill (`primaryContainer` bg, `primary` text).
   - Up to 5 rows separated by `cardBorderSubtle` dividers: `40dp` tinted icon badge, title (`14sp` Medium) + optional receipt `Image` icon (`14dp`), subtitle (`Kategori • Akun`), right-aligned signed amount (`14sp tnum`) + time (`Hari Ini • 09:15` or `25 Nov`).
   - **Empty state:** `SakuEmptyState` with receipt icon, `Belum ada transaksi`, helper copy.

### 2.2 Transaksi (`TransaksiScreen.kt`)
1. **Header:** `Transaksi` (`24sp` ExtraBold) + right primary `LiquidGlassButton` (`+ Catat`, `48dp`).
2. **Month Switcher Glass Card (`20dp` radius):** `<` `ChevronLeft` | `November 2026` (`16sp tnum`) | `>` `ChevronRight`.
3. **Search & Filter Row:**
   - `OutlinedTextField` (`18dp` radius, `glassSurfaceFallback`): `Search` icon + placeholder `Cari catatan...`.
   - Horizontal `LazyRow` (`8dp` gap) of capsule `FilterPill`s: `Semua` (selected: `primaryContainer` + `primary` border/text), `Keluar`, `Masuk`, `Pindah`, plus `Akun ▾` dropdown pill.
4. **Ringkasan Cashflow & Budget Glass Card (`24dp` radius, `18dp` padding):**
   - Title `Ringkasan Cashflow & Budget`.
   - 3 equal columns (`14dp` radius nested tiles): `Masuk` (`income`), `Keluar` (`expense`), `Selisih` (`primary`/`expense`).
   - **Budget bar:** `Pemakaian Budget Bulanan` + `86% • Rp 8,5 jt`, `7dp` capsule bar.
   - Divider + **`Pengeluaran per Kategori`** (top 4 categories): `26dp` icon badge + name + `Rp 2,4 jt / Rp 3 jt` + `5dp` colored progress bar.
5. **Date-Grouped Transactions:**
   - Sticky-style date header row: `Hari Ini` / `Kemarin` / `25 Nov 2026` (left) + `2 transaksi` (right).
   - `LiquidGlassCard` (`22dp` radius) containing transaction rows: `40dp` badge, title, subtitle, optional linked savings pill (`Savings` icon + `Dana Darurat` in `incomeContainer`), optional `38dp` square rounded (`10dp`) receipt photo thumbnail, signed amount + `HH:mm`.
   - **Empty state:** Glass card with `Belum ada transaksi bulan ini` (or `Tidak ada hasil cocok` when filtered).

### 2.3 Nabung (`NabungScreen.kt`)
1. **Header:** Left: `Nabung` (`24sp` Bold) + subtitle `Target tabungan & alokasi impian` (`12sp`). Right: compact `44dp` primary `LiquidGlassButton` (`+ Target Baru`).
2. **Savings Hero Card (`28dp` radius, `20dp` padding):**
   - Top: `TOTAL TABUNGAN` + `%` pill (`49%` in `incomeContainer`).
   - Main figure: `Rp 17.000.000` (`28sp` Bold `tnum`) + `dari total target Rp 35.000.000`.
   - `8dp` capsule progress bar (`income` fill).
   - 2 equal stat pills (`14dp` radius): `Aktif` (`2 target`, `primaryContainer`) & `Tercapai` (`0 target`, `incomeContainer`).
3. **Savings Goal Cards (`24dp` radius, `0dp` outer padding):**
   - **Header Band:**
     - *With cover photo:* `136dp` height `AsyncImage` with dark top/bottom scrim gradient, top-left `38dp` white-on-accent icon badge, top-right `34dp` circular translucent delete chip.
     - *Without cover photo:* Horizontal subtle accent gradient band (`14dp` vertical padding) with `42dp` tinted icon badge + `34dp` delete chip.
   - **Body (`18dp` H, `16dp` V):**
     - Goal name (`16sp` SemiBold) + deadline badge (`Event` icon + `34 hari lagi • 31 Des 2026` or `CheckCircle` + `Target tercapai`) + right `%` capsule pill (`63%`).
     - Saved amount (`18sp tnum`) + `/ Rp 20.000.000` (`12sp`), `8dp` progress bar, `Kurang Rp 7.500.000` (`11.5sp` muted).
     - Action row (`10dp` gap, `44dp` height): Primary green `↓ Setor` (`tint = colors.income`) + Secondary glass `↑ Tarik` (disabled if saved == 0).
4. **Ledger Sync Footnote Card (`20dp` radius):** `32dp` `transferContainer` info tile + `Sinkronisasi Otomatis ke Ledger` + explanation text.
5. **Empty State:** Glass card with `Belum ada target tabungan` + full-width `+ Buat Target Baru` CTA.

### 2.4 Pengaturan (`PengaturanScreen.kt`)
1. **Header:** `Pengaturan` (`24sp` Bold) + `Tampilan, deteksi Quick-Log, dan pencadangan data`.
2. **Card 1 — `TAMPILAN & PRIVASI`:**
   - `Tema Mineral` + 3-segment control (`16dp` container, `12dp` active `primary` pill): `Terang` | `Gelap` | `Sistem`.
   - Divider + `Privacy Mode` row (`38dp` `primaryContainer` `VisibilityOff` tile + subtitle + `LiquidGlassToggle`).
3. **Card 2 — `KEUANGAN`:**
   - 3 `SettingsNavigationRow`s with `38dp` tinted icon tiles & right `ChevronRight`:
     - `Kelola Sumber Dana / Akun` (`3 akun aktif`)
     - `Kelola Kategori` (`11 kategori tersedia`)
     - `Budget Bulanan` (`Rp 8.500.000`)
4. **Card 3 — `DETEKSI NOTIFIKASI E-WALLET`:**
   - Eyebrow + status capsule (`Aktif` green / `Siap • Nonaktif` slate / `Perlu Izin` ochre).
   - Info row (`Quick-Log Transaksi E-Wallet & Bank`, `10 aplikasi dipantau`).
   - 2-button row (`44dp`): `Atur E-Wallet` (secondary) + `Izin Sistem` / `Beri Izin` (primary when needed).
   - Full-width secondary button (`44dp`): `▶ Uji Simulasi Quick-Log`.
5. **Card 4 — `BACKUP & GOOGLE DRIVE SYNC`:**
   - `Backup Lokal (.zip)` + 2 full-width secondary buttons (`44dp`): `Export Backup (.zip)` & `Restore dari (.zip)`.
   - Divider + `Google Drive (appDataFolder)` + `Panduan Setup OAuth` text button.
   - *Disconnected:* Info box (`CloudOff`) + full-width primary `Hubungkan Akun Google`.
   - *Connected:* `incomeContainer` email card (`user@gmail.com` + `Terakhir sinkron: 27 Nov, 19:40`) + primary `Upload Sync` + split row `Restore dari Drive` (`1.35f`) & `Putuskan` (`0.85f`, `expense` tint).
6. **Footer:** Centered `Saku • Pengelola Keuangan Pribadi` (`10.5sp` muted).

### 2.5 Four Core Modal Sheets (Surface: `colors.sheetSurface`, `20dp` horizontal padding)
1. **Tambah / Edit Transaksi (`TransactionFormSheet.kt`):**
   - Header: `Catat Transaksi` (or `Edit Transaksi`) + `×` close. Optional Quick-Log banner (`primaryContainer`).
   - 3-way type segmented pill (`16dp` radius): `Keluar` (`expense` fill) | `Masuk` (`income` fill) | `Pindah Saldo` (`transfer` fill).
   - Large `Nominal` `OutlinedTextField` (`18dp` radius, `Rp ` prefix, `20sp` Bold `tnum`, live dot-grouping) + 4 quick nominal chips (`25rb`, `50rb`, `100rb`, `250rb`).
   - Horizontal `Sumber Dana` account picker chips (plus `Ke Akun` when Transfer).
   - `Kategori` `FlowRow` chips (`14dp` radius, `24dp` icon badge + label; hidden on Transfer).
   - `Tanggal` 3-pill row: `Hari ini` | `Kemarin` | `📅 Pilih Tanggal`.
   - `Catatan (opsional)` text field + `Foto Struk (WebP)` attach card (`AddPhotoAlternate`, or `150dp` preview with `Ganti` / `×`).
   - Primary `52dp` CTA button (`18dp` radius): `✓ Simpan Transaksi` (or `Simpan Perubahan`).
2. **Detail Transaksi (`TransactionDetailSheet.kt`):**
   - Top row: Type capsule pill (`Pengeluaran` / `Pemasukan` / `Pindah Saldo`) + `×` close.
   - Large signed nominal (`28sp` Bold `tnum`, colored by type) + optional eye reveal toggle when Privacy Mode is on.
   - Optional linked savings goal banner (`Terhubung ke target tabungan: Dana Darurat`).
   - Metadata card (`20dp` radius, `canvasGradientStart` fill): `Akun`, `Kategori` (or `Alur Transfer`: `BCA → GoPay`), `Waktu` (`27 Nov 2026 • 09:15`), `Catatan`.
   - Optional `Foto Struk` (`190dp` preview with `🔍 Perbesar` pill → fullscreen dark zoom dialog).
   - Bottom action row (`50dp` height): Outlined `Hapus` (`expense` border/text, triggers confirm dialog) + Primary `Edit Transaksi` (`1.3f` weight).
3. **Nabung & Target (`SavingsSheets.kt`):**
   - **`SavingsGoalFormSheet`:** Header (`Target Tabungan Baru` / `Edit Target Tabungan`), `Nama Target`, `Nominal Target` + presets (`2,5 jt`, `5 jt`, `10 jt`, `25 jt`), `Foto Sampul (Opsional)` (`148dp` WebP picker/preview), `Tenggat Waktu (Opsional)` date row, 8-swatch `SakuMineralColorPicker`, 9-icon `SakuIconKeyPicker`, primary `Simpan Target` button (+ `Hapus Target Ini` when editing).
   - **`SavingsTransferSheet`:** Header (`Setor Tabungan` / `Tarik Tabungan` + `Dana Darurat • Terkumpul Rp 12.500.000`), ledger-link info banner (`Setor → Pengeluaran` or `Tarik → Pemasukan`), `Nominal Setoran/Penarikan` + presets (`100rb`, `250rb`, `500rb`, `1jt`/`Semua`), vertical radio list of active accounts with balances & check circle, `Catatan (Opsional)`, primary CTA (`Konfirmasi Setor` green / `Konfirmasi Tarik` slate).
4. **Kelola Dompet & Kategori (`ManagementSheets.kt`):**
   - **`AccountsManagementSheet`:** Header (`Kelola Sumber Dana`, `3 aktif • 0 diarsipkan`), primary `+ Tambah Akun / E-Wallet` (expands inline form: Nama Akun, Jenis `Tunai/E-Wallet/Bank/Lainnya`, Saldo Awal, Mineral Color Picker, Icon Picker), `AKUN AKTIF` list (`16dp` cards with `38dp` badge, name, type + balance, Edit & Archive icon buttons), `DIARSIPKAN` list.
   - **`CategoriesManagementSheet`:** Header (`Kelola Kategori`), 2-tab switch (`Pengeluaran` | `Pemasukan`), `+ Tambah Kategori` inline form (Nama, Batas Bulanan opsional, Color Picker, Icon Picker), list of category cards showing monthly limit (`Batas: Rp 1.500.000 / bln`) or `Sistem` badge, with delete icon for custom categories.
   - *(Also in `ManagementSheets.kt`: `MonthlyBudgetSheet`, `EWalletSettingsSheet` for 10 ID e-wallet/bank apps, and `DriveSyncGuideSheet`.)*

---

## 3. Saku V1 Visual Inspiration Breakdown

### 3.1 V1 Color Palette & Theming System
Saku V1 uses an editorial **"Buku Tabungan" (Bank Passbook)** paper-and-ink aesthetic with **8 PaperTones × 18 Accents (14 built-in palettes)**.

| Role | Light Kertas (Signature) | Dark Kertas | Dark Obsidian (Preview) | Dark Monokrom (V1.2 Default) |
| :--- | :--- | :--- | :--- | :--- |
| `bg` (Paper) | `#ECEFE7` (Pale green-gray) | `#121A16` | `#0B0E14` | `#0D0F0E` / `#131313` |
| `sheet` (Card/Dock) | `#F7F8F3` | `#1A241F` | `#141923` | `#171A18` / `#1D1D1C` |
| `ink` (Primary text) | `#1B2721` | `#E7EDE5` | `#ECF0F7` | `#F0F2F0` |
| `muted` / `muted2` | `#5C6A62` / `#5F6B62` | `#A3AFA5` / `#8B978D` | `#A4ACB7` / `#8C95A3` | `#A8ACA8` / `#8E928E` |
| `accent` (Cap/Stamp) | `#23418F` (Cobalt Ink) | `#8FA5F2` (Lifted Cobalt) | `#8DAEF9` | `#8C8C8A` |
| `pos` (Income/Savings) | `#39704F` | `#7FBE99` | `#7FBE99` | `#7FBE99` |
| `error` (Expense) | `#A33A2B` | `#E38A7A` | `#E38A7A` | `#E38A7A` |
| `hair` / `hairSoft` | `ink @ 10%` / `6%` | `ink @ 12%` / `7%` | `ink @ 12%` / `7%` | `ink @ 12%` / `7%` |
| `tint1..4` (Cat Ramp) | `#24332B → #71806F → #A9B4A7 → #D3DAD0` | `#E7EDE5 → #9DAE9F → #5A6B5E → #33443A` | Monochrome ink ramp | Monochrome ink ramp |

### 3.2 V1 Typography & Surface Grammar
- **Fonts:** `Archivo` (Regular/Medium/SemiBold/Bold) for all UI & V1.6.8 numbers (`tnum`); `Newsreader` (400 Serif) used in V1.2 Klasik hero/display numbers (`43sp` hero, `32sp` display).
- **Scale:** `hero 38–43sp`, `display 28–32sp`, `title 19sp`, `wordmark 18sp`, `currency 16sp`, `stat 14.5sp`, `body 14sp`, `amount 13.5sp`, `section 12.5sp`, `label 11.5sp`, `micro/tab 11sp`.
- **Surface & Glass Philosophy:**
  - **Flat paper + `1dp` hairline (`t.hair`)** inside scrollable pages (zero drop shadows, zero gradients on content cards).
  - **Controlled Overlay Liquid Glass (`Glass.kt`):** Only Dock (`blur 6dp + lens 12dp`, `sheet @ 55%`), center `+` FAB (`blur 4dp + lens 8dp`, `accent @ 92%`), and Sheets (`blur 5dp + lens 9dp`, `sheet @ 72%`) use `drawBackdrop` as siblings of `sakuLayerBackdrop`.
  - **5-Slot Capsule Dock (`64dp` height, `50%` radius):** `Home` | `Transaksi` | **Center Circular `+` Cobalt FAB** | `Nabung` | `Setting`. Custom hand-drawn `20dp` stroke `1.5dp` icons.

### 3.3 V1 Screen-by-Screen Layout (Observed from Code & Previews)
- **Beranda (Light & Dark — V1.6.8 `BerandaBaru`):**
  - Header: `Saku` left, compact `< Nov 2026 >` capsule pill + circular eye button right.
  - **3D Gyro-Parallax `TiltBalanceCard` (`24dp` radius, `t.sheet` + `1dp` hairline, AGSL foil sheen, tap-to-flip):** Top `Total saldo` / `Rincian`; Hero `Rp` (`16sp` muted) + `15.680.000` (`38sp` SemiBold); hairline divider; 3-column inline stats (`Masuk +12.500.000`, `Keluar −1.670.000`, `Selisih +10.830.000`). Flipping shows monthly averages & account breakdown.
  - **3 Quick Action Pills (`48dp` Capsule):** Solid cobalt `+ Catat` (primary) + flat `Riwayat` & `Nabung` (secondary).
  - **Sumber Dana:** Horizontal row of clean `t.sheet` tiles (`Tunai Rp 1.065.000`, `GoPay Rp 1.455.000`, `BCA ...`) with `accentWash` icon square.
  - **Spending per kategori (flat on paper, top hairline):** Segmented monochrome 4-tint horizontal bar (`6dp`) + 2×2 tabular legend (`Nabung 1.200.000`, `Tagihan 340.000`, `Transport 85.000`, `Makan 45.000`).
  - **Nabung Row (flat, top hairline):** Compact single-line goal summary (`Dana Darurat`, `1.200.000 / 15.000.000`, `4dp` green track).
  - **Transaksi terbaru:** Flat rows with `38dp` circular glyph, title + `11/11 · BCA`, right-aligned signed amount (`−340.000` red) + **running balance below (`15.680.000`)**.
  - *(V1.2 Klasik `konsep-beranda-v4.png`: Pure flat passbook sheet with zero cards, 12-month bar/line trend chart, and strict 4-column table `TGL | KETERANGAN | JUMLAH | SALDO` where the top row's running balance is stamped in cobalt.)*
- **Transaksi (V1.6.8 Preview):**
  - Header `Transaksi` + eye icon.
  - Hero `Saldo total` card (`20dp` radius) that smoothly transitions into a **sticky `58dp` compact bar** on scroll.
  - Account filter pills (`Semua` inverted `t.ink` fill, `Tunai`, `GoPay`, `BCA`).
  - **Interactive 6/12-Month Trend Card (`20dp` radius):** `< November 2026 >` + vertical bar (or line) chart (`Jul`–`Des`) with active month highlighted in cobalt (`#23418F`) and scrubbable by touch.
  - 3 derived stats row (`Rata-rata keluar Rp 417.500`, `Hari aktif 6 hari`, `Catatan 6 trx`).
  - Flat transaction list with **running balance** under every amount.
- **Tabungan / Nabung (V1.6.8 Preview):**
  - Header `Tabungan` + eye icon.
  - `Total tersimpan` card (`20dp` radius) with `4% tercapai` green pill, `Rp 1.200.000`, `4dp` green track, hairline, `Saldo tersedia` & `Bulan ini +1.200.000`.
  - `Target impian` (`2 target`): Visual goal cards (`20dp` radius) — optional `136dp` cover photo with dark gradient overlay or clean icon header, `%` pill, edit pencil, `Rp 1.200.000 / Rp 15.000.000`, `4dp` track, full-pill cobalt `Setor` + outlined `Tarik` (`48dp`).
  - Bottom collapsible card: `+ Buat target nabung baru` (`↓` chevron).
- **Atur / Pengaturan:**
  - Pure flat hairline-separated navigation rows grouped under `Tampilan` (`Tema & Kaca`, `Beranda`, `Grafik`), `Keuangan` (`Sumber Dana`, `Kategori`), `Otomatis & Pengingat` (`Notifikasi & Widget`), `Data & Cadangan` (`Cadangan ZIP`, `Hapus Data`, `Panduan Awal`), plus `Ringkasan Data` key-value ledger at bottom.

---

## 4. V1 → V2 Inspiration Notes (For Revision Reference)

1. **Signature Passbook Clarity (Running Balance & Tabular Alignment):** V1's running balance underneath/beside every transaction row and strict `tnum` alignment give instant financial auditability. V2 can borrow this subtle running-balance line in `Transaksi` without breaking its glass aesthetic.
2. **Center `+` FAB vs 4-Tab Dock:** V1's 5-slot capsule dock puts a prominent circular `+` FAB right in the thumb zone across *all* screens, reducing "log expense in <10s" friction. V2 currently uses a 4-tab `LiquidBottomTabs` dock and places `+ Catat` / `Keluar / Masuk / Pindah` inside individual screens — worth evaluating whether a persistent quick-add trigger or V2's cleaner 4-tab liquid lens dock feels better.
3. **Interactive Monthly Trend Visualization:** V1's scrubbable 6/12-month bar/line chart on `Transaksi` (and Klasik Beranda) gives strong temporal context that V2's `Transaksi` screen currently replaces with a static monthly summary + category bars.
4. **Monochrome Category Discipline vs Editorial Mineral Palette:** V1's 4-step monochrome ink ramp (`tint1..4`) prevents "rainbow fintech slop", while V2's 8 muted mineral swatches (Forest Pine, Jade, Slate, Terracotta, Ochre, Plum, Umber, Olive) achieve a similar calm restraint with richer visual differentiation on frosted glass.
5. **Tactile Hero Card & Typography Restraint:** V1's `Rp` muted prefix (`16sp`) next to the large hero number (`38–43sp`) creates cleaner typographic hierarchy than same-size `Rp 18.450.000`. Combining V1's calm typographic hierarchy and data density with V2's Mineral Alabaster / Smoked Obsidian aurora + Liquid Glass surfaces yields the strongest design direction.
