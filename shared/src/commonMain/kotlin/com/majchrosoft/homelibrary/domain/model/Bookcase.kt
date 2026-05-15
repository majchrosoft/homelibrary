package com.majchrosoft.homelibrary.domain.model

import kotlinx.serialization.Serializable

/**
 * A physical bookcase / shelf / box that groups items by location.
 *
 * Stored at `users/{uid}/bookcases/{id}` in the Firebase Realtime Database, exactly
 * matching the structure of the legacy Angular app. Items reference the bookcase
 * they live in via [ItemDetails.bookcase], which holds this [id].
 */
@Serializable
data class Bookcase(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val location: String? = null,
)
