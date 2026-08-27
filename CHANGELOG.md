# Changelog

All notable changes to **Cardify** are documented in this file.

---

## [v1.0.0-beta.2] - 2026-08-27

### 📱 Full Adaptive & Multi-Screen Support
- **Responsive Architecture**:
  - Integrated Material 3 `WindowSizeClass` (`WindowWidthSizeClass`, `WindowHeightSizeClass`) and custom adaptive design system (`AdaptiveLayout.kt`).
  - Automatic form-factor detection for Foldables in unfolded state (Galaxy Z Fold series, Google Pixel Fold, OnePlus Open), Tablets (7"–14"), and Phones.
  - Adaptive content padding, dynamic column calculations, and responsive maximum width constraints (`responsiveContentWidth`).
- **Main Wallet Screen (`WalletScreen`)**:
  - `FULL_CARDS` mode dynamically transitions to an adaptive multi-column grid (`GridCells.Adaptive(340.dp)`) on wide screens and landscape mode, avoiding oversized stretched cards.
  - `GRID_TWO_COLUMNS` mode scales dynamically (2 columns on phones, 3 on foldables/landscape phones, 4 on wide tablets).
  - `LIST_ROWS` mode is centered with comfortable max-width bounds.
  - Collapsible top header automatically adjusts height (70dp on compact height/landscape vs 140dp on portrait) to maximize screen space.
- **Card Creator & Editor (`AddEditCardScreen`)**:
  - **Two-Pane Split Layout**: On tablets, foldables, and landscape screens, the screen divides into a sticky real-time live card preview on the left and a scrollable form on the right.
  - Form fields and chips constrained to ergonomic widths on wide devices.
- **Smart POS Fullscreen Barcode Dialog (`FullScreenBarcodeDialog`)**:
  - Automatic aspect ratio detection: 1D barcodes only rotate 90° on narrow portrait phones (`height > width * 1.15`).
  - On Foldables, Tablets, and in Landscape, 1D barcodes render horizontally across full width with high-contrast sharpness.
- **Camera Scanner (`CameraScannerScreen`)**:
  - Viewfinder reticle dynamically calculates square/landscape dimensions based on screen proportions, preventing distortion.
  - Centered bottom action bar with gallery and file import buttons.
- **Modal Sheets, Settings & Biometric Lock**:
  - `CardDetailSheet` and `SettingsScreen` centered with max-width boundaries (`620.dp` and `760.dp`).
  - `BiometricAuthLockScreen` unlock button constrained to max `380.dp`.

### 🐛 Bug Fixes & Stability
- **Category Localization & English Default**: Standardized default database categories to canonical English names (`Supermarkets`, `Clothing & Shoes`, `Pharmacy & Health`, `Gas & Auto`, `Restaurants & Cafes`, `Electronics`, `Entertainment`, `Other`). Fixed category badge localization on main wallet cards and added missing translation mapping for the "Other" / "Другое" category.
- **Package Installer Startup Crash**: Resolved an issue where launching the app directly from the system Package Installer immediately after installation caused a crash during splash screen exit.
- **Dynamic Color / Wallpaper Change Crash**: Fixed an `Activity.recreate()` crash triggered when changing system wallpapers or dynamic Monet color palettes by safely guarding `SplashScreen` exit animations and handling configuration changes smoothly.
- **Settings UI Polish**: Centered and repositioned the version badge cleanly directly under the app title.

### ✨ UI & Branding
- **Settings / About Section**:
  - Added official version badge (`v1.0.0-beta.2`).
  - Added direct one-tap link to the open-source GitHub repository ([github.com/ilya33gh/cardify](https://github.com/ilya33gh/cardify)).

---

## [v1.0.0-beta.1] - 2026-08-26

### 🚀 Initial Beta Release
- **Core Wallet**: Digitizing loyalty, membership, and discount cards with 1D & 2D barcodes.
- **POS Mode**: Instant 100% brightness override with full-screen high-contrast barcode display.
- **CameraX + ML Kit**: Real-time camera scanner supporting EAN-13, EAN-8, QR Code, Code 128, ITF, Code 39, and more.
- **Offline & Private**: 100% offline Room database, local JSON export & import.
- **Biometric Security**: Fingerprint / Face unlock and window FLAG_SECURE screenshot protection.
- **Material 3 Expressive UI**: Dynamic Color (Material You), OLED Black / Dark / Light themes, spring animations, and haptic feedback.
