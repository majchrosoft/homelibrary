package com.majchrosoft.homelibrary.presentation.item

import com.majchrosoft.homelibrary.domain.model.Item
import com.majchrosoft.homelibrary.domain.model.ItemDetails
import com.majchrosoft.homelibrary.domain.repository.AuthRepository
import com.majchrosoft.homelibrary.domain.repository.BookcaseRepository
import com.majchrosoft.homelibrary.domain.repository.ItemRepository
import com.majchrosoft.homelibrary.presentation.MviViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Add (when [editingItemId] is null) or edit an existing item. Loads bookcases
 * up-front so the bookcase picker has options, and prefills the form from the
 * existing item when in edit mode.
 *
 * Not a Koin singleton — one instance per edit session. Hosts construct it
 * manually with the right [editingItemId].
 */
class ItemEditViewModel(
    private val editingItemId: String?,
    private val itemRepository: ItemRepository,
    private val bookcaseRepository: BookcaseRepository,
    private val authRepository: AuthRepository,
) : MviViewModel<ItemEditState, ItemEditIntent>() {

    init { load() }

    override fun initialState() = ItemEditState(editingItemId = editingItemId, isLoading = true)

    private fun load() {
        scope.launch {
            val user = authRepository.currentUser.first()
            if (user == null) {
                setState { it.copy(isLoading = false, errorMessage = "Not signed in") }
                return@launch
            }
            val bookcases = bookcaseRepository.observeMine(user.id).first()
            val existing = editingItemId?.let { itemRepository.getById(user.id, it) }
            setState {
                if (existing == null) {
                    it.copy(isLoading = false, bookcases = bookcases)
                } else {
                    it.copy(
                        isLoading = false,
                        bookcases = bookcases,
                        title = existing.item.title,
                        author = existing.item.author,
                        type = existing.item.type,
                        quality = existing.item.quality,
                        selectedBookcaseId = existing.item.bookcase,
                        isbn = existing.item.isbn.orEmpty(),
                        publisher = existing.item.publisher.orEmpty(),
                        publishedYear = existing.item.publishedYear?.toString().orEmpty(),
                        language = existing.item.language.orEmpty(),
                        pages = existing.item.pages?.toString().orEmpty(),
                        coverUrl = existing.item.coverUrl.orEmpty(),
                        notes = existing.item.notes.orEmpty(),
                        shareable = existing.item.shareable,
                    )
                }
            }
        }
    }

    override fun handleIntent(intent: ItemEditIntent) {
        when (intent) {
            is ItemEditIntent.TitleChanged -> setState { it.copy(title = intent.value) }
            is ItemEditIntent.AuthorChanged -> setState { it.copy(author = intent.value) }
            is ItemEditIntent.TypeChanged -> setState { it.copy(type = intent.value) }
            is ItemEditIntent.QualityChanged -> setState { it.copy(quality = intent.value) }
            is ItemEditIntent.BookcaseSelected -> setState { it.copy(selectedBookcaseId = intent.bookcaseId) }
            is ItemEditIntent.IsbnChanged -> setState { it.copy(isbn = intent.value) }
            is ItemEditIntent.PublisherChanged -> setState { it.copy(publisher = intent.value) }
            is ItemEditIntent.PublishedYearChanged -> setState { it.copy(publishedYear = intent.value.filter { c -> c.isDigit() }) }
            is ItemEditIntent.LanguageChanged -> setState { it.copy(language = intent.value) }
            is ItemEditIntent.PagesChanged -> setState { it.copy(pages = intent.value.filter { c -> c.isDigit() }) }
            is ItemEditIntent.CoverUrlChanged -> setState { it.copy(coverUrl = intent.value) }
            is ItemEditIntent.NotesChanged -> setState { it.copy(notes = intent.value) }
            is ItemEditIntent.ShareableChanged -> setState { it.copy(shareable = intent.value) }
            ItemEditIntent.Save -> save()
            ItemEditIntent.DismissError -> setState { it.copy(errorMessage = null) }
        }
    }

    private fun save() {
        val current = state.value
        if (!current.isValid) {
            setState { it.copy(errorMessage = "Title is required") }
            return
        }
        scope.launch {
            setState { it.copy(isSaving = true, errorMessage = null) }
            val user = authRepository.currentUser.first()
            if (user == null) {
                setState { it.copy(isSaving = false, errorMessage = "Not signed in") }
                return@launch
            }
            val details = ItemDetails(
                title = current.title.trim(),
                author = current.author.trim(),
                type = current.type,
                quality = current.quality,
                bookcase = current.selectedBookcaseId,
                isbn = current.isbn.trim().ifBlank { null },
                publisher = current.publisher.trim().ifBlank { null },
                publishedYear = current.publishedYear.toIntOrNull(),
                language = current.language.trim().ifBlank { null },
                pages = current.pages.toIntOrNull(),
                coverUrl = current.coverUrl.trim().ifBlank { null },
                notes = current.notes.trim().ifBlank { null },
                shareable = current.shareable,
            )
            val result = if (editingItemId == null) {
                itemRepository.add(Item(ownerId = user.id, item = details))
                    .map { /* discard the new id — host navigates away on isSaved */ }
            } else {
                // Preserve existing borrow state when editing — only descriptive fields change here.
                val existing = itemRepository.getById(user.id, editingItemId)
                if (existing == null) {
                    Result.failure(IllegalStateException("Item disappeared during edit"))
                } else {
                    itemRepository.update(existing.copy(item = details))
                }
            }
            result
                .onSuccess { setState { it.copy(isSaving = false, isSaved = true) } }
                .onFailure { e -> setState { it.copy(isSaving = false, errorMessage = e.message) } }
        }
    }
}
