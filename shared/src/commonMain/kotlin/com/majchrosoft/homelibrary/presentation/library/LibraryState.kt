package com.majchrosoft.homelibrary.presentation.library

import com.majchrosoft.homelibrary.domain.model.Bookcase
import com.majchrosoft.homelibrary.domain.model.Item

sealed interface BorrowFilter {
    data object All : BorrowFilter

    data object Borrowed : BorrowFilter

    data object NotBorrowed : BorrowFilter
}

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
    val borrowFilter: BorrowFilter = BorrowFilter.All,
    val errorMessage: String? = null,
) {
    val filtered: List<Item>
        get() {
            val byBookcase =
                if (selectedBookcaseId == null) {
                    items
                } else {
                    items.filter { it.item.bookcase == selectedBookcaseId }
                }
            return when (borrowFilter) {
                BorrowFilter.All -> byBookcase
                BorrowFilter.Borrowed -> byBookcase.filter { it.borrow.isBorrowed }
                BorrowFilter.NotBorrowed -> byBookcase.filter { !it.borrow.isBorrowed }
            }.filter { i ->
                val t = i.item.title
                val a = i.item.author
                val isbn = i.item.isbn
                val p = i.item.publisher
                t.contains(query, ignoreCase = true) ||
                    a.contains(query, ignoreCase = true) ||
                    (isbn?.contains(query, ignoreCase = true) == true) ||
                    (p?.contains(query, ignoreCase = true) == true)
            }
        }
}

sealed interface LibraryIntent {
    data class QueryChanged(
        val query: String,
    ) : LibraryIntent

    data class BookcaseSelected(
        val bookcaseId: String?,
    ) : LibraryIntent

    data class BorrowFilterSelected(
        val filter: BorrowFilter,
    ) : LibraryIntent

    data object Refresh : LibraryIntent

    data object DismissError : LibraryIntent
}
