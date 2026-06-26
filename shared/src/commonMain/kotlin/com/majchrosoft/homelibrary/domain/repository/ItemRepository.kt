package com.majchrosoft.homelibrary.domain.repository

import com.majchrosoft.homelibrary.domain.model.Item
import kotlinx.coroutines.flow.Flow

/**
 * Read/write access to the signed-in user's items.
 */
interface ItemRepository {
    /** Live stream of the user's library, sorted by [com.majchrosoft.homelibrary.domain.model.ItemDetails.title]. */
    fun observeMyLibrary(ownerId: String): Flow<List<Item>>

    suspend fun getById(
        ownerId: String,
        itemId: String,
    ): Item?

    suspend fun add(item: Item): Result<Item>

    suspend fun update(item: Item): Result<Unit>

    suspend fun delete(
        ownerId: String,
        itemId: String,
    ): Result<Unit>
}
