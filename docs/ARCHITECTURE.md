# Architecture

## Goals

1. One codebase, three production targets — Android, iOS, Web.
2. Maximum business-logic sharing without compromising platform feel.
3. Predictable, testable state management.
4. Cheap to onboard new contributors.

## High-level diagram

```
                ┌──────────────────────────────────────────────┐
                │                shared (KMP)                  │
                │  ┌────────────────────────────────────────┐  │
                │  │  presentation/  ViewModels (MVI)       │  │
                │  └────────────────────────────────────────┘  │
                │  ┌────────────────────────────────────────┐  │
                │  │  domain/       models + repositories   │  │
                │  └────────────────────────────────────────┘  │
                │  ┌────────────────────────────────────────┐  │
                │  │  data/firebase  GitLive Firebase impls │  │
                │  └────────────────────────────────────────┘  │
                └────┬─────────────────┬────────────────┬──────┘
                     │                 │                │
              ┌──────▼──────┐    ┌─────▼──────┐  ┌──────▼─────┐
              │ Android UI  │    │   Web UI   │  │  iOS UI    │
              │  Compose    │    │  Compose   │  │  SwiftUI   │
              │ (composeApp)│    │ (composeApp│  │ (iosApp)   │
              │  androidMain│    │  wasmJsMain│  │            │
              └─────────────┘    └────────────┘  └────────────┘
```

The `shared` module is the only place new features land *first*. UI hosts are thin.

## Layer responsibilities

### `domain/`
Pure Kotlin. No Firebase, no Compose, no platform types. Models are `@Serializable` so the same types serialize for both Realtime DB and (eventually) wire formats.

### `data/firebase/`
Wraps GitLive's [Firebase Kotlin SDK](https://github.com/GitLiveApp/firebase-kotlin-sdk), which provides multiplatform bindings over the native Firebase SDKs (Android Java, iOS Obj-C, JS Web). One repository per aggregate (`AuthRepository`, `BookRepository`, `LoanRepository`).

### `presentation/`
MVI ViewModels exposing `StateFlow<State>` and accepting `Intent`s. The base `MviViewModel` carries its own `CoroutineScope` so it works identically on Android (where a screen drives `clear()` from `onCleared()`), Web (where `App.kt` drops the reference on logout), and iOS (where the Swift wrapper calls `clear()` from `deinit`).

## Why these technical choices

### Compose Multiplatform for Web (Wasm), not Compose HTML or Kotlin/JS-React
Two years ago Kotlin/JS + React was the safe pick. As of 2025–2026 the KMP community has consolidated on **Compose Multiplatform with Wasm**: it's what the official wizard at [kmp.jetbrains.com](https://kmp.jetbrains.com) defaults to, it lets us ship the *exact same Composable functions* on Android and Web, and the alpha caveats (bundle size, SEO) are tolerable for an authenticated app. Trade-offs:

| Aspect              | Compose Wasm (chosen)        | Kotlin/JS + React            | Compose HTML                  |
|---------------------|------------------------------|------------------------------|-------------------------------|
| UI sharing w/ Android | 100%                       | 0% (must rewrite)            | 0% (DOM only)                 |
| Bundle size         | Larger (~1–2MB gz)           | Small                        | Small                         |
| SEO                 | Weak (canvas)                | Strong                       | Strong                        |
| Maturity (2026)     | Stable, growing              | Mature                       | Deprecated by JetBrains       |
| Hiring & community  | Strongest momentum           | Mature ecosystem             | Shrinking                     |

For Home Library the home screen is behind auth, so SEO is a non-issue. If a public landing page is needed, ship it as static HTML in `firebase.json` rewrites and let the Wasm app live at `/app`.

### Native SwiftUI for iOS, not Compose for iOS
Compose for iOS is stable but still has rough edges around scroll inertia, accessibility, and the App Store reviewer's "is this really a native app?" sniff test. SwiftUI consuming the shared ViewModels gives us the App Store reviewability of a native app while still sharing 100% of business logic. The cost is one extra UI layer — see `iosApp/iosApp/*.swift`.

### GitLive Firebase, not direct platform SDKs
Native Firebase SDKs would force three parallel implementations. GitLive wraps all three (Android, iOS, JS) behind a single `commonMain` API surface, so `FirebaseBookRepository` is written once. For Wasm the GitLive JS interop is loaded by the Firebase JS SDK that we inject in `index.html`.

### MVI, not MVVM/MVP
MVI's single-source-of-truth state plays best with `StateFlow`-based UI on three different runtime models. It also makes ViewModels deterministic and trivial to unit-test.

### Koin, not Hilt or Kodein
Hilt is Android-only. Koin's `commonMain` API is the obvious fit and it works the same on iOS and Web. No annotation processor means faster builds.

### Realtime Database, not Firestore
The business description specifies Realtime Database, and it fits the live "is this book on loan right now?" experience better than Firestore's snapshot model. The repository interfaces don't leak this choice — swapping to Firestore later is a `data/firebase/` change only.

## Firebase data model

```
/users/{uid}                       → User profile (private)
/libraries/{uid}/items/{itemId}    → Book (owner-scoped)
/catalog/items/{itemId}            → Public projection of shareable items
                                     (written by a Cloud Function, not clients)
/loans/{loanId}                    → Loan transactions
/userLoans/{uid}/incoming/{loanId} → Index for "loans I've granted"
/userLoans/{uid}/outgoing/{loanId} → Index for "loans I've borrowed"
```

Authoritative rules in `shared/src/commonMain/kotlin/com/majchrosoft/homelibrary/data/firebase/database.rules.json`. The web/CI deployments push them via `firebase.json`.

## Testing strategy

| Layer        | Tool                       | Target           | Where it runs        |
|--------------|----------------------------|------------------|----------------------|
| domain       | `kotlin.test` + Turbine    | commonTest       | JVM on every PR      |
| presentation | `kotlin.test` + fakes      | commonTest       | JVM on every PR      |
| data         | Firebase Emulator Suite    | jvmTest          | Nightly CI (not PR)  |
| Android UI   | Compose UI test            | androidUnitTest  | On-demand            |
| iOS UI       | XCUITest via fastlane test | iosApp           | On-demand            |

The shared `BookListStateTest` in `commonTest` is the model for new tests — pure Kotlin, runs on every target.

## Module dependencies

```
composeApp ──► shared
iosApp     ──► shared (via XCFramework)
shared     ──► (no other modules)
```

`shared` may not depend on UI modules. `composeApp/iosApp` may not depend on each other.
