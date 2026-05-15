import SwiftUI
import Combine
import Shared

@MainActor
final class ProfileViewModelObservable: ObservableObject {
    @Published private(set) var state: ProfileState = ProfileState(
        user: nil, itemCount: 0, bookcaseCount: 0, shareableCount: 0
    )
    private let viewModel: ProfileViewModel
    private var cancellable: AnyCancellable?

    init() {
        viewModel = KoinHelperKt.resolveProfileViewModel()
        cancellable = StateFlowPublisher(stateFlow: viewModel.state)
            .sink { [weak self] newState in self?.state = newState }
    }

    func signOut() {
        viewModel.dispatch(intent: ProfileIntent.SignOut())
    }
}

struct ProfileView: View {
    @StateObject private var viewModel = ProfileViewModelObservable()
    @EnvironmentObject var navigator: NavigatorObservable

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    if let name = viewModel.state.user?.displayName, !name.isEmpty {
                        Text(name).font(.title2)
                    } else if let email = viewModel.state.user?.email {
                        Text(email).font(.title3)
                    } else {
                        Text("Signed out")
                    }
                    if let email = viewModel.state.user?.email, !email.isEmpty {
                        Text(email).foregroundColor(.secondary)
                    }
                }
                Section("Your library") {
                    HStack { Text("Items"); Spacer(); Text("\(viewModel.state.itemCount)") }
                    HStack { Text("Bookcases"); Spacer(); Text("\(viewModel.state.bookcaseCount)") }
                    HStack { Text("Shared on catalog"); Spacer(); Text("\(viewModel.state.shareableCount)") }
                }
                Section {
                    Button("Sign out", role: .destructive) { viewModel.signOut() }
                }
            }
            .navigationTitle("Profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button { navigator.back() } label: { Image(systemName: "chevron.left") }
                }
            }
        }
    }
}
