package com.majchrosoft.homelibrary.data.firebase

import com.majchrosoft.homelibrary.domain.model.Bookcase
import com.majchrosoft.homelibrary.domain.repository.BookcaseRepository
import dev.gitlive.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class FirebaseBookcaseRepository(
    private val database: FirebaseDatabase,
) : BookcaseRepository {
    override fun observeMine(ownerId: String): Flow<List<Bookcase>> =
        database
            .reference(FirebasePaths.userBookcases(ownerId))
            .valueEvents
            .map { snapshot ->
                snapshot.children
                    .mapNotNull { child ->
                        runCatching {
                            child.value(Bookcase.serializer()).copy(id = child.key.orEmpty())
                        }.getOrNull()
                    }.sortedBy { it.name.lowercase() }
            }

    override suspend fun getById(
        ownerId: String,
        bookcaseId: String,
    ): Bookcase? =
        runCatching {
            val snapshot =
                database
                    .reference(FirebasePaths.userBookcase(ownerId, bookcaseId))
                    .valueEvents
                    .first()
            if (!snapshot.exists) return@runCatching null
            snapshot.value(Bookcase.serializer()).copy(id = bookcaseId)
        }.getOrNull()

    override suspend fun add(
        ownerId: String,
        bookcase: Bookcase,
    ): Result<Bookcase> =
        runCatching {
            val ref = database.reference(FirebasePaths.userBookcases(ownerId)).push()
            val id = ref.key ?: error("Firebase failed to generate a push key")
            val toWrite = bookcase.copy(id = id)
            ref.setValue(Bookcase.serializer(), toWrite)
            toWrite
        }

    override suspend fun update(
        ownerId: String,
        bookcase: Bookcase,
    ): Result<Unit> =
        runCatching {
            require(bookcase.id.isNotBlank()) { "Bookcase.id is required for update" }
            database
                .reference(FirebasePaths.userBookcase(ownerId, bookcase.id))
                .setValue(Bookcase.serializer(), bookcase)
        }

    override suspend fun delete(
        ownerId: String,
        bookcaseId: String,
    ): Result<Unit> =
        runCatching {
            database.reference(FirebasePaths.userBookcase(ownerId, bookcaseId)).removeValue()
        }
}
