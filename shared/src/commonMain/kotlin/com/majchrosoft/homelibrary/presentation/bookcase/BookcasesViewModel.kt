package com.majchrosoft.homelibrary.presentation.bookcase

import com.majchrosoft.homelibrary.domain.repository.AuthRepository
import com.majchrosoft.homelibrary.domain.repository.BookcaseRepository
import com.majchrosoft.homelibrary.presentation.MviViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Live list of the signed-in user's bookcases. Owns the delete action — add /
 * edit happen on [BookcaseEditViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookcasesViewModel(
    private val bookcaseRepository: BookcaseRepository,
    private val authRepository: AuthRepository,
) : MviViewModel<BookcasesState, BookcasesIntent>() {
    init {
        authRepository.currentUser
            .flatMapLatest { user ->
                if (user == null) flowOf(emptyList()) else bookcaseRepository.observeMine(user.id)
            }.onEach { list -> setState { it.copy(isLoading = false, bookcases = list) } }
            .catch { e -> setState { it.copy(isLoading = false, errorMessage = e.message) } }
            .launchIn(scope)
    }

    override fun initialState() = BookcasesState()

    override fun handleIntent(intent: BookcasesIntent) {
        when (intent) {
            is BookcasesIntent.Delete -> delete(intent.bookcaseId)
            BookcasesIntent.DismissError -> setState { it.copy(errorMessage = null) }
        }
    }

    private fun delete(bookcaseId: String) {
        scope.launch {
            val user =
                authRepository.currentUser.first()
                    ?: return@launch setState { it.copy(errorMessage = "Not signed in") }
            bookcaseRepository
                .delete(user.id, bookcaseId)
                .onFailure { e -> setState { it.copy(errorMessage = e.message) } }
        }
    }
}
