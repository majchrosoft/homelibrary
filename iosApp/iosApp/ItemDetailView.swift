import SwiftUI
import Combine
import Shared

@MainActor
final class ItemDetailViewModelObservable: ObservableObject {
    @Published private(set) var state: ItemDetailState = ItemDetailState(
        isLoading: true, item: nil, bookcaseName: nil, errorMessage: nil, deleted: false
    )
    private let viewModel: ItemDetailViewModel
    private var cancellable: AnyCancellable?

    init(itemId: String) {
        viewModel = KoinHelperKt.resolveItemDetailViewModel(itemId: itemId)
        cancellable = StateFlowPublisher(stateFlow: viewModel.state)
            .sink { [weak self] newState in self?.state = newState }
    }

    deinit { viewModel.clear() }

    func toggleBorrow(borrowedBy: String?) {
        viewModel.dispatch(intent: ItemDetailIntent.ToggleBorrow(borrowedBy: borrowedBy))
    }
    func toggleShareable() {
        viewModel.dispatch(intent: ItemDetailIntent.ToggleShareable())
    }
    func delete() {
        viewModel.dispatch(intent: ItemDetailIntent.Delete())
    }
}

struct ItemDetailView: View {
    let itemId: String
    @StateObject private var viewModel: ItemDetailViewModelObservable
    @EnvironmentObject var navigator: NavigatorObservable

    @State private var confirmDelete = false
    @State private var borrowDialog = false
    @State private var borrowedBy = ""

    init(itemId: String) {
        self.itemId = itemId
        _viewModel = StateObject(wrappedValue: ItemDetailViewModelObservable(itemId: itemId))
    }

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.state.isLoading {
                    ProgressView()
                } else if let item = viewModel.state.item {
                    Form {
                        Section {
                            Text(item.item.title.isEmpty ? "Untitled" : item.item.title).font(.title2)
                            if !item.item.author.isEmpty {
                                Text(item.item.author).font(.headline).foregroundColor(.secondary)
                            }
                            HStack(spacing: 6) {
                                Tag(text: String(describing: item.item.type).lowercased())
                                Tag(text: String(describing: item.item.quality).lowercased())
                                if let bc = viewModel.state.bookcaseName { Tag(text: bc) }
                            }
                        }

                        Section("Details") {
                            DetailRow("ISBN", item.item.isbn)
                            DetailRow("Publisher", item.item.publisher)
                            DetailRow("Published", item.item.publishedYear.map { "\($0)" })
                            DetailRow("Language", item.item.language)
                            DetailRow("Pages", item.item.pages.map { "\($0)" })
                            DetailRow("Notes", item.item.notes)
                        }

                        Section("Sharing") {
                            Toggle("Share on public catalog", isOn: Binding(
                                get: { item.item.shareable },
                                set: { _ in viewModel.toggleShareable() }
                            ))
                        }

                        Section("Borrow") {
                            if item.borrow.isBorrowed {
                                if let by = item.borrow.borrowedBy { Text("Borrower: \(by)") }
                                Button("Mark as returned") { viewModel.toggleBorrow(borrowedBy: nil) }
                            } else {
                                Button("Mark as borrowed") {
                                    borrowedBy = ""
                                    borrowDialog = true
                                }
                            }
                        }

                        if let err = viewModel.state.errorMessage {
                            Text(err).foregroundColor(.red)
                        }
                    }
                } else {
                    Text(viewModel.state.errorMessage ?? "Item not found").foregroundColor(.red).padding()
                }
            }
            .navigationTitle(viewModel.state.item?.item.title ?? "Item")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button { navigator.back() } label: { Image(systemName: "chevron.left") }
                }
                if viewModel.state.item != nil {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button { navigator.push(Screen.ItemEdit(itemId: itemId)) } label: {
                            Image(systemName: "pencil")
                        }
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(role: .destructive) { confirmDelete = true } label: {
                            Image(systemName: "trash")
                        }
                    }
                }
            }
            .alert("Delete this item?", isPresented: $confirmDelete) {
                Button("Cancel", role: .cancel) {}
                Button("Delete", role: .destructive) { viewModel.delete() }
            } message: {
                Text("This will remove it from your library. The action can't be undone.")
            }
            .alert("Who's borrowing it?", isPresented: $borrowDialog) {
                TextField("Borrower", text: $borrowedBy)
                Button("Cancel", role: .cancel) {}
                Button("Confirm") { viewModel.toggleBorrow(borrowedBy: borrowedBy) }
            }
            .onChange(of: viewModel.state.deleted) { _, deleted in
                if deleted { navigator.back() }
            }
        }
    }
}

private struct DetailRow: View {
    let label: String
    let value: String?
    init(_ label: String, _ value: String?) { self.label = label; self.value = value }
    var body: some View {
        if let v = value, !v.isEmpty {
            HStack {
                Text(label).foregroundColor(.secondary)
                Spacer()
                Text(v).multilineTextAlignment(.trailing)
            }
        } else { EmptyView() }
    }
}

private struct Tag: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.caption)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(Color.gray.opacity(0.15))
            .clipShape(Capsule())
    }
}
