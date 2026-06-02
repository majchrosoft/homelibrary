package com.majchrosoft.homelibrary.presentation.bookcase

import com.majchrosoft.homelibrary.domain.model.Bookcase
import com.majchrosoft.homelibrary.domain.repository.AuthRepository
import com.majchrosoft.homelibrary.domain.repository.BookcaseRepository
import com.majchrosoft.homelibrary.presentation.MviViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BookcaseEditViewModel(
    private val editingBookcaseId: String?,
    private val bookcaseRepository: BookcaseRepository,
    private val authRepository: AuthRepository,
) : MviViewModel<BookcaseEditState, BookcaseEditIntent>() {
    init {
        load()
    }

    override fun initialState() = BookcaseEditState(editingBookcaseId = editingBookcaseId, isLoading = editingBookcaseId != null)

    private fun load() {
        if (editingBookcaseId == null) return
        scope.launch {
            val user = authRepository.currentUser.first()
            if (user == null) {
                setState { it.copy(isLoading = false, errorMessage = "Not signed in") }
                return@launch
            }
            val existing = bookcaseRepository.getById(user.id, editingBookcaseId)
            setState {
                if (existing == null) {
                    it.copy(isLoading = false, errorMessage = "Bookcase not found")
                } else {
                    it.copy(
                        isLoading = false,
                        name = existing.name,
                        description = existing.description.orEmpty(),
                        location = existing.location.orEmpty(),
                    )
                }
            }
        }
    }

    override fun handleIntent(intent: BookcaseEditIntent) {
        when (intent) {
            is BookcaseEditIntent.NameChanged -> setState { it.copy(name = intent.value) }
            is BookcaseEditIntent.DescriptionChanged -> setState { it.copy(description = intent.value) }
            is BookcaseEditIntent.LocationChanged -> setState { it.copy(location = intent.value) }
            BookcaseEditIntent.Save -> save()
            BookcaseEditIntent.DismissError -> setState { it.copy(errorMessage = null) }
        }
    }

    private fun save() {
        val current = state.value
        if (!current.isValid) {
            setState { it.copy(errorMessage = "Name is required") }
            return
        }
        scope.launch {
            setState { it.copy(isSaving = true, errorMessage = null) }
            val user = authRepository.currentUser.first()
            if (user == null) {
                setState { it.copy(isSaving = false, errorMessage = "Not signed in") }
                return@launch
            }
            val toSave =
                Bookcase(
                    id = editingBookcaseId.orEmpty(),
                    name = current.name.trim(),
                    description = current.description.trim().ifBlank { null },
                    location = current.location.trim().ifBlank { null },
                )
            val result =
                if (editingBookcaseId == null) {
                    bookcaseRepository.add(user.id, toSave).map { }
                } else {
                    bookcaseRepository.update(user.id, toSave)
                }
            result
                .onSuccess { setState { it.copy(isSaving = false, isSaved = true) } }
                .onFailure { e -> setState { it.copy(isSaving = false, errorMessage = e.message) } }
        }
    }
}
