# Rustgram 🦀⚡

Modern Telegram Android client crafted with **Rust**, **TDLib (MTProto 2.0)**, and **Jetpack Compose (Material 3 Expressive)**.

<p align="center">
  <img src="android/app/src/main/res/drawable/ic_launcher_foreground.xml" width="120" alt="Rustgram Logo" />
</p>

---

## 🌟 Highlights

- **Pure Rust Core**: High-performance async Tokio architecture binding directly to official Telegram TDLib v1.8.61 over JNI.
- **Material 3 Expressive UI**:
  - Full spatial & effects spring physics system (`MotionSprings`).
  - Tactile bouncy touch reactions (`expressiveBouncyClick`).
  - Geometric rounded hexagon loading indicators (`M3HexagonLoadingIndicator`).
  - Floating pill dock navigation bar (`FloatingDockBar`).
  - Morphing auth action buttons (`MorphingExpressiveButton`).
  - Segmented 5-digit OTP verification cells (`SegmentedOtpInput`).
- **Live TDLib Integration**:
  - Zero prototype mock data in runtime: all chats, messages, and profile updates stream live via TDLib `UpdateNewChat`, `UpdateNewMessage`, and async store events.
  - Automatic profile picture downloading and disk-caching via Coil.
  - Send and receive live messages with delivery status checkmarks (`✓` / `✓✓`).
- **Full Settings Suite**:
  - Appearance: Light / Dark theme toggles with full Android 12+ Material You Dynamic Color support.
  - Storage & Data: TDLib disk cache inspection and one-tap cache clearing.
  - Privacy & Security: Overview of MTProto 2.0 session encryption and 2FA management.

---

## 📱 Screenshots & Architecture

```
┌──────────────────────────────────────────────┐
│           Jetpack Compose UI                 │
│  (M3 Expressive, Springs, Coil, Floating Dock)│
└──────────────────────┬───────────────────────┘
                       │ JNI (NativeBridge.kt)
┌──────────────────────▼───────────────────────┐
│        Rust JNI Layer (apps/telegram_mobile) │
└──────────────────────┬───────────────────────┘
                       │ Tokio Channel / Rust Core
┌──────────────────────▼───────────────────────┐
│   Core Backend & Reactive Store (crates/tg_core) │
└──────────────────────┬───────────────────────┘
                       │ Native TDLib C FFI
┌──────────────────────▼───────────────────────┐
│       Official TDLib v1.8.61 (MTProto)       │
└──────────────────────────────────────────────┘
```

---

## 🛠️ Prerequisites

- **Java Development Kit (JDK)**: JDK 17
- **Rust**: Stable (toolchain target `aarch64-linux-android`)
- **Android SDK**: API level 35 (minSdk 26) + Android NDK 27+
- **Gradle**: 8.11+ (or provided `gradlew`)

---

## 🚀 Building from Source

### 1. Build Rust Native Library
```bash
cargo build -p rustgram --release --target aarch64-linux-android
```

### 2. Assemble Android APK
```bash
cd android
./gradlew assembleRelease -g ../.gradle_home
```

The release APK will be located at:
```
android/app/build/outputs/apk/release/app-release.apk
```
(and copied to root `rustgram.apk`).

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! See [`CONTRIBUTING.md`](CONTRIBUTING.md) for full setup guidelines and architecture principles.

---

## 📄 License

Dual-licensed under MIT and Apache-2.0.
