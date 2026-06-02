package com.majchrosoft.homelibrary.data.firebase

import com.majchrosoft.homelibrary.domain.model.Item
import com.majchrosoft.homelibrary.domain.repository.ItemRepository
import dev.gitlive.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/**
 * Firebase Realtime Database implementation of [ItemRepository].
 *
 * Reads and writes match the legacy Angular tree at `users/{ownerId}/items/{id}`
 * with the nested `item`/`borrow` sub-objects so we can ship the KMP rewrite
 * against the existing dev DB without a data migration.
 */
internal class FirebaseItemRepository(
    private val database: FirebaseDatabase,
) : ItemRepository {
    override fun observeMyLibrary(ownerId: String): Flow<List<Item>> =
        database
            .reference(FirebasePaths.userItems(ownerId))
            .valueEvents
            .map { snapshot ->
                snapshot.children
                    .mapNotNull { child ->
                        runCatching { child.value(Item.serializer()).withId(child.key, ownerId) }
                            .getOrNull()
                    }.sortedBy { it.item.title.lowercase() }
            }

    override fun observeSharedCatalog(
        query: String?,
        limit: Int,
    ): Flow<List<Item>> =
        database
            .reference(FirebasePaths.SHARED_CATALOG)
            .orderByChild("item/title")
            .limitToFirst(limit)
            .valueEvents
            .map { snapshot ->
                snapshot.children
                    .mapNotNull { child ->
                        runCatching { child.value(Item.serializer()).withId(child.key, null) }
                            .getOrNull()
                    }.filter { item ->
                        query.isNullOrBlank() ||
                            item.item.title.contains(query, ignoreCase = true) ||
                            item.item.author.contains(query, ignoreCase = true)
                    }
            }

    override suspend fun getById(
        ownerId: String,
        itemId: String,
    ): Item? =
        runCatching {
            val snapshot =
                database
                    .reference(FirebasePaths.userItem(ownerId, itemId))
                    .valueEvents
                    .first()
            if (!snapshot.exists) return@runCatching null
            snapshot.value(Item.serializer()).withId(itemId, ownerId)
        }.getOrNull()

    override suspend fun add(item: Item): Result<Item> =
        runCatching {
            require(item.ownerId.isNotBlank()) { "Item.ownerId is required" }
            val now = Clock.System.now()
            val ref = database.reference(FirebasePaths.userItems(item.ownerId)).push()
            val id = ref.key ?: error("Firebase failed to generate a push key")
            val toWrite = item.copy(id = id, createdAt = now, updatedAt = now)
            ref.setValue(Item.serializer(), toWrite)
            toWrite
        }

    override suspend fun update(item: Item): Result<Unit> =
        runCatching {
            require(item.ownerId.isNotBlank() && item.id.isNotBlank()) {
                "Item.id and Item.ownerId are required for update"
            }
            val toWrite = item.copy(updatedAt = Clock.System.now())
            database
                .reference(FirebasePaths.userItem(item.ownerId, item.id))
                .setValue(Item.serializer(), toWrite)
        }

    override suspend fun delete(
        ownerId: String,
        itemId: String,
    ): Result<Unit> =
        runCatching {
            database.reference(FirebasePaths.userItem(ownerId, itemId)).removeValue()
        }
}

/**
 * Firebase RTDB stores the entity *under* its push-key, so the deserialised
 * payload doesn't carry the id. Patch it back in (and ownerId, when known) so
 * the rest of the app can rely on a populated [Item].
 */
private fun Item.withId(
    id: String?,
    ownerId: String?,
): Item =
    copy(
        id = id ?: this.id,
        ownerId = ownerId ?: this.ownerId,
    )
