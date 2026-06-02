package com.majchrosoft.homelibrary.data.firebase

/**
 * Single source of truth for Firebase Realtime Database paths.
 *
 * Mirrors the tree of the legacy Angular app exactly:
 *
 * ```
 * users/
 *   {uid}/
 *     profile        ← optional [User] profile snapshot
 *     bookcases/
 *       {bookcaseId}
 *     items/
 *       {itemId}/
 *         item/      ← descriptive fields (title, author, type, ...)
 *         borrow/    ← embedded borrow state
 *         createdAt
 *         updatedAt
 * catalog/
 *   items/           ← projection of shareable=true items, written by a Cloud Function
 * ```
 *
 * Keep this file in sync with `database.rules.json`.
 */
internal object FirebasePaths {
    fun userProfile(userId: String) = "users/$userId/profile"

    fun userBookcases(userId: String) = "users/$userId/bookcases"

    fun userBookcase(
        userId: String,
        bookcaseId: String,
    ) = "users/$userId/bookcases/$bookcaseId"

    fun userItems(userId: String) = "users/$userId/items"

    fun userItem(
        userId: String,
        itemId: String,
    ) = "users/$userId/items/$itemId"

    /** Public catalog projection — only items where shareable=true are mirrored here by a Cloud Function. */
    const val SHARED_CATALOG = "catalog/items"
}
