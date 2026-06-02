package com.majchrosoft.homelibrary.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Physical condition of an item.
 *
 * Stored as a lowercase string under `users/{uid}/items/{id}/item/quality` in the
 * existing Firebase Realtime Database. The legacy Angular app uses the field name
 * `quality` (not `condition`) — we keep that to avoid a data migration.
 */
@Serializable
enum class ItemQuality {
    @SerialName("new")
    NEW,

    @SerialName("like_new")
    LIKE_NEW,

    @SerialName("good")
    GOOD,

    @SerialName("fair")
    FAIR,

    @SerialName("poor")
    POOR,

    @SerialName("damaged")
    DAMAGED,
}
