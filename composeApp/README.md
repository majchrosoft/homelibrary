# composeApp — Android (and Web)

The `composeApp` module is the Compose Multiplatform host. It currently ships the **Android** target, with a **Web (Wasm)** target wired but temporarily commented out — see [Web](#web-wasmjs--temporarily-disabled) below.

This README is the single source of truth for running the Android app locally against the **development Firebase project**.

## 1 · Prerequisites

| Tool           | Version  | Install                                                |
|----------------|----------|--------------------------------------------------------|
| JDK            | 17       | `brew install --cask temurin@17`                       |
| Android Studio | Ladybug+ | Includes Android SDK 35 and the KMP plugin             |
| Android SDK    | 35       | Installed through Android Studio                       |
| Node           | 20+      | `brew install node` (only for `firebase-tools`)        |
| firebase-tools | latest   | `npm install -g firebase-tools && firebase login`      |

> `minSdk = 24`, `compileSdk = 35`, `targetSdk = 35`. The `debug` build variant adds the `.debug` applicationId suffix so it can sit next to a release build on the same device.

## 2 · Configure the SDK path

```bash
cp local.properties.example local.properties
# edit local.properties and set:
#   sdk.dir=/Users/<you>/Library/Android/sdk
```

`local.properties` is gitignored — never commit it.

## 3 · Wire up Firebase

The Android app reads its Firebase config from **`composeApp/google-services.json`** (gitignored). There is **no `.env` file** on the Android side — `applyPlugin("com.google.gms.google-services")` parses `google-services.json` at build time and codegens the credentials.

### 3.1 Get the file

In the Firebase Console for the **dev** project:

1. **Project settings → Your apps → Add app → Android**.
2. Package name: `com.majchrosoft.homelibrary` *(also add `com.majchrosoft.homelibrary.debug` as a second Android app — the debug variant uses that suffix)*.
3. Download `google-services.json`.

Drop it at the exact path:

```
homelibrary-kmp/composeApp/google-services.json
```

Verify it's ignored:

```bash
git status   # google-services.json must NOT appear in the file list
```

### 3.2 Development Realtime Database URL

The dev RTDB URL lives **inside** `google-services.json` under
`project_info.firebase_url`. It looks like:

```
https://<DEV-PROJECT-ID>-default-rtdb.<region>.firebasedatabase.app
```

If you ever need to point the build at a different DB for a one-off test, override at runtime in `composeApp/src/androidMain/kotlin/com/majchrosoft/homelibrary/HomeLibraryApplication.kt`:

```kotlin
Firebase.database(url = "https://<other-rtdb>.firebasedatabase.app")
```

…but the default flow is "drop in the dev `google-services.json` and you're done".

### 3.3 Deploy the RTDB rules

The rules used by the new KMP schema live in the shared module. Push them once per environment:

```bash
firebase use <DEV-PROJECT-ID>
firebase deploy --only database
```

`firebase.json` at the repo root already points at the right rules file.

## 4 · (Optional) Local `.env` for build secrets

Production release builds read signing keys from environment variables — used by `composeApp/build.gradle.kts` when `ANDROID_KEYSTORE_PATH` is set:

| Env var                     | What it is                            |
|-----------------------------|---------------------------------------|
| `ANDROID_KEYSTORE_PATH`     | Absolute path to a `.jks` keystore    |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password                     |
| `ANDROID_KEY_ALIAS`         | Key alias inside the keystore         |
| `ANDROID_KEY_PASSWORD`      | Password for that key                 |

For local debug builds these are **not required** — Gradle falls back to the Android SDK debug keystore. If you want them persisted across shells, drop them in a project-local `.env.local` file (gitignored) and `source` it before invoking Gradle:

```bash
# composeApp/.env.local — gitignored
export ANDROID_KEYSTORE_PATH=$HOME/.android/upload.jks
export ANDROID_KEYSTORE_PASSWORD=••••
export ANDROID_KEY_ALIAS=upload
export ANDROID_KEY_PASSWORD=••••
```

```bash
source composeApp/.env.local
./gradlew :composeApp:assembleRelease
```

## 5 · Run the app

The fastest path is to open the project in Android Studio and hit **Run ▶** with the **composeApp** configuration. From the command line:

```bash
# Install the debug build on the currently connected device or emulator
./gradlew :composeApp:installDebug

# Launch it
adb shell am start -n com.majchrosoft.homelibrary.debug/com.majchrosoft.homelibrary.MainActivity
```

Sign in with one of the test accounts seeded by `tools/anonymize/anonimize_dev.js` — the default password is set inside that script.

## 6 · What you should see

On first launch, with valid `google-services.json` and the rules deployed, you'll get:

1. **Sign in screen** — email / password (with toggle to sign-up).
2. After auth: **Library** with search, bookcase filter chips, and a floating "+" button.
3. From the Library top bar you can reach **Shared catalog** (community books), **Bookcases** (manage shelves), and **Profile** (stats + sign out).
4. Tap any item → **Item detail** (toggle borrow, toggle share-on-catalog, edit, delete).
5. Tap "+" → **Item edit** (add / edit form).

If you see a permission-denied error in Logcat, the most likely cause is that you forgot `firebase deploy --only database` after step 3.3.

## 7 · Day-to-day commands

```bash
./gradlew :shared:jvmTest                     # fast pure-Kotlin tests
./gradlew ktlintFormat detekt                 # before pushing
./gradlew :composeApp:installDebug            # rebuild + install on device
```

## 8 · Common issues

**`Default FirebaseApp is not initialized.`** `google-services.json` is missing or in the wrong place. It must sit at `composeApp/google-services.json` — not at the repo root.

**`No matching client found for package name 'com.majchrosoft.homelibrary.debug'`.** Add a *second* Android app in Firebase Console with that exact package name, download a fresh `google-services.json`, and replace the file. The original only carried the production package.

**Build error: `Plugin with id 'com.google.gms.google-services' not found`.** You're on an older AGP version; run `./gradlew --refresh-dependencies` and make sure `gradle/libs.versions.toml` matches what's checked in (`agp = "8.7.3"`).

**`gradle wrapper` complains it can't find Gradle.** The wrapper jar is intentionally not committed — install Gradle 8.10+ system-wide once (`brew install gradle`) and run `gradle wrapper` from the repo root to generate the wrapper jar.

---

## Web (wasmJs) — temporarily disabled

The Compose Multiplatform Web target is wired in source but **commented out in `composeApp/build.gradle.kts` and `shared/build.gradle.kts`** because GitLive's Firebase Kotlin bindings don't yet publish a `wasmJs` variant. See [`src/wasmJsMain/README.md`](src/wasmJsMain/README.md) for the rationale, the re-enable checklist, and how to run the legacy Angular web app in the meantime.

---

For the architectural why-this-way, see [`docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md). For releasing to the Play Store, see [`docs/RELEASING.md`](../docs/RELEASING.md).
