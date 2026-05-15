package com.majchrosoft.homelibrary.presentation.library

import com.majchrosoft.homelibrary.domain.model.Bookcase
import com.majchrosoft.homelibrary.domain.model.Item

/**
 * UI state for the home-library list screen.
 *
 * The screen shows bookcases at the top (chips / filter) and the user's items
 * underneath, optionally filtered by [query] and [selectedBookcaseId]. Both
 * lists come from the user's own Realtime DB subtree.
 */
data class LibraryState(
    val isLoading: Boolean = true,
    val query: String = "",
    val items: List<Item> = emptyList(),
    val bookcases: List<Bookcase> = emptyList(),
    /** When non-null, only items in this bookcase are surfaced via [filtered]. */
    val selectedBookcaseId: String? = null,
    val errorMessage: String? = null,
) {
    val filtered: List<Item>
        get() {
            val byBookcase = if (selectedBookcaseId == null) {
                items
            } else {
                items.filter { it.item.bookcase == selectedBookcaseId }
            }
            return if (query.isBlank()) {
                byBookcase
            } else {
                byBookcase.filter { i ->
                    i.item.title.contains(query, ignoreCase = true) ||
                        i.item.author.contains(query, ignoreCase = true) ||
                        (i.item.isbn?.contains(query, ignoreCase = true) == true)
                }
            }
        }
}

sealed interface LibraryIntent {
    data class QueryChanged(val query: String) : LibraryIntent
    data class BookcaseSelected(val bookcaseId: String?) : LibraryIntent
    data object Refresh : LibraryIntent
    data object DismissError : LibraryIntent
}
