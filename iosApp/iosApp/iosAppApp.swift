import SwiftUI
import FirebaseCore
import Shared

@main
struct iosAppApp: App {

    init() {
        FirebaseApp.configure()
        // Bootstraps the same Koin DI container the Android & Web hosts use.
        IosKoinEntryKt.startKoinForIos()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
