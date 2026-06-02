package com.majchrosoft.homelibrary.domain.repository

import com.majchrosoft.homelibrary.domain.model.Bookcase
import kotlinx.coroutines.flow.Flow

/**
 * Read/write access to the user's bookcases (physical groupings: shelves, boxes,
 * rooms, ...). Each [com.majchrosoft.homelibrary.domain.model.Item] references a
 * bookcase by ID via [com.majchrosoft.homelibrary.domain.model.ItemDetails.bookcase].
 */
interface BookcaseRepository {
    fun observeMine(ownerId: String): Flow<List<Bookcase>>

    suspend fun getById(
        ownerId: String,
        bookcaseId: String,
    ): Bookcase?

    suspend fun add(
        ownerId: String,
        bookcase: Bookcase,
    ): Result<Bookcase>

    suspend fun update(
        ownerId: String,
        bookcase: Bookcase,
    ): Result<Unit>

    suspend fun delete(
        ownerId: String,
        bookcaseId: String,
    ): Result<Unit>
}
