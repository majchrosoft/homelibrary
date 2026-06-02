package com.majchrosoft.homelibrary.data.firebase

import com.majchrosoft.homelibrary.domain.model.Bookcase
import com.majchrosoft.homelibrary.domain.repository.BookcaseRepository
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
import com.majchrosoft.homelibrary.data.firebase.JSON

/**
 * Iterates over all children of a Firebase [FirebaseDataSnapshotJs] in Kotlin/Wasm.
 *
 * [FirebaseDataSnapshotJs] is a plain JavaScript object. We use top-level [js] calls
 * (which Kotlin/Wasm allows at top-level function bodies and property initializers) to access
 * child values by key.
 */
typealias SnapshotChildAction = (key: String, value: JsAny?) -> Boolean
private fun forEachSnapshotChild(
    snapshot: FirebaseDataSnapshotJs,
    action: SnapshotChildAction,
): Boolean {
    val keys: js.Array<String> = js("Object.keys(snapshot)")
    var i = 0
    val length = keys.length
    var stopped = false
    while (i < length && !stopped) {
        val key = keys[i++]
        val jsValue: Any? = js("snapshot[key]")
        val value: JsAny? = jsValue as? JsAny
        val cont: Boolean = action(key, value)
        stopped = cont == false
    }
    return !stopped
}

internal class WasmBookcaseRepository : BookcaseRepository {
    private val db: FirebaseDbJs? get() = getFirebaseDb()
    private val utils: FirebaseDbUtilsJs? get() = getFirebaseDbUtils()

    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
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

    override fun observeMine(ownerId: String): Flow<List<Bookcase>> =
        callbackFlow {
            Napier.d { "WasmBookcaseRepository: observeMine started for $ownerId" }
            var unsubscribe: (() -> Unit)? = null
            val job =
                launch {
                    try {
                        withDb { d, u ->
                            Napier.d { "WasmBookcaseRepository: withDb ready for observeMine ($ownerId)" }
                            val path = "users/$ownerId/bookcases"
                            val reference = u.ref(d, path)
                            unsubscribe =
                                u.onValue(reference) { snapshot ->
                                    Napier.d { "WasmBookcaseRepository: onValue (bookcases) triggered for $ownerId" }
                                    val cases = mutableListOf<Bookcase>()
                                    forEachSnapshotChild(snapshot) { key, rawValue ->
                                        if (rawValue != null) {
                                            try {
                                                val jsonString = JSON.stringify(rawValue)
                                                val bookcase =
                                                    json.decodeFromString<Bookcase>(jsonString).copy(id = key)
                                                cases.add(bookcase)
                                            } catch (e: Exception) {
                                                Napier.e(e) { "Failed to decode bookcase $key" }
                                            }
                                        }
                                        true
                                    }
                                    Napier.d { "WasmBookcaseRepository: Emitting ${cases.size} bookcases for $ownerId" }
                                    trySend(cases.sortedBy { it.name.lowercase() })
                                }
                        }
                    } catch (e: Exception) {
                        Napier.e(e) { "WasmBookcaseRepository: Error in observeMine for $ownerId" }
                        trySend(emptyList())
                        close(e)
                    }
                }
            awaitClose {
                Napier.d { "WasmBookcaseRepository: observeMine closed for $ownerId" }
                job.cancel()
                unsubscribe?.invoke()
            }
        }

    override suspend fun getById(
        ownerId: String,
        bookcaseId: String,
    ): Bookcase? =
        try {
            withDb { d, u ->
                kotlin.coroutines.suspendCoroutine { continuation ->
                    val path = "users/$ownerId/bookcases/$bookcaseId"
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
                                        val bookcase = json.decodeFromString<Bookcase>(jsonString).copy(id = bookcaseId)
                                        continuation.resume(bookcase)
                                    } catch (e: Exception) {
                                        Napier.e(e) { "Failed to decode bookcase $bookcaseId" }
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

    override suspend fun add(
        ownerId: String,
        bookcase: Bookcase,
    ): Result<Bookcase> =
        try {
            withDb { d, u ->
                val path = "users/$ownerId/bookcases"
                val listRef = u.ref(d, path)
                val newRef = u.push(listRef)
                val id = newRef.key ?: throw Exception("Failed to get push key")
                val toWrite = bookcase.copy(id = id)
                val jsonString = Json.encodeToString(Bookcase.serializer(), toWrite)
                val jsObject = JSON.parse(jsonString)
                u.set(newRef, jsObject).await()
                Result.success(toWrite)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun update(
        ownerId: String,
        bookcase: Bookcase,
    ): Result<Unit> =
        try {
            withDb { d, u ->
                val path = "users/$ownerId/bookcases/${bookcase.id}"
                val ref = u.ref(d, path)
                val jsonString = Json.encodeToString(Bookcase.serializer(), bookcase)
                val jsObject = JSON.parse(jsonString)
                u.set(ref, jsObject).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun delete(
        ownerId: String,
        bookcaseId: String,
    ): Result<Unit> =
        try {
            withDb { d, u ->
                val path = "users/$ownerId/bookcases/$bookcaseId"
                val ref = u.ref(d, path)
                u.remove(ref).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
