package com.majchrosoft.homelibrary.data.firebase

import com.majchrosoft.homelibrary.domain.model.Item
import com.majchrosoft.homelibrary.domain.repository.ItemRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    /**
     * Reads all children of a Firebase DataSnapshot into a Kotlin Map.
     *
     * Firebase DataSnapshot is a plain JS object with no Symbol.iterator.
     * Kotlin/Wasm requires js() to be a single expression, so we do the
     * entire iteration on the JS side in one shot, returning a plain JS
     * object that we turn into a Kotlin Map without any additional js().
     */
    @Suppress("UNCHECKED_CAST")
    private fun readSnapshotChildren(snapshot: FirebaseDataSnapshotJs): Map<String, JsAny?> {
        // Single js() call: returns a plain JS object { key: JSON.stringify(value), ... }
        val result: JsAny = js("Object.fromEntries(Object.entries(snapshot).map(([k, v]) => [k, JSON.stringify(v)]))")
        if (result == null) return emptyMap()
        val entries: Array<String> = (js("Object.keys(result)") as? js.Array<String>) ?: js("Object.keys(result)") as js.Array<String>
        val map = mutableMapOf<String, JsAny?>()
        for (key in entries) {
            val jsonVal = js("result[key]") as? String
            map[key] = if (jsonVal != null) JSON.parse(jsonVal) else null
        }
        return map
    }

    private suspend fun <T> withDb(block: suspend (FirebaseDbJs, FirebaseDbUtilsJs) -> T): T {
        val startTime = Clock.System.now().toEpochMilliseconds()
        var currentDb = getFirebaseDb()
        var currentUtils = getFirebaseDbUtils()

        while ((currentDb == null || currentUtils == null) &&
            (Clock.System.now().toEpochMilliseconds() - startTime < 30000)
        ) {
            delay(500)
            currentDb = getFirebaseDb()
            currentUtils = getFirebaseDbUtils()
        }

        if (currentDb == null || currentUtils == null) {
            val errorMsg = "Firebase Database not initialized after 30s"
            Napier.e { errorMsg }
            throw Exception(errorMsg)
        }

        return block(currentDb, currentUtils)
    }

    override fun observeMyLibrary(ownerId: String): Flow<List<Item>> =
        callbackFlow {
            Napier.d { "WasmItemRepository: observeMyLibrary started for $ownerId" }
            var unsubscribe: (() -> Unit)? = null
            val job =
                launch {
                    try {
                        withDb { d, u ->
                            Napier.d { "WasmItemRepository: withDb ready for observeMyLibrary ($ownerId)" }
                            val path = "users/$ownerId/items"
                            val reference = u.ref(d, path)
                            unsubscribe =
                                u.onValue(reference) { snapshot ->
                                    Napier.d { "WasmItemRepository: onValue (items) triggered for $ownerId" }
                                    val children = readSnapshotChildren(snapshot)
                                    val items = children.mapNotNull { (key, rawValue) ->
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
                job.cancel()
                unsubscribe?.invoke()
            }
        }

    override fun observeSharedCatalog(
        query: String?,
        limit: Int,
    ): Flow<List<Item>> =
        callbackFlow {
            var unsubscribe: (() -> Unit)? = null
            val job =
                launch {
                    try {
                        withDb { d, u ->
                            val path = "catalog/items"
                            val reference = u.ref(d, path)
                            val q = u.query(reference, u.orderByChild("item/title"), u.limitToFirst(limit))

                            unsubscribe =
                                u.onValue(q) { snapshot ->
                                    val children = readSnapshotChildren(snapshot)
                                    val items = children.mapNotNull { (key, rawValue) ->
                                        rawValue?.let {
                                            try {
                                                val jsonString = JSON.stringify(it)
                                                val item = json.decodeFromString<Item>(jsonString).copy(id = key)
                                                if (query.isNullOrBlank() ||
                                                    item.item.title.contains(query, ignoreCase = true) ||
                                                    item.item.author.contains(query, ignoreCase = true)
                                                ) item else null
                                            } catch (e: Exception) {
                                                Napier.e(e) { "Failed to decode catalog item $key" }
                                                null
                                            }
                                        }
                                    }
                                    trySend(items)
                                }
                        }
                    } catch (e: Exception) {
                        trySend(emptyList())
                        close(e)
                    }
                }
            awaitClose {
                job.cancel()
                unsubscribe?.invoke()
            }
        }

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
}
