import SwiftUI
import Combine
import Shared

@MainActor
final class SharedCatalogViewModelObservable: ObservableObject {
    @Published private(set) var state: SharedCatalogState = SharedCatalogState(
        isLoading: true, query: "", items: [], errorMessage: nil
    )
    private let viewModel: SharedCatalogViewModel
    private var cancellable: AnyCancellable?

    init() {
        viewModel = KoinHelperKt.resolveSharedCatalogViewModel()
        cancellable = StateFlowPublisher(stateFlow: viewModel.state)
            .sink { [weak self] newState in self?.state = newState }
    }

    func setQuery(_ q: String) {
        viewModel.dispatch(intent: SharedCatalogIntent.QueryChanged(query: q))
    }
}

struct SharedCatalogView: View {
    @StateObject private var viewModel = SharedCatalogViewModelObservable()
    @EnvironmentObject var navigator: NavigatorObservable

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading) {
                TextField("Search the public catalog", text: Binding(
                    get: { viewModel.state.query },
                    set: { viewModel.setQuery($0) }
                ))
                .textFieldStyle(.roundedBorder)
                .padding(.horizontal)
                .padding(.top)

                if viewModel.state.isLoading {
                    Spacer(); HStack { Spacer(); ProgressView(); Spacer() }; Spacer()
                } else if let err = viewModel.state.errorMessage {
                    Text("Error: \(err)").foregroundColor(.red).padding()
                } else if viewModel.state.items.isEmpty {
                    Text("No matching items right now. Be the first to share — flip the toggle on any of your items.").padding()
                } else {
                    List(viewModel.state.items, id: \.id) { item in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.item.title.isEmpty ? "Untitled" : item.item.title).font(.headline)
                            if !item.item.author.isEmpty {
                                Text(item.item.author).font(.subheadline)
                            }
                            Text("Owner: \(item.ownerId.isEmpty ? "anonymous" : item.ownerId) · \(String(describing: item.item.type).lowercased())")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            if item.borrow.isBorrowed {
                                Text("Currently on loan — waitlist will appear here.")
                                    .font(.caption).foregroundColor(.orange)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Borrow from the community")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button { navigator.back() } label: { Image(systemName: "chevron.left") }
                }
            }
        }
    }
}
