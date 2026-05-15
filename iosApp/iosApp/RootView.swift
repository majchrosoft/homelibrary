import SwiftUI
import Shared

/// Top-level router. Decides between the sign-in flow and the authenticated
/// section, then within the authenticated section swaps SwiftUI views based on
/// the KMP `Navigator.current` state.
struct RootView: View {

    @StateObject private var auth = AuthViewModelObservable()
    @StateObject private var navigator = NavigatorObservable()

    var body: some View {
        Group {
            if auth.state.user != nil {
                AuthedRoot()
                    .environmentObject(navigator)
            } else {
                SignInView(viewModel: auth)
            }
        }
    }
}

private struct AuthedRoot: View {
    @EnvironmentObject var navigator: NavigatorObservable

    var body: some View {
        switch navigator.current {
        case is Screen.Library:
            LibraryView()
        case is Screen.Bookcases:
            BookcasesView()
        case let s as Screen.BookcaseEdit:
            BookcaseEditView(bookcaseId: s.bookcaseId)
        case let s as Screen.ItemDetail:
            ItemDetailView(itemId: s.itemId)
        case let s as Screen.ItemEdit:
            ItemEditView(itemId: s.itemId)
        case is Screen.SharedCatalog:
            SharedCatalogView()
        case is Screen.Profile:
            ProfileView()
        default:
            LibraryView()
        }
    }
}
