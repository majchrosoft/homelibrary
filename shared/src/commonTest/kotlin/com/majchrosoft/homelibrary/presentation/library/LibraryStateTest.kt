package com.majchrosoft.homelibrary.presentation.library

import com.majchrosoft.homelibrary.domain.model.Bookcase
import com.majchrosoft.homelibrary.domain.model.Item
import com.majchrosoft.homelibrary.domain.model.ItemDetails
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryStateTest {
    private fun item(
        id: String,
        title: String,
        author: String = "",
        isbn: String? = null,
        bookcase: String? = null,
    ) = Item(
        id = id,
        ownerId = "owner",
        item = ItemDetails(title = title, author = author, isbn = isbn, bookcase = bookcase),
    )

    @Test
    fun emptyQueryReturnsAllItemsUnchanged() {
        val state = LibraryState(items = listOf(item("1", "A"), item("2", "B")))
        assertEquals(2, state.filtered.size)
    }

    @Test
    fun filtersByTitleCaseInsensitively() {
        val state =
            LibraryState(
                query = "kotlin",
                items = listOf(item("1", "Kotlin in Action"), item("2", "Effective Java")),
            )
        assertEquals(listOf("Kotlin in Action"), state.filtered.map { it.item.title })
    }

    @Test
    fun filtersByAuthor() {
        val state =
            LibraryState(
                query = "Bloch",
                items =
                    listOf(
                        item("1", "Effective Java", author = "Joshua Bloch"),
                        item("2", "Clean Code", author = "Robert C. Martin"),
                    ),
            )
        assertEquals(listOf("Effective Java"), state.filtered.map { it.item.title })
    }

    @Test
    fun filtersByIsbn() {
        val state =
            LibraryState(
                query = "9781617",
                items = listOf(item("1", "X", isbn = "9781617295362"), item("2", "Y", isbn = "9780132350884")),
            )
        assertEquals(listOf("X"), state.filtered.map { it.item.title })
    }

    @Test
    fun filtersByBookcase() {
        val livingRoom = Bookcase(id = "lr", name = "Living room")
        val office = Bookcase(id = "of", name = "Office")
        val state =
            LibraryState(
                selectedBookcaseId = livingRoom.id,
                bookcases = listOf(livingRoom, office),
                items =
                    listOf(
                        item("1", "A", bookcase = "lr"),
                        item("2", "B", bookcase = "of"),
                        item("3", "C", bookcase = "lr"),
                    ),
            )
        assertEquals(listOf("A", "C"), state.filtered.map { it.item.title })
    }
}
