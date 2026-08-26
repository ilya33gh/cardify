<div align="center">

  <img src="app/src/main/res/drawable/ic_splash_logo.xml" width="128" height="128" alt="cardify logo" />

  # cardify

  **Современный, приватный и 100% автономный кошелек дисконтных и бонусных карт для Android.**

  [![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
  [![Android SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0%2B)-3DDC84.svg?style=flat&logo=android)](https://developer.android.com)
  [![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20M3-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
  [![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

---

## 📱 О проекте

**cardify** — это стильное приложение для Android, позволяющее оцифровать и хранить все ваши скидочные, дисконтные и клубные карты в одном месте. 

Приложение работает **100% оффлайн**, не требует регистрации, не собирает аналитику и хранит данные исключительно на устройстве пользователя.

---

## ✨ Основные возможности

- 💳 **Добавление и кастомизация карт**: сохранение номеров, названий магазинов, заметок, выбор из палитры градиентных цветов и категорий.
- ⚡ **Кассовый полноэкранный режим (POS Barcode Mode)**:
  - При клике по штрихкоду открывается полноэкранный вид, повернутый на 90°.
  - Автоматически устанавливает 100% яркость экрана для мгновенного считывания любыми сканерами на кассе.
- 📷 **Быстрый сканер штрихкодов**: интеграция **CameraX** и **Google ML Kit** для моментального сканирования любых 1D и 2D кодов (EAN-13, EAN-8, QR Code, Code 128, ITF, Code 39 и др.).
- 🏷️ **Кастомные и системные категории**: фильтрация карт с помощью интерактивных чипов категорий (Супермаркеты, АЗС, Рестораны, Одежда, Электроника и др.).
- 🔒 **Безопасность и приватность**:
  - Блокировка приложения через **Biometric API** (отпечаток пальца / Face Unlock) с настраиваемым тайм-аутом.
  - Защита от снимков экрана (`FLAG_SECURE`) в меню недавних приложений.
- 💾 **Резервное копирование**: локальный экспорт и импорт всей базы карт в формате JSON.
- 🎨 **Material 3 & Material You**:
  - Динамическая цветовая гамма, поддержка тёмной и светлой темы.
  - Плавные пружинные анимации (Spring Physics) и отклик (Haptic Feedback).
- 🚀 **Анимированный Splash Screen**: нативный стартовый экран Android 12+ с упругой векторной анимацией исчезновения.

---

## 🛠 Стек технологий

- **Язык**: [Kotlin 2.0](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3 Expressive Design)
- **Архитектура**: Clean Architecture + MVVM (StateFlow, Coroutines)
- **База данных**: [Room ORM](https://developer.android.com/training/data-storage/room) (SQLite)
- **Камера и Штрихкоды**:
  - [CameraX](https://developer.android.com/training/camerax) — управление камерой
  - [Google ML Kit Barcode Scanning](https://developers.google.com/ml-kit/vision/barcode-scanning) — распознавание штрихкодов
  - [ZXing Core](https://github.com/zxing/zxing) — векторная генерация штрихкодов
- **Загрузка изображений**: [Coil Compose](https://coil-kt.github.io/coil/)
- **Безопасность**: AndroidX Biometric
- **Сборщик**: Gradle (KSP, Version Catalogs `libs.versions.toml`)

---

## 🚀 Инструкция по сборке и запуску

### Требования:
- **Android Studio**: Ladybug (2024.2.1) или новее
- **JDK**: 21
- **Android SDK**: Compile SDK 35, Min SDK 26 (Android 8.0+)

### Шаги по установке:

1. **Клонируйте репозиторий**:
   ```bash
   git clone https://github.com/your-username/cardify.git
   cd cardify
   ```

2. **Настройте локальную конфигурацию**:
   Скопируйте `local.properties.example` в `local.properties` и укажите путь к Android SDK (если необходимо):
   ```bash
   cp local.properties.example local.properties
   ```

3. **Соберите проект**:
   - **Linux / macOS**:
     ```bash
     ./gradlew assembleDebug
     ```
   - **Windows (PowerShell / CMD)**:
     ```cmd
     .\gradlew.bat assembleDebug
     ```

4. **Установка на устройство или эмулятор**:
   ```bash
   ./gradlew installDebug
   ```

---

## 📋 Чек-лист перед публикацией на GitHub

1. ✅ **Проверка `.gitignore`**: убедитесь, что сборка `build/`, папка `.idea/` и `local.properties` не попадают в коммиты.
2. ✅ **Отсутствие приватных данных**: убедитесь, что в репозитории нет личных API-ключей или keystore файлов от подписи релизов.
3. 📄 **Лицензия**: добавьте файл `LICENSE` (например, MIT или Apache 2.0).

---

## 📄 Лицензия

Проект распространяется под лицензией **MIT**. Подробности в файле [LICENSE](LICENSE).
