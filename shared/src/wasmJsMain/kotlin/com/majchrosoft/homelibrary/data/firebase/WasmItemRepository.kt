package com.majchrosoft.homelibrary.data.firebase

import com.majchrosoft.homelibrary.domain.model.Item
import com.majchrosoft.homelibrary.domain.repository.ItemRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class WasmItemRepository : ItemRepository {
    private val db: FirebaseDbJs? get() = getFirebaseDb()
    private val utils: FirebaseDbUtilsJs? get() = getFirebaseDbUtils()

    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    override fun observeMyLibrary(ownerId: String): Flow<List<Item>> =
        callbackFlow {
            Napier.d { "WasmItemRepository: observeMyLibrary started for $ownerId" }
            var unsubscribe: (() -> Unit)? = null

            val job =
                launch {
                    try {
                        withDb { d, u ->
                            val path = "users/$ownerId/items"
                            val reference = u.ref(d, path)

                            unsubscribe =
                                u.onValue(reference) { snapshot ->
                                    try {
                                        Napier.d { "WasmItemRepository: onValue (items) triggered for $ownerId" }
                                        val exists = snapshot.exists()
                                        Napier.d { "WasmItemRepository: snapshot exists: $exists" }
                                        val children = snapshotToMap(snapshot)
                                        val items =
                                            children
                                                .mapNotNull { (key, rawValue) ->
                                                    rawValue?.let {
                                                        try {
                                                            val jsonString = JSON.stringify(it)
                                                            json.decodeFromString<Item>(jsonString).copy(
                                                                id = key,
                                                                ownerId = ownerId,
                                                            )
                                                        } catch (e: Exception) {
                                                            Napier.e(e) { "Failed to decode item $key" }
                                                            null
                                                        }
                                                    }
                                                }.sortedBy { it.item.title.lowercase() }

                                        Napier.d { "WasmItemRepository: Emitting ${items.size} items for $ownerId" }
                                        trySend(items)
                                    } catch (e: Exception) {
                                        Napier.e(e) { "WasmItemRepository: Error processing snapshot for $ownerId" }
                                        trySend(emptyList())
                                    }
                                }
                        }
                    } catch (e: Exception) {
                        Napier.e(e) { "WasmItemRepository: Error in observeMyLibrary for $ownerId" }
                        trySend(emptyList())
                        close(e)
                    }
                }

            awaitClose {
                Napier.d { "WasmItemRepository: observeMyLibrary closed for $ownerId" }
                unsubscribe?.invoke()
            }
        }.shareIn(
            scope = kotlinx.coroutines.MainScope(),
            replay = 1,
            started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
        ).stateIn(
            scope = kotlinx.coroutines.MainScope(),
            initialValue = emptyList(),
            started =
                kotlinx.coroutines.flow.SharingStarted
                    .WhileSubscribed(5000),
        )

    override suspend fun getById(
        ownerId: String,
        itemId: String,
    ): Item? =
        try {
            withDb { d, u ->
                suspendCoroutine { continuation ->
                    val path = "users/$ownerId/items/$itemId"
                    val reference = u.ref(d, path)
                    var unsubscribe: (() -> Unit)? = null
                    unsubscribe =
                        u.onValue(reference) { snapshot ->
                            try {
                                unsubscribe?.invoke()
                                if (!snapshot.exists()) {
                                    continuation.resume(null)
                                } else {
                                    val rawValue = snapshot.getValue()
                                    if (rawValue == null) {
                                        continuation.resume(null)
                                    } else {
                                        try {
                                            val jsonString = JSON.stringify(rawValue)
                                            val item =
                                                json.decodeFromString<Item>(jsonString).copy(
                                                    id = itemId,
                                                    ownerId = ownerId,
                                                )
                                            continuation.resume(item)
                                        } catch (e: Exception) {
                                            Napier.e(e) { "Failed to decode item $itemId" }
                                            continuation.resume(null)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Napier.e(e) { "WasmItemRepository: Error in getById callback for $itemId" }
                                try {
                                    continuation.resume(null)
                                } catch (inner: Exception) {
                                    Napier.e(inner) { "WasmItemRepository: Error resuming continuation for $itemId" }
                                }
                            }
                        }
                }
            }
        } catch (e: Exception) {
            null
        }

    override suspend fun add(item: Item): Result<Item> =
        try {
            withDb { d, u ->
                val path = "users/${item.ownerId}/items"
                val listRef = u.ref(d, path)
                val newRef = u.push(listRef)
                val id = newRef.key ?: throw Exception("Failed to get push key")
                val toWrite = item.copy(id = id)
                val jsonString = Json.encodeToString(Item.serializer(), toWrite)
                val jsObject = JSON.parse(jsonString)
                u.set(newRef, jsObject).await()
                Result.success(toWrite)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun update(item: Item): Result<Unit> =
        try {
            withDb { d, u ->
                val path = "users/${item.ownerId}/items/${item.id}"
                val ref = u.ref(d, path)
                val jsonString = Json.encodeToString(Item.serializer(), item)
                val jsObject = JSON.parse(jsonString)
                u.set(ref, jsObject).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun delete(
        ownerId: String,
        itemId: String,
    ): Result<Unit> =
        try {
            withDb { d, u ->
                val path = "users/$ownerId/items/$itemId"
                val ref = u.ref(d, path)
                u.remove(ref).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    private suspend fun <T> withDb(block: suspend (FirebaseDbJs, FirebaseDbUtilsJs) -> T): T {
        var currentDb = getFirebaseDb()
        var currentUtils = getFirebaseDbUtils()
        Napier.d { "WasmItemRepository: withDb - db: ${currentDb != null}, utils: ${currentUtils != null}" }

        if (currentDb == null || currentUtils == null) {
            val startTime = Clock.System.now().toEpochMilliseconds()
            Napier.d { "WasmItemRepository: withDb - waiting for Firebase Database..." }
            while ((currentDb == null || currentUtils == null) &&
                (Clock.System.now().toEpochMilliseconds() - startTime < 30000)
            ) {
                delay(500)
                currentDb = getFirebaseDb()
                currentUtils = getFirebaseDbUtils()
                Napier.d { "WasmItemRepository: withDb - waiting... db: ${currentDb != null}, utils: ${currentUtils != null}" }
            }
        }

        if (currentDb == null || currentUtils == null) {
            val errorMsg = "Firebase Database not initialized after 30s"
            Napier.e { errorMsg }
            throw Exception(errorMsg)
        }

        return block(currentDb, currentUtils)
    }
}
