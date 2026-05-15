package com.majchrosoft.homelibrary.presentation.catalog

import com.majchrosoft.homelibrary.domain.repository.ItemRepository
import com.majchrosoft.homelibrary.presentation.MviViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Live view over the public shareable catalog with a debounced server-side
 * query. Limits to 50 results — paging will come later if the dataset outgrows
 * a single Firebase node.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SharedCatalogViewModel(
    private val itemRepository: ItemRepository,
) : MviViewModel<SharedCatalogState, SharedCatalogIntent>() {

    private val queryFlow = MutableStateFlow("")

    init {
        queryFlow
            .debounce(300)
            .flatMapLatest { q -> itemRepository.observeSharedCatalog(query = q.ifBlank { null }) }
            .onEach { items -> setState { it.copy(isLoading = false, items = items) } }
            .catch { e -> setState { it.copy(isLoading = false, errorMessage = e.message) } }
            .launchIn(scope)
    }

    override fun initialState() = SharedCatalogState()

    override fun handleIntent(intent: SharedCatalogIntent) {
        when (intent) {
            is SharedCatalogIntent.QueryChanged -> {
                setState { it.copy(query = intent.query) }
                queryFlow.value = intent.query
            }
            SharedCatalogIntent.DismissError -> setState { it.copy(errorMessage = null) }
        }
    }
}
