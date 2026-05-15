import SwiftUI
import Combine
import Shared

@MainActor
final class BookcaseEditViewModelObservable: ObservableObject {
    @Published private(set) var state: BookcaseEditState
    private let viewModel: BookcaseEditViewModel
    private var cancellable: AnyCancellable?

    init(bookcaseId: String?) {
        viewModel = KoinHelperKt.resolveBookcaseEditViewModel(bookcaseId: bookcaseId)
        state = viewModel.state.value as! BookcaseEditState
        cancellable = StateFlowPublisher(stateFlow: viewModel.state)
            .sink { [weak self] newState in self?.state = newState }
    }

    deinit { viewModel.clear() }

    func dispatch(_ intent: BookcaseEditIntent) { viewModel.dispatch(intent: intent) }
}

struct BookcaseEditView: View {
    let bookcaseId: String?
    @StateObject private var viewModel: BookcaseEditViewModelObservable
    @EnvironmentObject var navigator: NavigatorObservable

    init(bookcaseId: String?) {
        self.bookcaseId = bookcaseId
        _viewModel = StateObject(wrappedValue: BookcaseEditViewModelObservable(bookcaseId: bookcaseId))
    }

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.state.isLoading {
                    ProgressView()
                } else {
                    Form {
                        Section {
                            TextField("Name *", text: Binding(
                                get: { viewModel.state.name },
                                set: { viewModel.dispatch(.NameChanged(value: $0)) }
                            ))
                            TextField("Location (e.g. Living room)", text: Binding(
                                get: { viewModel.state.location },
                                set: { viewModel.dispatch(.LocationChanged(value: $0)) }
                            ))
                            TextField(
                                "Description",
                                text: Binding(
                                    get: { viewModel.state.description_ },
                                    set: { viewModel.dispatch(.DescriptionChanged(value: $0)) }
                                ),
                                axis: .vertical
                            )
                            .lineLimit(2...6)
                        }
                        if let err = viewModel.state.errorMessage {
                            Section { Text(err).foregroundColor(.red) }
                        }
                        Section {
                            Button(bookcaseId == nil ? "Create bookcase" : "Save changes") {
                                viewModel.dispatch(.Save())
                            }
                            .disabled(!viewModel.state.isValid || viewModel.state.isSaving)
                        }
                    }
                }
            }
            .navigationTitle(bookcaseId == nil ? "Add bookcase" : "Edit bookcase")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button { navigator.back() } label: { Image(systemName: "chevron.left") }
                }
            }
            .onChange(of: viewModel.state.isSaved) { _, saved in
                if saved { navigator.back() }
            }
        }
    }
}
