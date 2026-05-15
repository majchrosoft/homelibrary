package com.majchrosoft.homelibrary.presentation.item

import com.majchrosoft.homelibrary.domain.model.BorrowState
import com.majchrosoft.homelibrary.domain.repository.AuthRepository
import com.majchrosoft.homelibrary.domain.repository.BookcaseRepository
import com.majchrosoft.homelibrary.domain.repository.ItemRepository
import com.majchrosoft.homelibrary.presentation.MviViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Loads and mutates a single item. Lifecycle: constructed by the host with the
 * `itemId` it wants to show, then destroyed (via [clear]) when navigating away.
 *
 * Not registered as a Koin singleton — that would tie one instance to a single
 * item ID at app scope. Hosts instantiate it manually with the resolved
 * dependencies and the chosen [itemId].
 */
class ItemDetailViewModel(
    private val itemId: String,
    private val itemRepository: ItemRepository,
    private val bookcaseRepository: BookcaseRepository,
    private val authRepository: AuthRepository,
) : MviViewModel<ItemDetailState, ItemDetailIntent>() {

    init { load() }

    override fun initialState() = ItemDetailState()

    private fun load() {
        scope.launch {
            val user = authRepository.currentUser.first()
            if (user == null) {
                setState { it.copy(isLoading = false, errorMessage = "Not signed in") }
                return@launch
            }
            val item = itemRepository.getById(user.id, itemId)
            if (item == null) {
                setState { it.copy(isLoading = false, errorMessage = "Item not found") }
                return@launch
            }
            val bookcaseName = item.item.bookcase
                ?.let { bcId -> bookcaseRepository.getById(user.id, bcId)?.name }
            setState { it.copy(isLoading = false, item = item, bookcaseName = bookcaseName) }
        }
    }

    override fun handleIntent(intent: ItemDetailIntent) {
        when (intent) {
            is ItemDetailIntent.ToggleBorrow -> toggleBorrow(intent.borrowedBy)
            ItemDetailIntent.ToggleShareable -> toggleShareable()
            ItemDetailIntent.Delete -> delete()
            ItemDetailIntent.DismissError -> setState { it.copy(errorMessage = null) }
        }
    }

    private fun toggleBorrow(borrowedBy: String?) {
        val current = state.value.item ?: return
        val newBorrow = if (current.borrow.isBorrowed) {
            BorrowState(isBorrowed = false)
        } else {
            BorrowState(
                isBorrowed = true,
                borrowedBy = borrowedBy.orEmpty().ifBlank { "Unknown" },
                borrowedAt = Clock.System.now(),
            )
        }
        val updated = current.copy(borrow = newBorrow)
        scope.launch {
            itemRepository.update(updated)
                .onSuccess { setState { it.copy(item = updated) } }
                .onFailure { e -> setState { it.copy(errorMessage = e.message) } }
        }
    }

    private fun toggleShareable() {
        val current = state.value.item ?: return
        val updated = current.copy(item = current.item.copy(shareable = !current.item.shareable))
        scope.launch {
            itemRepository.update(updated)
                .onSuccess { setState { it.copy(item = updated) } }
                .onFailure { e -> setState { it.copy(errorMessage = e.message) } }
        }
    }

    private fun delete() {
        val current = state.value.item ?: return
        scope.launch {
            itemRepository.delete(current.ownerId, current.id)
                .onSuccess { setState { it.copy(deleted = true) } }
                .onFailure { e -> setState { it.copy(errorMessage = e.message) } }
        }
    }
}
