package com.majchrosoft.homelibrary.presentation.library

import com.majchrosoft.homelibrary.domain.model.Bookcase
import com.majchrosoft.homelibrary.domain.model.Item
import com.majchrosoft.homelibrary.domain.repository.AuthRepository
import com.majchrosoft.homelibrary.domain.repository.BookcaseRepository
import com.majchrosoft.homelibrary.domain.repository.ItemRepository
import com.majchrosoft.homelibrary.presentation.MviViewModel
import com.majchrosoft.homelibrary.presentation.library.BorrowFilter
import io.github.aakira.napier.Napier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Streams the signed-in user's items + bookcases into a single [LibraryState].
 *
 * The ViewModel is a Koin app-scoped singleton — Android, Web, and iOS hosts
 * each pull the same instance and just bind the StateFlow into their UI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val itemRepository: ItemRepository,
    private val bookcaseRepository: BookcaseRepository,
    private val authRepository: AuthRepository,
) : MviViewModel<LibraryState, LibraryIntent>() {
    init {
        Napier.d { "LibraryViewModel: init" }
        authRepository.currentUser
            .distinctUntilChanged()
            .onEach { user -> Napier.d("LibraryViewModel: authRepository.currentUser emitted: ${user?.id}", tag = "DEBUG app") }
            .flatMapLatest { user ->
                if (user == null) {
                    Napier.d { "LibraryViewModel: user is null, emitting empty" }
                    flowOf(emptyList<Item>() to emptyList<Bookcase>())
                } else {
                    Napier.d { "LibraryViewModel: user is ${user.id}, observing library and bookcases" }
                    combine(
                        itemRepository
                            .observeMyLibrary(user.id)
                            .onEach { Napier.d { "LibraryViewModel: itemRepository.observeMyLibrary emitted ${it.size} items" } },
                        bookcaseRepository
                            .observeMine(user.id)
                            .onEach { Napier.d { "LibraryViewModel: bookcaseRepository.observeMine emitted ${it.size} bookcases" } },
                    ) { items, bookcases -> items to bookcases }
                }
            }.onEach { (items, bookcases) ->
                Napier.d { "LibraryViewModel: Updating state with ${items.size} items and ${bookcases.size} bookcases" }
                setState {
                    it.copy(
                        isLoading = false,
                        items = items,
                        bookcases = bookcases,
                    )
                }
            }.catch { e ->
                Napier.e(e) { "LibraryViewModel: Error in flow" }
                setState { it.copy(isLoading = false, errorMessage = e.message) }
            }.launchIn(scope)
    }

    override fun initialState() = LibraryState()

    override fun handleIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.QueryChanged -> setState { it.copy(query = intent.query) }
            is LibraryIntent.BookcaseSelected -> setState { it.copy(selectedBookcaseId = intent.bookcaseId) }
            is LibraryIntent.BorrowFilterSelected -> setState { it.copy(borrowFilter = intent.filter) }
            LibraryIntent.Refresh -> setState { it.copy(isLoading = true) }
            LibraryIntent.DismissError -> setState { it.copy(errorMessage = null) }
        }
    }
}
