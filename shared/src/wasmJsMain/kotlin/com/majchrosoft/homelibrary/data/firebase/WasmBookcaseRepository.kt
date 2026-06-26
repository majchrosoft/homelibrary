package com.majchrosoft.homelibrary.data.firebase

import com.majchrosoft.homelibrary.domain.model.Bookcase
import com.majchrosoft.homelibrary.domain.repository.BookcaseRepository
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

internal class WasmBookcaseRepository : BookcaseRepository {
    private val db: FirebaseDbJs? get() = getFirebaseDb()
    private val utils: FirebaseDbUtilsJs? get() = getFirebaseDbUtils()

    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    override fun observeMine(ownerId: String): Flow<List<Bookcase>> =
        callbackFlow {
            Napier.d { "WasmBookcaseRepository: observeMine started for $ownerId" }
            var unsubscribe: (() -> Unit)? = null

            val job =
                launch {
                    try {
                        withDb { d, u ->
                            val path = "users/$ownerId/bookcases"
                            val reference = u.ref(d, path)

                            unsubscribe =
                                u.onValue(reference) { snapshot ->
                                    Napier.d { "WasmBookcaseRepository: onValue (bookcases) triggered for $ownerId" }
                                    val children = snapshotToMap(snapshot)
                                    val bookcases =
                                        children
                                            .mapNotNull { (key, rawValue) ->
                                                rawValue?.let {
                                                    try {
                                                        val jsonString = JSON.stringify(it)
                                                        json.decodeFromString<Bookcase>(jsonString).copy(id = key)
                                                    } catch (e: Exception) {
                                                        Napier.e(e) { "Failed to decode bookcase $key" }
                                                        null
                                                    }
                                                }
                                            }.sortedBy { it.name.lowercase() }

                                    Napier.d { "WasmBookcaseRepository: Emitting ${bookcases.size} bookcases for $ownerId" }
                                    trySend(bookcases)
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
        bookcaseId: String,
    ): Bookcase? =
        try {
            withDb { d, u ->
                suspendCoroutine { continuation ->
                    val path = "users/$ownerId/bookcases/$bookcaseId"
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
                                            val bookcase =
                                                json.decodeFromString<Bookcase>(jsonString).copy(id = bookcaseId)
                                            continuation.resume(bookcase)
                                        } catch (e: Exception) {
                                            Napier.e(e) { "Failed to decode bookcase $bookcaseId" }
                                            continuation.resume(null)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Napier.e(e) { "WasmBookcaseRepository: Error in getById callback for $bookcaseId" }
                                try {
                                    continuation.resume(null)
                                } catch (inner: Exception) {
                                    Napier.e(inner) { "WasmBookcaseRepository: Error resuming continuation for $bookcaseId" }
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

    private suspend fun <T> withDb(block: suspend (FirebaseDbJs, FirebaseDbUtilsJs) -> T): T {
        var currentDb = getFirebaseDb()
        var currentUtils = getFirebaseDbUtils()
        Napier.d { "WasmBookcaseRepository: withDb - db: ${currentDb != null}, utils: ${currentUtils != null}" }

        if (currentDb == null || currentUtils == null) {
            val startTime = Clock.System.now().toEpochMilliseconds()
            Napier.d { "WasmBookcaseRepository: withDb - waiting for Firebase Database..." }
            while ((currentDb == null || currentUtils == null) &&
                (Clock.System.now().toEpochMilliseconds() - startTime < 30000)
            ) {
                delay(500)
                currentDb = getFirebaseDb()
                currentUtils = getFirebaseDbUtils()
                Napier.d { "WasmBookcaseRepository: withDb - waiting... db: ${currentDb != null}, utils: ${currentUtils != null}" }
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
