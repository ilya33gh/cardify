<div align="center">

  # 💳 cardify

  **A modern, private, and 100% offline loyalty & discount card wallet for Android.**

  [![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
  [![Android SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0%2B)-3DDC84.svg?style=flat&logo=android)](https://developer.android.com)
  [![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20M3-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
  [![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

---

## 📱 About

**cardify** is a sleek, privacy-first Android application designed to digitize and store all your loyalty cards, discount cards, and membership passes in one place.

Built with modern Android development standards, **cardify** is **100% offline**, requires no account registration, tracks zero analytics, and keeps all your data strictly on your device.

---

## ✨ Features

- 💳 **Card Customization & Organization**: Easily add and personalize cards with store names, custom color palettes, notes, and categories.
- ⚡ **POS Barcode Mode (Fullscreen Checkout View)**:
  - Tapping any barcode opens a 90°-rotated, full-screen vertical barcode view.
  - Automatically overrides screen brightness to **100%** for instant, hassle-free scanning at store checkouts.
- 📷 **Real-time Camera Scanner**: Integrated with **CameraX** and **Google ML Kit** for fast detection of all 1D and 2D barcodes (EAN-13, EAN-8, QR Code, Code 128, ITF, Code 39, etc.).
- 🏷️ **Custom & Preset Categories**: Organize cards with interactive filter chips (Supermarkets, Gas Stations, Restaurants, Clothing, Electronics, Custom user categories).
- 🔒 **Privacy & Biometric Protection**:
  - Secure the app using **Biometric API** (Fingerprint / Face Unlock) with customizable inactivity lock timeouts.
  - Optional **FLAG_SECURE** window protection to prevent screenshots and app switcher previews.
- 💾 **Local Data Backup & Restore**: Export and import your card database to/from JSON files locally without third-party servers.
- 🎨 **Material 3 Expressive UI**:
  - Dynamic Color (Material You) support, full Light/Dark mode, spring-based animations, and crisp haptic feedback.
- 🚀 **Animated Splash Screen**: Native Android 12+ SplashScreen with a smooth vector scale & fade exit transition.

---

## 🛠 Tech Stack

- **Language**: [Kotlin 2.0](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 Expressive Design
- **Architecture**: Clean Architecture / MVVM (StateFlow, Kotlin Coroutines)
- **Local Storage**: [Room ORM](https://developer.android.com/training/data-storage/room) (SQLite)
- **Camera & Barcode Processing**:
  - [CameraX](https://developer.android.com/training/camerax) — Camera management
  - [Google ML Kit Barcode Scanning](https://developers.google.com/ml-kit/vision/barcode-scanning) — Real-time barcode detection
  - [ZXing Core](https://github.com/zxing/zxing) — High-precision barcode image rendering
- **Image Loading**: [Coil Compose](https://coil-kt.github.io/coil/)
- **Security**: AndroidX Biometric + Window FLAG_SECURE
- **Build System**: Gradle with Kotlin DSL, KSP, and Version Catalogs (`libs.versions.toml`)

---

## 🚀 Building & Running

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1) or newer
- **JDK**: 21
- **Android SDK**: Compile SDK 35, Min SDK 26 (Android 8.0+)

### Setup Instructions

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/cardify.git
   cd cardify
   ```

2. **Configure Local Environment**:
   Copy `local.properties.example` to `local.properties` and verify your Android SDK path:
   ```bash
   cp local.properties.example local.properties
   ```

3. **Build the Debug APK**:
   - **macOS / Linux**:
     ```bash
     ./gradlew assembleDebug
     ```
   - **Windows (PowerShell / CMD)**:
     ```cmd
     .\gradlew.bat assembleDebug
     ```

4. **Install on connected device or emulator**:
   ```bash
   ./gradlew installDebug
   ```

---


## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
