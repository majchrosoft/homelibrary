package com.majchrosoft.homelibrary.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * A registered user of the Home Library platform.
 *
 * Stored at `users/{uid}/profile` in the Firebase Realtime Database. The library
 * data (bookcases / items) lives at sibling paths under `users/{uid}/...` — see
 * [com.majchrosoft.homelibrary.data.firebase.FirebasePaths].
 */
@Serializable
data class User(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
    /** Whether this user makes any of their items shareable on the public catalog. */
    val sharesPublicly: Boolean = false,
    val createdAt: Instant? = null,
)
