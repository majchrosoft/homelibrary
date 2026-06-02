package com.majchrosoft.homelibrary.presentation.item

import com.majchrosoft.homelibrary.domain.model.Bookcase
import com.majchrosoft.homelibrary.domain.model.ItemQuality
import com.majchrosoft.homelibrary.domain.model.ItemType

/**
 * Form state for adding or editing an item. All free-text fields are kept as
 * raw strings so the UI doesn't need to coerce blanks → nulls for binding.
 */
data class ItemEditState(
    /** When non-null, the screen is in edit mode for this existing item. */
    val editingItemId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val bookcases: List<Bookcase> = emptyList(),
    // Form fields
    val title: String = "",
    val author: String = "",
    val type: ItemType = ItemType.BOOK,
    val quality: ItemQuality = ItemQuality.GOOD,
    val selectedBookcaseId: String? = null,
    val isbn: String = "",
    val publisher: String = "",
    val publishedYear: String = "",
    val language: String = "",
    val pages: String = "",
    val coverUrl: String = "",
    val notes: String = "",
    val shareable: Boolean = false,
) {
    val isValid: Boolean get() = title.isNotBlank()
}

sealed interface ItemEditIntent {
    data class TitleChanged(
        val value: String,
    ) : ItemEditIntent

    data class AuthorChanged(
        val value: String,
    ) : ItemEditIntent

    data class TypeChanged(
        val value: ItemType,
    ) : ItemEditIntent

    data class QualityChanged(
        val value: ItemQuality,
    ) : ItemEditIntent

    data class BookcaseSelected(
        val bookcaseId: String?,
    ) : ItemEditIntent

    data class IsbnChanged(
        val value: String,
    ) : ItemEditIntent

    data class PublisherChanged(
        val value: String,
    ) : ItemEditIntent

    data class PublishedYearChanged(
        val value: String,
    ) : ItemEditIntent

    data class LanguageChanged(
        val value: String,
    ) : ItemEditIntent

    data class PagesChanged(
        val value: String,
    ) : ItemEditIntent

    data class CoverUrlChanged(
        val value: String,
    ) : ItemEditIntent

    data class NotesChanged(
        val value: String,
    ) : ItemEditIntent

    data class ShareableChanged(
        val value: Boolean,
    ) : ItemEditIntent

    data object Save : ItemEditIntent

    data object DismissError : ItemEditIntent
}
