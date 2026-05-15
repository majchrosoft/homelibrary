package com.majchrosoft.homelibrary.presentation.item

import com.majchrosoft.homelibrary.domain.model.Bookcase
import com.majchrosoft.homelibrary.domain.model.Item

/**
 * UI state for the single-item detail screen. Surfaces the underlying [Item],
 * its parent [Bookcase] (resolved from `item.bookcase`), the loading / error
 * state, and any short-lived feedback shown to the user (e.g. "Item deleted").
 */
data class ItemDetailState(
    val isLoading: Boolean = true,
    val item: Item? = null,
    /** Resolved name for [Item.item.bookcase] — null when item has no bookcase or it can't be found. */
    val bookcaseName: String? = null,
    val errorMessage: String? = null,
    /** Set to true once the item is deleted so the host can navigate away. */
    val deleted: Boolean = false,
)

sealed interface ItemDetailIntent {
    /** Toggle the item's loan status. When marking as borrowed, [borrowedBy] is required. */
    data class ToggleBorrow(val borrowedBy: String?) : ItemDetailIntent

    /** Flip [com.majchrosoft.homelibrary.domain.model.ItemDetails.shareable]. */
    data object ToggleShareable : ItemDetailIntent

    data object Delete : ItemDetailIntent
    data object DismissError : ItemDetailIntent
}
