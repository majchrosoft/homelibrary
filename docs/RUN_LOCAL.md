# Run locally against the Firebase dev database

This is the end-to-end recipe for getting the Home Library KMP apps talking to your Firebase **dev** project on your machine. Follow it top-to-bottom the first time, then jump to "Day-to-day" for the short loop.

> **What works today (KMP rewrite):** Android, iOS.
> **Web is temporarily disabled** in the KMP project — `dev.gitlive:firebase-*:2.1.0` doesn't yet publish a wasmJs variant, and Compose Multiplatform's web target is wasmJs. Until either GitLive ships wasmJs support or we hand-roll Firebase JS SDK bindings, the production web frontend remains the legacy Angular app under `~/Documents/projects/majchrosoft/home-library`. The `wasmJs { }` blocks in `shared/build.gradle.kts` and `composeApp/build.gradle.kts` are commented out with a pointer to this paragraph.

> **Important.** This doc assumes you've already anonymized your dev DB via `tools/anonymize/anonimize_dev.js`. Don't point any of these apps at production while iterating.

---

## 0 · One-time prerequisites

| Tool             | Version  | Install                                                            |
|------------------|----------|--------------------------------------------------------------------|
| JDK              | 17       | `brew install --cask temurin@17`                                   |
| Android Studio   | Ladybug+ | Includes Android SDK + KMP plugin                                  |
| Xcode            | 15+      | App Store (iOS only)                                               |
| Node             | 20+      | `brew install node` — needed for `firebase-tools`                  |
| `firebase-tools` | latest   | `npm install -g firebase-tools && firebase login`                  |
| `xcodegen`       | latest   | `brew install xcodegen` (iOS only)                                 |
| CocoaPods        | 1.15+    | `sudo gem install cocoapods` (iOS only)                            |

Generate the Gradle wrapper jar once (intentionally not committed):

```bash
cd /Users/pawelmajchrowicz/Documents/projects/majchrosoft/homelibrary-kmp
gradle wrapper       # uses the Gradle from your system PATH (8.10+)
```

---

## 1 · Wire up the dev Firebase project

In the [Firebase Console](https://console.firebase.google.com/) for your **dev** project:

1. **Authentication → Sign-in method →** enable **Email/Password**.
2. **Realtime Database →** create the DB if it doesn't already exist; note the URL — it looks like
   `https://<PROJECT-ID>-default-rtdb.<region>.firebasedatabase.app`.
3. **Project settings → Your apps →** make sure you have an Android, an iOS, and a Web app registered. If not, click "Add app" and follow the wizard. Bundle / package IDs:
   - Android: `com.majchrosoft.homelibrary` (debug variant adds `.debug` suffix)
   - iOS: `com.majchrosoft.homelibrary`
   - Web: any nickname is fine; e.g. `home-library-web-dev`

Now download the three config files:

| Platform | Download from                                                                 | Drop it at                                              |
|----------|-------------------------------------------------------------------------------|---------------------------------------------------------|
| Android  | Project settings → Android app → `google-services.json`                       | `composeApp/google-services.json`                       |
| iOS      | Project settings → iOS app → `GoogleService-Info.plist`                       | `iosApp/iosApp/GoogleService-Info.plist`                |
| Web      | Project settings → Web app → SDK setup → Config (the JS object)               | paste into `composeApp/src/wasmJsMain/resources/firebase-config.js` (see step 2 below) |

All three are gitignored — verify with `git status` before committing.

### Web config file

Copy the example and edit:

```bash
cp composeApp/src/wasmJsMain/resources/firebase-config.example.js \
   composeApp/src/wasmJsMain/resources/firebase-config.js
```

Replace each `REPLACE_ME` with the value from Firebase Console → Project settings → Web app → SDK setup. Keep `databaseURL` set to the URL from step 1.2.

### Push the RTDB rules

The rules used by the new schema live at `shared/src/commonMain/kotlin/com/majchrosoft/homelibrary/data/firebase/database.rules.json`. Push them:

```bash
firebase use <YOUR-DEV-PROJECT-ID>
firebase deploy --only database
```

(`firebase.json` already points at the right rules file.)

---

## 2 · Run each target

Both KMP targets read from the **same** dev DB once configured above. Sign in with one of the test accounts created by `tools/anonymize/anonimize_dev.js` (default password is set in that script — check the README before running it the first time).

### Web (KMP build) — disabled for now

The `wasmJs { }` target blocks are commented out (see the banner at the top of this file). For browser access while we wait on GitLive Firebase wasmJs support, run the legacy Angular app:

```bash
cd ~/Documents/projects/majchrosoft/home-library
npm install
npm start
```

### Android

```bash
./gradlew :composeApp:installDebug
adb shell am start -n com.majchrosoft.homelibrary.debug/com.majchrosoft.homelibrary.MainActivity
```

Or, simpler: open the project in Android Studio and hit **Run ▶**.

The `applicationIdSuffix = ".debug"` keeps the dev build separate from a future production build on the same device. Make sure your debug variant is registered in Firebase as `com.majchrosoft.homelibrary.debug` if you hit auth errors complaining about an unknown package.

### iOS (SwiftUI + shared XCFramework)

```bash
cd iosApp
xcodegen generate
pod install
open iosApp.xcworkspace
```

Then in Xcode:

1. Select the **iosApp** scheme + an iOS Simulator (e.g. iPhone 15).
2. Hit **Run ▶**. The first build runs `./gradlew :shared:assembleSharedReleaseXCFramework` automatically (see `project.yml` `preBuildScripts`) — give it a few minutes the first cold build.

If the build complains about `GoogleService-Info.plist not found`, double-check it sits at `iosApp/iosApp/GoogleService-Info.plist` and is included in the iosApp target sources (xcodegen handles this — re-run `xcodegen generate` after dropping the file in).

---

## 3 · Day-to-day loop

```bash
# pure-Kotlin tests run in seconds
./gradlew :shared:jvmTest

# format + static analysis before pushing
./gradlew ktlintFormat detekt

# spin up whichever target you're iterating on
./gradlew :composeApp:installDebug                      # android
cd iosApp && xcodegen generate && open iosApp.xcworkspace # ios
# Web target is currently the legacy Angular app — see the banner up top.
```

The shared module is the single source of truth — change a model in `shared/`, all three apps pick it up.

---

## 4 · Sanity check the connection

Sign in on Android and iOS and look for the same items / bookcases. They should match what you see in Firebase Console → Realtime Database → `users/<your-uid>/...`. If the list is empty:

- Confirm the user UID in the auth tab matches a top-level key under `users/` in the DB.
- Confirm the rules deploy (`firebase deploy --only database`) succeeded.
- Open the browser dev tools (web) / Logcat (Android) / Xcode console (iOS) and look for permission-denied messages — those usually mean a stale rules deploy, not a code bug.

---

## 5 · Common errors

**`Default FirebaseApp is not initialized` on Android.** `google-services.json` is missing or in the wrong folder. It must sit at `composeApp/google-services.json` (not the project root).

**`No matching client found for package name 'com.majchrosoft.homelibrary.debug'`.** Add a *new* Android app in Firebase Console with that exact package name, download a fresh `google-services.json`, and replace the file. The original `google-services.json` only had the production package.

**iOS: `Module 'Shared' not found`.** The XCFramework didn't build. Run `./gradlew :shared:assembleSharedDebugXCFramework --info` from the repo root to see the underlying error.

**`Could not resolve dev.gitlive:firebase-*:2.1.0` / "No matching variant of dev.gitlive:firebase-*".** You re-enabled the `wasmJs { }` block before GitLive ships wasm support — see the banner at the top of this file.
