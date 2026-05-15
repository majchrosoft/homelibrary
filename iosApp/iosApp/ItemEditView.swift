import SwiftUI
import Combine
import Shared

@MainActor
final class ItemEditViewModelObservable: ObservableObject {
    @Published private(set) var state: ItemEditState
    private let viewModel: ItemEditViewModel
    private var cancellable: AnyCancellable?

    init(itemId: String?) {
        viewModel = KoinHelperKt.resolveItemEditViewModel(itemId: itemId)
        state = viewModel.state.value as! ItemEditState
        cancellable = StateFlowPublisher(stateFlow: viewModel.state)
            .sink { [weak self] newState in self?.state = newState }
    }

    deinit { viewModel.clear() }

    func dispatch(_ intent: ItemEditIntent) { viewModel.dispatch(intent: intent) }
}

struct ItemEditView: View {
    let itemId: String?
    @StateObject private var viewModel: ItemEditViewModelObservable
    @EnvironmentObject var navigator: NavigatorObservable

    init(itemId: String?) {
        self.itemId = itemId
        _viewModel = StateObject(wrappedValue: ItemEditViewModelObservable(itemId: itemId))
    }

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.state.isLoading {
                    ProgressView()
                } else {
                    Form {
                        Section("Item") {
                            TextField("Title *", text: bind(\.title) { .TitleChanged(value: $0) })
                            TextField("Author", text: bind(\.author) { .AuthorChanged(value: $0) })
                            typePicker
                            qualityPicker
                            bookcasePicker
                        }
                        Section("Metadata") {
                            TextField("ISBN", text: bind(\.isbn) { .IsbnChanged(value: $0) })
                            TextField("Publisher", text: bind(\.publisher) { .PublisherChanged(value: $0) })
                            TextField("Year", text: bind(\.publishedYear) { .PublishedYearChanged(value: $0) })
                                .keyboardType(.numberPad)
                            TextField("Pages", text: bind(\.pages) { .PagesChanged(value: $0) })
                                .keyboardType(.numberPad)
                            TextField("Language", text: bind(\.language) { .LanguageChanged(value: $0) })
                            TextField("Cover URL", text: bind(\.coverUrl) { .CoverUrlChanged(value: $0) })
                            TextField("Notes", text: bind(\.notes) { .NotesChanged(value: $0) }, axis: .vertical)
                                .lineLimit(2...6)
                        }
                        Section("Sharing") {
                            Toggle("Share on public catalog", isOn: Binding(
                                get: { viewModel.state.shareable },
                                set: { viewModel.dispatch(.ShareableChanged(value: $0)) }
                            ))
                        }
                        if let err = viewModel.state.errorMessage {
                            Section { Text(err).foregroundColor(.red) }
                        }
                        Section {
                            Button(itemId == nil ? "Add to library" : "Save changes") {
                                viewModel.dispatch(.Save())
                            }
                            .disabled(!viewModel.state.isValid || viewModel.state.isSaving)
                        }
                    }
                }
            }
            .navigationTitle(itemId == nil ? "Add item" : "Edit item")
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

    private var typePicker: some View {
        Picker("Type", selection: Binding(
            get: { viewModel.state.type },
            set: { viewModel.dispatch(.TypeChanged(value: $0)) }
        )) {
            ForEach(EnumExportsKt.itemTypeValues(), id: \.self) { t in
                Text(String(describing: t).lowercased()).tag(t)
            }
        }
    }

    private var qualityPicker: some View {
        Picker("Quality", selection: Binding(
            get: { viewModel.state.quality },
            set: { viewModel.dispatch(.QualityChanged(value: $0)) }
        )) {
            ForEach(EnumExportsKt.itemQualityValues(), id: \.self) { q in
                Text(String(describing: q).lowercased()).tag(q)
            }
        }
    }

    private var bookcasePicker: some View {
        Picker("Bookcase", selection: Binding(
            get: { viewModel.state.selectedBookcaseId ?? "" },
            set: { viewModel.dispatch(.BookcaseSelected(bookcaseId: $0.isEmpty ? nil : $0)) }
        )) {
            Text("— None —").tag("")
            ForEach(viewModel.state.bookcases, id: \.id) { bc in
                Text(bc.name.isEmpty ? "Untitled" : bc.name).tag(bc.id)
            }
        }
    }

    private func bind(
        _ keyPath: KeyPath<ItemEditState, String>,
        _ intent: @escaping (String) -> ItemEditIntent
    ) -> Binding<String> {
        Binding(
            get: { viewModel.state[keyPath: keyPath] },
            set: { viewModel.dispatch(intent($0)) }
        )
    }
}
