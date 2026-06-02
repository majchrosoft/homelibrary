package com.majchrosoft.homelibrary.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Type of physical or digital item tracked in the home library.
 *
 * The serialized values match the lowercase string values used by the legacy
 * Angular app and the existing Firebase Realtime Database (`users/{uid}/items/{id}/item/type`).
 */
@Serializable
enum class ItemType {
    @SerialName("book")
    BOOK,

    @SerialName("audiobook")
    AUDIOBOOK,

    @SerialName("ebook")
    EBOOK,

    @SerialName("comic")
    COMIC,

    @SerialName("magazine")
    MAGAZINE,

    @SerialName("dvd")
    DVD,

    @SerialName("bluray")
    BLURAY,

    @SerialName("vinyl")
    VINYL,

    @SerialName("boardgame")
    BOARDGAME,

    @SerialName("other")
    OTHER,
}
