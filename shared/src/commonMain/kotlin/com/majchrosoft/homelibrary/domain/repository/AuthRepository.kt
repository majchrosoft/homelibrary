package com.majchrosoft.homelibrary.domain.repository

import com.majchrosoft.homelibrary.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Emits the currently signed-in user, or null when signed out. Hot, multi-subscriber-safe. */
    val currentUser: Flow<User?>

    suspend fun getBearerToken(): String?

    suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<User>

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String?,
    ): Result<User>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    suspend fun signOut()
}
