package com.majchrosoft.homelibrary.presentation.catalog

import com.majchrosoft.homelibrary.domain.model.Item

/**
 * Public catalog: items where the owning user opted-in via
 * [com.majchrosoft.homelibrary.domain.model.ItemDetails.shareable] = true. A
 * Cloud Function projects them under `catalog/items` — see [com.majchrosoft.homelibrary.data.firebase.FirebasePaths.sharedCatalog].
 */
data class SharedCatalogState(
    val isLoading: Boolean = true,
    val query: String = "",
    val items: List<Item> = emptyList(),
    val errorMessage: String? = null,
)

sealed interface SharedCatalogIntent {
    data class QueryChanged(val query: String) : SharedCatalogIntent
    data object DismissError : SharedCatalogIntent
}
