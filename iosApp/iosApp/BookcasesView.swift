import SwiftUI
import Combine
import Shared

@MainActor
final class BookcasesViewModelObservable: ObservableObject {
    @Published private(set) var state: BookcasesState = BookcasesState(
        isLoading: true, bookcases: [], errorMessage: nil
    )
    private let viewModel: BookcasesViewModel
    private var cancellable: AnyCancellable?

    init() {
        viewModel = KoinHelperKt.resolveBookcasesViewModel()
        cancellable = StateFlowPublisher(stateFlow: viewModel.state)
            .sink { [weak self] newState in self?.state = newState }
    }

    func delete(_ id: String) {
        viewModel.dispatch(intent: BookcasesIntent.Delete(bookcaseId: id))
    }
}

struct BookcasesView: View {
    @StateObject private var viewModel = BookcasesViewModelObservable()
    @EnvironmentObject var navigator: NavigatorObservable
    @State private var pendingDelete: Bookcase?

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                Group {
                    if viewModel.state.isLoading {
                        ProgressView()
                    } else if let err = viewModel.state.errorMessage {
                        Text("Error: \(err)").foregroundColor(.red).padding()
                    } else if viewModel.state.bookcases.isEmpty {
                        Text("No bookcases yet — tap + to add a shelf, box, or room.").padding()
                    } else {
                        List(viewModel.state.bookcases, id: \.id) { bc in
                            Button { navigator.push(Screen.BookcaseEdit(bookcaseId: bc.id)) } label: {
                                VStack(alignment: .leading) {
                                    Text(bc.name.isEmpty ? "Untitled" : bc.name).font(.headline)
                                    if let loc = bc.location, !loc.isEmpty {
                                        Text(loc).font(.caption).foregroundColor(.secondary)
                                    }
                                    if let desc = bc.description_, !desc.isEmpty {
                                        Text(desc).font(.caption).foregroundColor(.secondary)
                                    }
                                }
                            }
                            .buttonStyle(.plain)
                            .swipeActions {
                                Button(role: .destructive) { pendingDelete = bc } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                        }
                    }
                }
                .navigationTitle("Bookcases")
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button { navigator.back() } label: { Image(systemName: "chevron.left") }
                    }
                }

                Button { navigator.push(Screen.BookcaseEdit(bookcaseId: nil)) } label: {
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
            .alert(item: $pendingDelete) { bc in
                Alert(
                    title: Text("Delete \(bc.name.isEmpty ? "this bookcase" : bc.name)?"),
                    message: Text("Items in this bookcase will keep their reference but show as unassigned."),
                    primaryButton: .destructive(Text("Delete")) { viewModel.delete(bc.id) },
                    secondaryButton: .cancel()
                )
            }
        }
    }
}

// Bookcase exported from Kotlin doesn't conform to Identifiable; we extend it locally.
extension Bookcase: Identifiable {}
