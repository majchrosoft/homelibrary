package com.majchrosoft.homelibrary.data.firebase

import com.majchrosoft.homelibrary.domain.SessionManager
import com.majchrosoft.homelibrary.domain.model.User
import com.majchrosoft.homelibrary.domain.repository.AuthRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

internal class WasmAuthRepository(
    private val sessionManager: SessionManager,
) : AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(sessionManager.user)
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    init {
        Napier.d("WasmAuthRepository: init", tag = "DEBUG app")
        Napier.d("WasmAuthRepository: initialized with user: ${sessionManager.user?.id}", tag = "DEBUG app")
        observeFirebaseAuthState()
    }

    private fun observeFirebaseAuthState() {
        Napier.d("WasmAuthRepository: Starting observeFirebaseAuthState", tag = "DEBUG app")
        callbackFlow {
            Napier.d("WasmAuthRepository: callbackFlow started", tag = "DEBUG app")
            var unsubscribe: (() -> Unit)? = null
            var firstEmission = true
            fun trySubscribe(): Boolean {
                Napier.d("WasmAuthRepository: trySubscribe called", tag = "DEBUG app")
                val a = getFirebaseAuth()
                val u = getFirebaseAuthUtils()
                if (a != null && u != null) {
                    Napier.d("WasmAuthRepository: Firebase Auth ready, subscribing", tag = "DEBUG app")
                    unsubscribe = u.onAuthStateChanged(a) { jsUser ->
                        val user = jsUser?.toDomain()
                        Napier.d("WasmAuthRepository: onAuthStateChanged emitted user: ${user?.id}", tag = "DEBUG app")
                        if (firstEmission) {
                            firstEmission = false
                            if (user == null) {
                                Napier.d("WasmAuthRepository: First emission is null. Delaying to see if it's a false negative.", tag = "DEBUG app")
                                launch {
                                    delay(2000)
                                    // Only emit null if it hasn't been emitted yet or changed
                                    if (_currentUser.value == sessionManager.user) {
                                        Napier.d("WasmAuthRepository: Still no Firebase user after 2s delay. Emitting null.", tag = "DEBUG app")
                                        trySend(user) // Use the user from closure which is null
                                    } else {
                                        Napier.d("WasmAuthRepository: User already updated during delay, skipping null emission", tag = "DEBUG app")
                                    }
                                }
                            } else {
                                Napier.d("WasmAuthRepository: First emission with user: ${user.id}", tag = "DEBUG app")
                                trySend(user)
                            }
                        } else {
                            Napier.d("WasmAuthRepository: Subsequent emission with user: ${user?.id}", tag = "DEBUG app")
                            trySend(user)
                        }
                    }
                    // Also check immediate currentUser if it's already there
                    val immediateUser = a.currentUser?.toDomain()
                    if (immediateUser != null) {
                        Napier.d("WasmAuthRepository: Found immediate currentUser: ${immediateUser.id}", tag = "DEBUG app")
                        firstEmission = false
                        trySend(immediateUser)
                    } else {
                        Napier.d("WasmAuthRepository: No immediate currentUser found", tag = "DEBUG app")
                    }
                    return true
                }
                Napier.d("WasmAuthRepository: Firebase Auth JS not ready yet (a: ${a != null}, u: ${u != null})", tag = "DEBUG app")
                return false
            }

            if (!trySubscribe()) {
                Napier.d("WasmAuthRepository: Firebase Auth not ready, starting retry loop", tag = "DEBUG app")
                val job = launch {
                    val startTime = Clock.System.now().toEpochMilliseconds()
                    while ((Clock.System.now().toEpochMilliseconds() - startTime < 30000)) {
                        delay(500)
                        if (trySubscribe()) break
                    }
                }
                awaitClose {
                    Napier.d("WasmAuthRepository: callbackFlow closing (retry path)", tag = "DEBUG app")
                    job.cancel()
                    unsubscribe?.invoke()
                }
            } else {
                awaitClose { 
                    Napier.d("WasmAuthRepository: callbackFlow closing (normal path)", tag = "DEBUG app")
                    unsubscribe?.invoke() 
                }
            }
        }.onEach { user ->
            Napier.d("WasmAuthRepository: flow emission - Updating _currentUser to ${user?.id}", tag = "DEBUG app")
            // Update the Flow
            _currentUser.value = user
            sessionManager.user = user
        }.catch { e ->
            Napier.e("WasmAuthRepository: Error in observeFirebaseAuthState flow", e, tag = "DEBUG app")
        }.launchIn(CoroutineScope(Dispatchers.Main.immediate))
    }

    override suspend fun getBearerToken(): String? =
        try {
            withAuth { a, u ->
                val user = a.currentUser
                user?.let { u.getIdToken(it).awaitNonnull().toString() }
            }
        } catch (e: Exception) {
            sessionManager.bearerToken
        }

    private suspend fun <T> withAuth(block: suspend (FirebaseAuthJs, FirebaseAuthUtilsJs) -> T): T {
        val startTime = Clock.System.now().toEpochMilliseconds()
        var currentAuth = getFirebaseAuth()
        var currentUtils = getFirebaseAuthUtils()

        if (currentAuth == null || currentUtils == null) {
            Napier.d { "Waiting for Firebase Auth initialization... (auth: ${currentAuth != null}, utils: ${currentUtils != null})" }
        }

        while ((currentAuth == null || currentUtils == null) &&
            (Clock.System.now().toEpochMilliseconds() - startTime < 30000)
        ) {
            delay(500)
            currentAuth = getFirebaseAuth()
            currentUtils = getFirebaseAuthUtils()
        }

        if (currentAuth == null || currentUtils == null) {
            val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
            val errorMsg =
                "Firebase Auth not initialized after ${elapsed}ms (auth: ${currentAuth != null}, " +
                    "utils: ${currentUtils != null}). Check if firebase-config.js is loaded correctly."
            Napier.e { errorMsg }
            throw Exception(errorMsg)
        }

        return block(currentAuth, currentUtils)
    }

    override suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<User> =
        try {
            withAuth { a, u ->
                val credential = u.signInWithEmailAndPassword(a, email, password).awaitNonnull()
                val firebaseUser = credential.user ?: throw Exception("Firebase returned null user")
                val token = u.getIdToken(firebaseUser).awaitNonnull().toString()
                sessionManager.bearerToken = token
                val user = firebaseUser.toDomain()
                Result.success(user)
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String?,
    ): Result<User> =
        try {
            withAuth { a, u ->
                val credential = u.createUserWithEmailAndPassword(a, email, password).awaitNonnull()
                val firebaseUser = credential.user!! ?: throw Exception("Firebase returned null user")
                if (displayName != null) {
                    val profile = createJsObject()
                    setDisplayName(profile, displayName)
                    u.updateProfile(firebaseUser, profile).await()
                }
                val token = u.getIdToken(firebaseUser).awaitNonnull().toString()
                sessionManager.bearerToken = token
                Result.success(firebaseUser.toDomain(displayNameOverride = displayName))
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        try {
            withAuth { a, u ->
                u.sendPasswordResetEmail(a, email).await()
                Result.success(Unit)
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }

    override suspend fun signOut() {
        try {
            withAuth { a, u ->
                u.signOut(a).await()
            }
            sessionManager.clear()
            _currentUser.value = null
        } catch (e: Throwable) {
            Napier.e(e) { "Failed to sign out" }
        }
    }
}

private fun FirebaseUserJs.toDomain(displayNameOverride: String? = null): User =
    User(
        id = uid,
        email = email.orEmpty(),
        displayName = displayNameOverride ?: displayName,
        photoUrl = photoURL,
        sharesPublicly = false,
        createdAt = Clock.System.now(),
    )

private fun createJsObject(): JsAny = js("({ displayName: null })")

private fun setDisplayName(
    obj: JsAny,
    name: String,
) {
    js("obj.displayName = name")
}
