package com.majchrosoft.homelibrary.domain.repository

import com.majchrosoft.homelibrary.domain.model.Item
import kotlinx.coroutines.flow.Flow

/**
 * Read/write access to the signed-in user's items, plus a read view over the
 * public shareable catalog populated by a Cloud Function.
 */
interface ItemRepository {
    /** Live stream of the user's library, sorted by [com.majchrosoft.homelibrary.domain.model.ItemDetails.title]. */
    fun observeMyLibrary(ownerId: String): Flow<List<Item>>

    /** Live stream of the public shareable catalog from all users. */
    fun observeSharedCatalog(query: String? = null, limit: Int = 50): Flow<List<Item>>

    suspend fun getById(ownerId: String, itemId: String): Item?
    suspend fun add(item: Item): Result<Item>
    suspend fun update(item: Item): Result<Unit>
    suspend fun delete(ownerId: String, itemId: String): Result<Unit>
}
