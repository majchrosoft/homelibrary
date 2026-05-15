# Local setup

## Prerequisites

| Tool                | Version              | Notes                                       |
|---------------------|----------------------|---------------------------------------------|
| JDK                 | 17                   | Temurin recommended (`brew install --cask temurin@17`) |
| Android Studio      | Ladybug+ (2024.2+)   | With KMP plugin                             |
| Xcode               | 15+                  | iOS only                                    |
| Node                | 20+                  | Only for `firebase-tools`                   |
| Ruby                | 3.2+                 | Only for fastlane (iOS releases)            |
| `xcodegen`          | latest               | `brew install xcodegen`                     |

## One-time

```bash
# 0. Generate the Gradle wrapper jar (one-off — the .jar is intentionally not
#    committed; only the `.properties` is). Requires a system Gradle 8.10+.
gradle wrapper

# 1. SDK locations
cp local.properties.example local.properties
# edit sdk.dir

# 2. Firebase configs (download from Firebase Console → Project settings)
#    DO NOT commit any of these — all are gitignored.
mv ~/Downloads/google-services.json composeApp/google-services.json
mv ~/Downloads/GoogleService-Info.plist iosApp/iosApp/GoogleService-Info.plist
cp composeApp/src/wasmJsMain/resources/firebase-config.example.js \
   composeApp/src/wasmJsMain/resources/firebase-config.js
# edit with the values from Project settings → Web app

# 3. Firebase CLI (web hosting deploys + RTDB rules deploys)
npm install -g firebase-tools
firebase login
```

## Run

| Target         | Command                                                 |
|----------------|---------------------------------------------------------|
| Common tests   | `./gradlew :shared:jvmTest`                             |
| Android (debug)| `./gradlew :composeApp:installDebug` then launch        |
| Web (dev)      | `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`     |
| iOS (Xcode)    | `cd iosApp && xcodegen generate && pod install && open iosApp.xcworkspace` |

## Day-to-day workflow

1. Branch off `develop`: `git checkout -b feat/loan-flow`
2. Write your change in `shared/` first; pure-Kotlin tests run in seconds.
3. Wire UI in `composeApp/` for Android+Web and `iosApp/` for iOS.
4. `./gradlew ktlintFormat detekt` before pushing.
5. Push & open PR → CI runs lint, tests, and three platform builds.

## Troubleshooting

**Compose Wasm fails to load with `WasmFunctionType` errors.** Bump `kotlin` and `compose` together in `gradle/libs.versions.toml` — they must be a known-good pair.

**`No matching configurations found for...`** Check that the `wasmJs` source set is named exactly `wasmJsMain`/`wasmJsTest`. The 2.x KMP toolchain renamed it from `jsMain`.

**iOS build hangs on "Build Shared XCFramework".** The first cold build downloads native dependencies; subsequent builds are cached. If it's still stuck after 10 minutes, cancel and run `./gradlew :shared:assembleSharedDebugXCFramework --info` directly to see what's happening.

**`google-services.json missing` during Android build in CI.** That's expected on PRs — CI injects a placeholder (see `.github/scripts/google-services.placeholder.json`). Real release builds inject the secret.
