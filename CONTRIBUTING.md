# Contributing to Rustgram

We welcome contributions from developers interested in high-performance Rust mobile development, Telegram client protocols, and modern Android UI!

## 📐 Architecture Principles

Rustgram is architected around a strict boundary:
1. **Kotlin (Jetpack Compose)**: Exclusively owns UI rendering, lifecycle, user touch interactions, and Android hardware hooks. It delegates all domain logic, state, and network communication to Rust.
2. **Rust Core (`crates/tg_core`)**: Owns state stores, asynchronous network interaction, encryption, and TDLib lifecycle.
3. **Rust Bridge (`apps/telegram_mobile`)**: JNI bridge exposing thread-safe functions to Kotlin (`NativeBridge.kt`).

### Design Guidelines (Material 3 Expressive)
- Use `MotionSprings` for animations:
  - Spatial changes (size, offset, scale) use spring damping with overshoot (`dampingRatio = 0.6f .. 0.8f`).
  - Effect changes (colors, opacity) use `Spring.DampingRatioNoBouncy`.
- Avoid hardcoded static mock lists in runtime code. All live entities should flow through TDLib updates and the reactive `LocalStore`.

## 🛠️ Setting up the Environment

1. Install Rust via `rustup` and add the Android target:
   ```bash
   rustup target add aarch64-linux-android
   ```
2. Ensure `ANDROID_HOME` and `ANDROID_NDK_HOME` are set in your environment.
3. Use Gradle with a dedicated home directory:
   ```bash
   cd android
   ./gradlew assembleDebug -g ../.gradle_home
   ```

## 🧪 Testing

- Run Rust unit tests:
  ```bash
  cargo test -p tg_core
  ```
- Run Android unit tests:
  ```bash
  cd android
  ./gradlew test -g ../.gradle_home
  ```

## 📬 Pull Request Guidelines

1. Ensure code compiles cleanly with no warnings on both Rust and Kotlin.
2. Keep commits atomic and descriptive.
3. If introducing new UI components, verify both Dark and Light theme compatibility as well as Dynamic Color support.
