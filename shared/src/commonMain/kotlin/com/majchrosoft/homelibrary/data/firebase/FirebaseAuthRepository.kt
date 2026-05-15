package com.majchrosoft.homelibrary.data.firebase

import com.majchrosoft.homelibrary.domain.model.User
import com.majchrosoft.homelibrary.domain.repository.AuthRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

internal class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
) : AuthRepository {

    override val currentUser: Flow<User?> = auth.authStateChanged.map { it?.toDomain() }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password)
        result.user?.toDomain() ?: error("Firebase returned null user after sign-in")
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String?,
    ): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password)
        val firebaseUser = result.user ?: error("Firebase returned null user after sign-up")
        displayName?.let { firebaseUser.updateProfile(displayName = it) }
        firebaseUser.toDomain(displayNameOverride = displayName)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email)
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}

private fun FirebaseUser.toDomain(displayNameOverride: String? = null): User = User(
    id = uid,
    email = email.orEmpty(),
    displayName = displayNameOverride ?: displayName,
    photoUrl = photoURL,
    sharesPublicly = false,
    // Firebase doesn't surface creation timestamp on every platform; default to "now" and let
    // the user profile document, written separately, carry the authoritative createdAt.
    createdAt = Clock.System.now(),
)
