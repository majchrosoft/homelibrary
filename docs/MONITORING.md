# Monitoring & Logging

## Client-side logging

[Napier](https://github.com/AAkira/Napier) is wired in all three hosts (`HomeLibraryApplication.onCreate`, iOS `iosAppApp.init`, Web `main()`). Use it everywhere instead of `println` / `console.log`:

```kotlin
import io.github.aakira.napier.Napier
Napier.d { "User signed in: ${user.id}" }
Napier.e(throwable) { "Failed to fetch library" }
```

In debug builds Napier prints to logcat / Xcode console / browser console. In release builds the Android `Antilog` is *not* installed by default — wire **Firebase Crashlytics** by adding the Crashlytics Antilog. (Left out of the scaffold so first-run isn't blocked on Crashlytics setup.)

## Crash reporting

| Platform | Tool                                  |
|----------|---------------------------------------|
| Android  | Firebase Crashlytics (recommended)    |
| iOS      | Firebase Crashlytics                  |
| Web      | Sentry / Firebase Crashlytics for Web |

To add Crashlytics: enable in Firebase Console → drop the SDK as described in
its docs → install the appropriate Napier `Antilog` so logs flow into reports.

## Firebase observability

Realtime Database has a real-time usage dashboard in the Firebase Console
(*Database → Usage*). For long-term metrics, enable BigQuery export.

## Performance

- Compose Multiplatform: enable [`tracing-perfetto`](https://developer.android.com/jetpack/androidx/releases/tracing) in debug builds.
- Web: use the standard Chrome DevTools Performance tab — Wasm symbols are preserved when compiled with `-Pkotlin.wasm.debug=true`.
- iOS: SwiftUI has its own Instruments template — `Time Profiler` + `Hangs`.

## Alerting

Configure the following in Firebase / Play Console / App Store Connect:

- **Firebase budget alert** at 80% of the monthly cap.
- **Play Store alerting** for ANR / crash rate regressions ≥ 0.5%.
- **App Store Connect → Crashes** weekly digest email.
