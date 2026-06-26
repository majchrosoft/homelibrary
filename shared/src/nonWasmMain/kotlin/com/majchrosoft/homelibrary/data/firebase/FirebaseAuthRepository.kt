package com.majchrosoft.homelibrary.data.firebase

import com.majchrosoft.homelibrary.domain.SessionManager
import com.majchrosoft.homelibrary.domain.model.User
import com.majchrosoft.homelibrary.domain.repository.AuthRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock

internal class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val sessionManager: SessionManager,
) : AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(sessionManager.user)
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    init {
        auth.authStateChanged
            .map { it?.toDomain() }
            .onEach { user ->
                _currentUser.value = user
                sessionManager.user = user
                // If user is null, sessionManager.user = null will clear it.
                // Token update is handled on sign-in or can be refreshed here if needed.
            }.launchIn(CoroutineScope(Dispatchers.Main))
    }

    override suspend fun getBearerToken(): String? = auth.currentUser?.getIdToken(forceRefresh = false)

    override suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<User> =
        runCatching {
            val result = auth.signInWithEmailAndPassword(email, password)
            val firebaseUser = result.user ?: error("Firebase returned null user after sign-in")
            val token = firebaseUser.getIdToken(forceRefresh = true)
            sessionManager.bearerToken = token
            firebaseUser.toDomain()
        }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String?,
    ): Result<User> =
        runCatching {
            val result = auth.createUserWithEmailAndPassword(email, password)
            val firebaseUser = result.user ?: error("Firebase returned null user after sign-up")
            displayName?.let { firebaseUser.updateProfile(displayName = it) }
            val token = firebaseUser.getIdToken(forceRefresh = true)
            sessionManager.bearerToken = token
            firebaseUser.toDomain(displayNameOverride = displayName)
        }

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        runCatching {
            auth.sendPasswordResetEmail(email)
        }

    override suspend fun signOut() {
        auth.signOut()
        sessionManager.clear()
        _currentUser.value = null
    }
}

private fun FirebaseUser.toDomain(displayNameOverride: String? = null): User =
    User(
        id = uid,
        email = email.orEmpty(),
        displayName = displayNameOverride ?: displayName,
        photoUrl = photoURL,
        sharesPublicly = false,
        // Firebase doesn't surface creation timestamp on every platform; default to "now" and let
        // the user profile document, written separately, carry the authoritative createdAt.
        createdAt = Clock.System.now(),
    )
