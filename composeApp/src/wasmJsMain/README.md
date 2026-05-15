# Web (Compose Multiplatform + Wasm) — temporarily disabled

This directory is the **web target** for Home Library: a Kotlin/Wasm bundle that reuses the same `App` composable as the Android target, hosted in a tiny `index.html`.

> **Heads-up.** The `wasmJs { }` blocks in `shared/build.gradle.kts` and `composeApp/build.gradle.kts` are currently **commented out**. Reason: GitLive's `dev.gitlive:firebase-*:2.1.0` does not yet publish a `wasmJs` variant, so the build fails at `:kotlinNpmInstall` with `"No matching variant of dev.gitlive:firebase-* was found"`. Until either GitLive ships wasm support or we refactor Firebase off `commonMain` for the web target, **the production web frontend is the legacy Angular app** at `~/Documents/projects/majchrosoft/home-library`.
>
> Skip to [Plan B: legacy Angular app](#plan-b--run-the-legacy-angular-web-frontend) if you just need a working browser experience today.

---

## 1 · Prerequisites

| Tool           | Version  | Install                                                |
|----------------|----------|--------------------------------------------------------|
| JDK            | 17       | `brew install --cask temurin@17`                       |
| Node           | 20+      | `brew install node`                                    |
| firebase-tools | latest   | `npm install -g firebase-tools && firebase login`      |
| A modern browser | latest | Chrome / Safari / Firefox with Wasm GC support         |

## 2 · Wire up Firebase (Web)

Unlike Android and iOS, the web target has **no native Firebase SDK** under the hood — it imports the Firebase **JS SDK** at runtime. We feed the config into the page via a small JavaScript file, NOT via `.env`. Webpack would also work but adds a build step we don't need for a single-page bundle.

### 2.1 Get the web config

In the Firebase Console for the **dev** project:

1. **Project settings → Your apps → Add app → Web** (any nickname, e.g. `home-library-web-dev`).
2. **SDK setup and configuration → Config** — copy the JS object.

### 2.2 Drop it in

```bash
cp composeApp/src/wasmJsMain/resources/firebase-config.example.js \
   composeApp/src/wasmJsMain/resources/firebase-config.js
```

Edit the new file (gitignored) and replace every `REPLACE_ME` with the value from the console:

```js
// composeApp/src/wasmJsMain/resources/firebase-config.js  — gitignored
window.__FIREBASE_CONFIG__ = {
    apiKey: "AIzaSy…",
    authDomain: "<DEV-PROJECT-ID>.firebaseapp.com",
    databaseURL: "https://<DEV-PROJECT-ID>-default-rtdb.<region>.firebasedatabase.app",
    projectId: "<DEV-PROJECT-ID>",
    storageBucket: "<DEV-PROJECT-ID>.appspot.com",
    messagingSenderId: "1234567890",
    appId: "1:1234567890:web:abc123def456"
};
```

### 2.3 Development Realtime Database URL

The `databaseURL` above **is** the dev RTDB URL. Get it from **Firebase Console → Realtime Database** — it's printed at the top of the page next to the database picker:

```
https://<DEV-PROJECT-ID>-default-rtdb.<region>.firebasedatabase.app
```

Keep that URL consistent across Android, iOS and Web — otherwise sign-in works but `items` and `bookcases` appear empty because the clients are looking at different databases.

### 2.4 Deploy the RTDB rules

Once per environment:

```bash
firebase use <DEV-PROJECT-ID>
firebase deploy --only database
```

### 2.5 `.env`? Not for the browser

Browser code can't read process env vars — every value baked into a web bundle is public. The Firebase web config is **not secret** (it's gated by RTDB rules and App Check, not by hiding the keys), which is why we ship it as a plain JS file. If you want the same indirection developers expect with Node apps, a `composeApp/src/wasmJsMain/.env.example` file is acceptable but its values still have to be copied into `firebase-config.js` at build time — there's no runtime `process.env` in Wasm.

## 3 · Re-enable the wasmJs target

When you're ready to bring web back, the two source files to touch are:

1. **`shared/build.gradle.kts`** — uncomment the `wasmJs { … }` block (there's a comment with the exact rationale at the top of it). You will also need to either (a) wait for GitLive Firebase wasmJs publishing or (b) extract Firebase deps into an intermediate `nonWebMain` source set and use the Firebase **JS SDK** directly from `wasmJsMain` via `external` interop.

2. **`composeApp/build.gradle.kts`** — uncomment the `wasmJs { … }` block.

Then:

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun     # dev server with HMR
```

The dev server defaults to `http://localhost:8080`.

## 4 · Build a production bundle

```bash
./gradlew :composeApp:wasmJsBrowserDistribution
# output → composeApp/build/dist/wasmJs/productionExecutable
```

CI uploads that directory to Firebase Hosting on tagged releases — see `.github/workflows/release-web.yml` and [`docs/RELEASING.md`](../../../../docs/RELEASING.md).

## 5 · index.html

`composeApp/src/wasmJsMain/resources/index.html` boots the bundle. It expects:

- `firebase-config.js` to be served from the same directory (gitignored locally, generated from the `FIREBASE_WEB_CONFIG` GitHub secret in CI).
- `composeApp.js` — the Kotlin/Wasm output that imports the Firebase JS SDK at runtime.

If you change the host page, keep both `<script>` tags in this order — the config must be on `window` before the Wasm bundle parses it.

---

## Plan B — run the legacy Angular web frontend

The legacy Angular app **is the production web experience** until the KMP wasmJs target is re-enabled. It already points at the same dev Firebase project, so signing in there gives you exactly the same data Android and iOS see.

```bash
cd ~/Documents/projects/majchrosoft/home-library
cp src/environments/environment.dev.example.ts src/environments/environment.dev.ts
# edit src/environments/environment.dev.ts with the same Firebase web config
# values you'd put in firebase-config.js above
npm install
npm start
```

It serves at `http://localhost:4200`. See that repo's own `README.md` for full details.

## 6 · Common issues

**Browser console: `window.__FIREBASE_CONFIG__ is undefined`.** `firebase-config.js` is missing or its `<script>` tag isn't loaded before `composeApp.js`. Check `index.html`.

**`Could not resolve dev.gitlive:firebase-*:2.1.0` / `No matching variant of dev.gitlive:firebase-*`.** You re-enabled `wasmJs { … }` before GitLive shipped wasm support. Back out the change or do the `nonWebMain` source-set refactor — see [`docs/RUN_LOCAL.md`](../../../../docs/RUN_LOCAL.md).

**Bundle loads but every RTDB read returns `permission_denied`.** You forgot `firebase deploy --only database`, or the `firebase-config.js` `databaseURL` doesn't match the project where the rules were deployed.

---

For the project-wide architecture choices (Compose Wasm vs Kotlin/JS vs Compose HTML), see [`docs/ARCHITECTURE.md`](../../../../docs/ARCHITECTURE.md). For the broader local-run recipe across all three targets, see [`docs/RUN_LOCAL.md`](../../../../docs/RUN_LOCAL.md).
