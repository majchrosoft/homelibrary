import Foundation
import Combine
import Shared

/// SwiftUI bridge over the KMP `AuthViewModel`. Subscribes to the StateFlow
/// via the GitLive Firebase / kotlinx-coroutines `collect` extension and
/// republishes as an `@Published` property.
@MainActor
final class AuthViewModelObservable: ObservableObject {
    @Published private(set) var state: AuthState = AuthState(user: nil, isLoading: false, errorMessage: nil)

    private let viewModel: AuthViewModel
    private var cancellable: AnyCancellable?

    init() {
        // Resolve from the Koin container started in `iosAppApp.init()`.
        self.viewModel = KoinHelperKt.resolveAuthViewModel()

        // FlowAdapter is generated for us by the GitLive Firebase plugin under the hood;
        // we re-collect into a Combine subject for SwiftUI ergonomics.
        cancellable = StateFlowPublisher(stateFlow: viewModel.state)
            .sink { [weak self] newState in
                self?.state = newState
            }
    }

    deinit { viewModel.clear() }

    func signIn(email: String, password: String) {
        viewModel.dispatch(intent: AuthIntent.SignIn(email: email, password: password))
    }

    func signUp(email: String, password: String, displayName: String?) {
        viewModel.dispatch(intent: AuthIntent.SignUp(email: email, password: password, displayName: displayName))
    }

    func signOut() {
        viewModel.dispatch(intent: AuthIntent.SignOut())
    }

    func dismissError() {
        viewModel.dispatch(intent: AuthIntent.DismissError())
    }
}
