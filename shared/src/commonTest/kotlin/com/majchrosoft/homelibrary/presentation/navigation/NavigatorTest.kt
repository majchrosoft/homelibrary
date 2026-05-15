package com.majchrosoft.homelibrary.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigatorTest {

    @Test
    fun startsOnLibraryAndCannotGoBack() {
        val nav = Navigator()
        assertEquals(Screen.Library, nav.current.value)
        assertFalse(nav.canGoBack)
    }

    @Test
    fun pushAdvancesAndUnlocksBack() {
        val nav = Navigator()
        nav.push(Screen.Bookcases)
        assertEquals(Screen.Bookcases, nav.current.value)
        assertTrue(nav.canGoBack)
    }

    @Test
    fun backUnwindsToPreviousScreen() {
        val nav = Navigator()
        nav.push(Screen.Bookcases)
        nav.push(Screen.BookcaseEdit("bc-1"))
        nav.back()
        assertEquals(Screen.Bookcases, nav.current.value)
        nav.back()
        assertEquals(Screen.Library, nav.current.value)
    }

    @Test
    fun backIsNoOpAtRoot() {
        val nav = Navigator()
        nav.back()
        nav.back()
        assertEquals(Screen.Library, nav.current.value)
    }

    @Test
    fun replaceSwapsTheTopWithoutGrowingTheStack() {
        val nav = Navigator()
        nav.push(Screen.ItemEdit(itemId = null))
        nav.replace(Screen.ItemDetail("abc-123"))
        assertEquals(Screen.ItemDetail("abc-123"), nav.current.value)
        nav.back()
        // Library, not the original ItemEdit — replace dropped that entry.
        assertEquals(Screen.Library, nav.current.value)
    }

    @Test
    fun popToRootUnwindsEverything() {
        val nav = Navigator()
        nav.push(Screen.Bookcases)
        nav.push(Screen.BookcaseEdit("bc-1"))
        nav.push(Screen.ItemDetail("i-1"))
        nav.popToRoot()
        assertEquals(Screen.Library, nav.current.value)
        assertFalse(nav.canGoBack)
    }
}
