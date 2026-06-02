# Home Library — Kotlin Multiplatform

A cross-platform home library: catalogue your books and other media, see where each item is, who has it on loan, and (optionally) lend out shareable items to other users on the platform. One Kotlin codebase, three production targets:

- **Android** — Compose Multiplatform UI, distributed via Google Play
- **iOS** — Native SwiftUI consuming the shared KMP framework, distributed via TestFlight / App Store
- **Web** — Compose Multiplatform UI (Wasm). Note: Firebase functionality is currently limited on Web due to SDK support; see [`docs/RUN_LOCAL.md`](docs/RUN_LOCAL.md) for details.

The backend is **Firebase**: Authentication, Realtime Database, Storage, and a small set of Cloud Functions (the public catalog projection lives there — not in the client).

## Repository layout

```
homelibrary-kmp/
├── shared/             # KMP module — domain, data, presentation (ViewModels)
├── composeApp/         # Compose Multiplatform UI for Android + Web (Wasm)
├── iosApp/             # SwiftUI app + fastlane + xcodegen project.yml
├── .github/workflows/  # ci.yml + release-{android,ios,web}.yml + release-please
├── docs/               # Architecture, setup, releasing
├── config/detekt/      # Detekt config
├── firebase.json       # Hosting + Realtime DB rules wiring
└── gradle/libs.versions.toml
```

## Quick start

Prerequisites: JDK 17, Android Studio Ladybug+ (or IntelliJ IDEA with KMP plugin), Xcode 15+ (iOS only), Node 20+ (web hosting CLI only).

```bash
# Clone & bootstrap
git clone <repo>
cd homelibrary-kmp
cp local.properties.example local.properties     # set sdk.dir
# Drop your Firebase configs in:
#   composeApp/google-services.json
#   iosApp/iosApp/GoogleService-Info.plist

# Run the dev loop
./gradlew :shared:jvmTest                         # fast unit tests
./gradlew :composeApp:assembleDebug               # Android APK
./gradlew :shared:assembleSharedDebugXCFramework  # iOS framework
```

**Running locally against the dev Firebase project? Per-platform recipes:**

- 🤖 **Android** → [`composeApp/README.md`](composeApp/README.md)
- 🍎 **iOS** → [`iosApp/README.md`](iosApp/README.md)
- 🌐 **Web** → [`composeApp/src/wasmJsMain/README.md`](composeApp/src/wasmJsMain/README.md) *(KMP web target is temporarily disabled — see that file for the legacy Angular fallback)*

For the cross-target overview see [`docs/RUN_LOCAL.md`](docs/RUN_LOCAL.md); for iOS signing and CI wiring, [`docs/SETUP.md`](docs/SETUP.md).

## Architecture at a glance

Clean Architecture layers, all in `shared/commonMain`:

```
domain/        ← models + repository interfaces + use cases
  ↑
data/firebase/ ← Firebase implementations of the repositories
  ↑
presentation/  ← MVI ViewModels, exposing StateFlow<State>
                  ↑
                  Android (Compose) + Web (Compose) + iOS (SwiftUI)
```

Why this split: the **only** platform-specific code is the UI host and the small `expect/actual` pieces (logging, system info). Bug fixes and feature work happen once, in `commonMain`.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for module diagrams, key technical decisions (Compose-for-Web vs Wasm, GitLive Firebase, MVI pattern), and the Firebase data model.

## Releases

Tagging `vX.Y.Z` on `main` triggers all three release workflows. We use [release-please](https://github.com/googleapis/release-please) to automate version bumps and changelogs from [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` → minor bump
- `fix:` → patch bump
- `feat!:` / `BREAKING CHANGE:` → major bump

The full release checklist (signing, store metadata, screenshots, rollouts) lives in [`docs/RELEASING.md`](docs/RELEASING.md).

## Contributing

- One branch per change; PR into `develop`. CI must pass.
- `develop` → `main` via release-please's auto-PR.
- All commits follow Conventional Commits.

## License

TBD by project owner.
