# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Homelibrary — a Kotlin Multiplatform app (one codebase, three targets: Android via Compose, iOS via SwiftUI+XCFramework, Web via Compose Wasm). Backend is Firebase (Auth, Realtime DB, Storage, Cloud Functions).

## Quick commands

```bash
# Tests (fast, JVM)
./gradlew :shared:jvmTest

# Format + static analysis
./gradlew ktlintFormat detekt

# Android
./gradlew :composeApp:assembleDebug            # build APK
./gradlew :composeApp:installDebug              # install to device/emulator

# iOS (requires xcodegen + cocoapods)
cd iosApp && xcodegen generate && pod install && open iosApp.xcworkspace

# Web (Wasm dev server on localhost:8080)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Generate Firebase config files from .env
./gradlew generateFirebaseConfig

# Release sanity
./gradlew clean check

# Tag a release (triggers CI via release-please)
git tag -a vX.Y.Z -m "..." && git push origin vX.Y.Z
```

## Architecture

```
                ┌── shared (KMP) ──────────────────────────┐
                │  domain/        models + repo interfaces  │
                │  data/firebase  Firebase implementations  │
                │  presentation/  MVI ViewModels (StateFlow)│
                │  di/            Koin modules              │
                └────┬──────────────┬───────────┬───────────┘
                      │              │           │
                ┌─────▼──┐    ┌─────▼────┐  ┌───▼────┐
                │Android │    │   Web    │  │  iOS   │
                │Compose │    │ Compose  │  │SwiftUI │
                └────────┘    └──────────┘  └────────┘
```

- `shared` is the only place new features land first. UI hosts are thin.
- `shared` uses a `nonWasmMain` source set for Firebase dependencies (GitLive Firebase SDK), because `dev.gitlive:firebase-*:2.1.0` does not publish a `wasmJs` variant. The web Wasm target cannot use Firebase directly.
- ViewModels extend `MviViewModel` (in `presentation/MviViewModel.kt`) exposing `StateFlow<State>` and accepting `Intent`s. The `clear()` lifecycle method is called per-platform (Android `onCleared`, iOS `deinit`, Web on logout).
- DI via Koin — `di/SharedModule.kt` wires repositories and ViewModels; `di/PlatformModule.kt` provides platform-specific bindings.

## Key files

| Purpose | Path |
|---|---|
| DI wiring | `shared/src/commonMain/kotlin/.../di/SharedModule.kt` |
| DI platform bindings | `shared/src/commonMain/kotlin/.../di/PlatformModule.kt` |
| MVI base | `shared/src/commonMain/kotlin/.../presentation/MviViewModel.kt` |
| Domain models | `shared/src/commonMain/kotlin/.../domain/model/*.kt` |
| Repository interfaces | `shared/src/commonMain/kotlin/.../domain/repository/*.kt` |
| ViewModels | `shared/src/commonMain/kotlin/.../presentation/*/ *ViewModel.kt` |
| Compose screens | `composeApp/src/commonMain/kotlin/.../ui/*/*Screen.kt` |
| App entry (Android) | `composeApp/src/androidMain/kotlin/.../MainActivity.kt` |
| App entry (Wasm) | `composeApp/src/wasmJsMain/kotlin/.../main.kt` |
| Firebase rules | `shared/src/commonMain/kotlin/.../data/firebase/database.rules.json` |
| Ktlint config | root `build.gradle.kts` (applied to all subprojects) |
| Detekt config | `config/detekt/detekt.yml` |

## Source set structure (shared module)

```
commonMain/          — pure Kotlin (models, DI, Koin,ktor, coroutines)
nonWasmMain/         — Firebase SDK deps (depends on commonMain)
  ├─ androidMain/    — Firebase + OkHttp Ktor engine
  ├─ iosMain/        — Firebase + Darwin Ktor engine
  ├─ jvmMain/        — Firebase (used for tests/desktop)
  └─ wasmJsMain/     — NO Firebase; JS Ktor engine only
commonTest/          — JVM tests (kotlin.test + Turbine + Koin test)
```

## Firebase data model

```
/users/{uid}                       → User profile
/libraries/{uid}/items/{itemId}    → Book (owner-scoped)
/catalog/items/{itemId}            → Public projection (Cloud Function written)
/loans/{loanId}                    → Loan transactions
/userLoans/{uid}/incoming/{loanId} → Loans I've granted
/userLoans/{uid}/outgoing/{loanId} → Loans I've borrowed
```

## Testing

- Pure-Kotlin tests in `shared/src/commonTest/` — run with `./gradlew :shared:jvmTest`. These cover domain models, ViewModels (with fake repositories), and navigation.
- `BookListState` tests in `presentation/library/LibraryStateTest.kt` are the model for new tests.
- No UI tests are committed yet (standalone Compose UI tests / XCUITest are on-demand).

## Releasing

Tagging `vX.Y.Z` on `main` triggers release-please → version bump → three release workflows:
- `release-android.yml` → AAB → Play Store (internal track)
- `release-ios.yml` → IPA → TestFlight
- `release-web.yml` → Wasm bundle → Firebase Hosting

## Development tips

- All generated Firebase config files are gitignored: `composeApp/google-services.json`, `iosApp/iosApp/GoogleService-Info.plist`, `composeApp/src/wasmJsMain/resources/firebase-config.js`. They are generated from `.env` at build time.
- The `shared` module's Wasm target is enabled but Firebase is notfunctional on it. For full web functionality, use the legacy Angular app at `~/Documents/projects/majchrosoft/home-library`.
- Before running CI release flows locally, ensure `.env` is populated; missing fields produce `REPLACE_ME` placeholders.
