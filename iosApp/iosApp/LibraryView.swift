import SwiftUI
import Combine
import Shared

/// SwiftUI bridge over the KMP `LibraryViewModel`. Subscribes to the StateFlow
/// via `StateFlowPublisher` and republishes as an `@Published` property.
@MainActor
final class LibraryViewModelObservable: ObservableObject {
    @Published private(set) var state: LibraryState = LibraryState(
        isLoading: true, query: "", items: [], bookcases: [],
        selectedBookcaseId: nil, errorMessage: nil
    )
    private let viewModel: LibraryViewModel
    private var cancellable: AnyCancellable?

    init() {
        viewModel = KoinHelperKt.resolveLibraryViewModel()
        cancellable = StateFlowPublisher(stateFlow: viewModel.state)
            .sink { [weak self] newState in self?.state = newState }
    }

    // The LibraryViewModel is a Koin singleton (shared across app); don't clear it on deinit.

    func setQuery(_ q: String) {
        viewModel.dispatch(intent: LibraryIntent.QueryChanged(query: q))
    }

    func selectBookcase(_ id: String?) {
        viewModel.dispatch(intent: LibraryIntent.BookcaseSelected(bookcaseId: id))
    }
}

struct LibraryView: View {
    @StateObject private var viewModel = LibraryViewModelObservable()
    @EnvironmentObject var navigator: NavigatorObservable

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                VStack(alignment: .leading) {
                    TextField(
                        "Search title, author, ISBN",
                        text: Binding(
                            get: { viewModel.state.query },
                            set: { viewModel.setQuery($0) }
                        )
                    )
                    .textFieldStyle(.roundedBorder)
                    .padding(.horizontal)
                    .padding(.top)

                    if !viewModel.state.bookcases.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                FilterChip(
                                    title: "All",
                                    isSelected: viewModel.state.selectedBookcaseId == nil
                                ) { viewModel.selectBookcase(nil) }

                                ForEach(viewModel.state.bookcases, id: \.id) { bc in
                                    FilterChip(
                                        title: bc.name.isEmpty ? "Untitled" : bc.name,
                                        isSelected: viewModel.state.selectedBookcaseId == bc.id
                                    ) { viewModel.selectBookcase(bc.id) }
                                }
                            }
                            .padding(.horizontal)
                        }
                    }

                    if viewModel.state.isLoading {
                        Spacer(); HStack { Spacer(); ProgressView(); Spacer() }; Spacer()
                    } else if let err = viewModel.state.errorMessage {
                        Text("Error: \(err)").foregroundColor(.red).padding()
                    } else if viewModel.state.filtered.isEmpty {
                        Text("No items yet — tap + to add one.").padding()
                    } else {
                        List(viewModel.state.filtered, id: \.id) { item in
                            Button {
                                navigator.push(Screen.ItemDetail(itemId: item.id))
                            } label: {
                                LibraryRow(item: item)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .navigationTitle("My Library")
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button { navigator.push(Screen.SharedCatalog()) } label: {
                            Image(systemName: "globe")
                        }
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        Button { navigator.push(Screen.Bookcases()) } label: {
                            Image(systemName: "books.vertical")
                        }
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        Button { navigator.push(Screen.Profile()) } label: {
                            Image(systemName: "person.crop.circle")
                        }
                    }
                }

                Button {
                    navigator.push(Screen.ItemEdit(itemId: nil))
                } label: {
                    Image(systemName: "plus")
                        .font(.title)
                        .padding()
                        .background(Color.accentColor)
                        .foregroundColor(.white)
                        .clipShape(Circle())
                        .shadow(radius: 4)
                }
                .padding()
            }
        }
    }
}

private struct LibraryRow: View {
    let item: Item

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(item.item.title.isEmpty ? "Untitled" : item.item.title).font(.headline)
            if !item.item.author.isEmpty {
                Text(item.item.author).font(.subheadline)
            }
            Text("Type: \(String(describing: item.item.type).lowercased()) · Quality: \(String(describing: item.item.quality).lowercased())")
                .font(.caption)
                .foregroundColor(.secondary)
            if item.borrow.isBorrowed {
                let suffix = item.borrow.borrowedBy.map { " — \($0)" } ?? ""
                Text("On loan\(suffix)").font(.caption).foregroundColor(.orange)
            }
        }
    }
}

private struct FilterChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isSelected ? Color.accentColor.opacity(0.2) : Color.gray.opacity(0.15))
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}
