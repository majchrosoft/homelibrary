# iosApp — iOS (SwiftUI + shared KMP framework)

A native SwiftUI app that consumes the `Shared` XCFramework produced by the `:shared` Gradle module. Business logic, repositories, and ViewModels live in Kotlin — the iOS side is just the UI layer + a thin Combine bridge over `StateFlow`.

This README walks through running the iOS app locally against the **development Firebase project**.

## 1 · Prerequisites

| Tool           | Version  | Install                                                |
|----------------|----------|--------------------------------------------------------|
| macOS          | 14+      | Required by Xcode 15+                                  |
| Xcode          | 15+      | Mac App Store                                          |
| JDK            | 17       | `brew install --cask temurin@17`                       |
| `xcodegen`     | latest   | `brew install xcodegen`                                |
| CocoaPods      | 1.15+    | `sudo gem install cocoapods`                           |
| Ruby           | 3.2+     | macOS system Ruby is fine; only needed for `fastlane`  |
| firebase-tools | latest   | `npm install -g firebase-tools && firebase login`      |

Deployment target: **iOS 15.0** (see `iosApp/project.yml`).

## 2 · Wire up Firebase

iOS reads its Firebase config from a **`GoogleService-Info.plist`** file — there is **no `.env`** on the iOS side. The native Firebase SDK parses the plist at runtime via `FirebaseApp.configure()` (called in `iosAppApp.swift`).

### 2.1 Get the file

In the Firebase Console for the **dev** project:

1. **Project settings → Your apps → Add app → iOS**.
2. Bundle identifier: `com.majchrosoft.homelibrary`.
3. Download `GoogleService-Info.plist`.

Drop it at the exact path:

```
homelibrary-kmp/iosApp/iosApp/GoogleService-Info.plist
```

It's gitignored — verify with:

```bash
git status   # GoogleService-Info.plist must NOT appear
```

### 2.2 Development Realtime Database URL

The dev RTDB URL is **inside** `GoogleService-Info.plist` under the
`DATABASE_URL` key. It looks like:

```
https://<DEV-PROJECT-ID>-default-rtdb.<region>.firebasedatabase.app
```

That key is what the GitLive `Firebase.database` call reads on startup. If you ever need to point at a different DB without re-downloading the plist, you can override in `iosAppApp.swift`:

```swift
import FirebaseDatabase
// before IosKoinEntryKt.startKoinForIos()
Database.database(url: "https://<other-rtdb>.firebasedatabase.app")
```

### 2.3 Deploy the RTDB rules

Run this once (or whenever the rules in `shared/src/commonMain/.../database.rules.json` change):

```bash
firebase use <DEV-PROJECT-ID>
firebase deploy --only database
```

## 3 · (Optional) fastlane `.env`

Release uploads to TestFlight via fastlane read App Store Connect credentials from environment variables. For local **development** builds they are **not required**.

For the local fastlane lanes (`bundle exec fastlane beta`), drop a gitignored env file at `iosApp/fastlane/.env.local`:

```bash
# iosApp/fastlane/.env.local  — gitignored
export APP_STORE_CONNECT_KEY_ID=ABC1234DEF
export APP_STORE_CONNECT_ISSUER_ID=11111111-2222-3333-4444-555555555555
export APP_STORE_CONNECT_KEY_CONTENT="$(cat ~/secrets/AuthKey_ABC1234DEF.p8)"
export MATCH_PASSWORD=••••••••
export FASTLANE_USER=majchrosoft@gmail.com
```

```bash
cd iosApp
source fastlane/.env.local
bundle exec fastlane beta
```

CI reads the same variable names from GitHub secrets — see [`docs/RELEASING.md`](../docs/RELEASING.md).

## 4 · Generate the Xcode project

The repo intentionally does **not** commit `iosApp.xcodeproj` — it's regenerated via `xcodegen` from `project.yml` to keep the diff readable. After cloning, run:

```bash
cd iosApp
xcodegen generate
pod install
open iosApp.xcworkspace
```

Re-run `xcodegen generate` whenever you add or remove a SwiftUI file under `iosApp/iosApp/`.

## 5 · Build the shared XCFramework

The Xcode project's `preBuildScripts` triggers this automatically on first build, but if you want to do it manually:

```bash
# from the repo root
./gradlew :shared:assembleSharedReleaseXCFramework
```

The output lands at `shared/build/XCFrameworks/release/Shared.xcframework` — the same path that `project.yml` references as a framework dependency.

## 6 · Run the app

In Xcode:

1. Select the **iosApp** scheme.
2. Pick an iOS Simulator (e.g. iPhone 15 / iOS 17).
3. Hit **Run ▶**.

The first cold build takes a few minutes — Gradle compiles the shared framework. Subsequent builds reuse the cached XCFramework.

Sign in with a test account from `tools/anonymize/anonimize_dev.js`.

## 7 · What you should see

1. **Sign in / sign up** view.
2. **Library** — search bar, bookcase filter chips, list of items, "+" FAB. Top bar has icons for Shared catalog (globe), Bookcases (shelf), Profile (person).
3. Tap an item → **Item detail** (toggle borrow, share-on-catalog switch, edit, delete).
4. "+" FAB → **Item edit** (full form with type / quality picker, bookcase picker, ISBN / publisher / pages / language / cover URL / notes / shareable switch).
5. **Bookcases** view with swipe-to-delete and "+" FAB → **Bookcase edit**.
6. **Profile** view with counts + sign out.

## 8 · Day-to-day commands

```bash
# Re-generate the Xcode project after adding/removing Swift files
cd iosApp && xcodegen generate

# Pure-Kotlin tests (the same tests CI runs on every PR)
./gradlew :shared:jvmTest

# Lint
./gradlew ktlintFormat detekt
```

## 9 · Common issues

**`Module 'Shared' not found`.** The XCFramework didn't build. Run `./gradlew :shared:assembleSharedDebugXCFramework --info` from the repo root and read the underlying error.

**`GoogleService-Info.plist not found`.** Confirm it's at `iosApp/iosApp/GoogleService-Info.plist` and re-run `xcodegen generate` so the file appears in the target's sources.

**Build hangs on "Build Shared XCFramework".** First cold build downloads native deps. If it's still stuck after ~10 minutes, cancel and run the Gradle command manually to see what's going on.

**Pods step says it can't find Firebase.** GitLive pulls Firebase via Swift Package Manager through the XCFramework — the `Podfile` is intentionally minimal. If you see SwiftPM errors instead, **File → Packages → Reset Package Caches** in Xcode.

**Simulator's network requests fail.** The simulator inherits the Mac's network. Tethered hotspots with captive portals occasionally drop UDP, which Firestore's GRPC transport relies on. Switch to a real Wi-Fi or use the device.

---

For project structure and the Kotlin/Swift bridge, see [`docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md). For releases to TestFlight / App Store, see [`docs/RELEASING.md`](../docs/RELEASING.md).
