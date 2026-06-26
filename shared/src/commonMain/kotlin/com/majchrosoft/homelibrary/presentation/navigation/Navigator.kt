package com.majchrosoft.homelibrary.presentation.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cross-platform screen graph. Kept in [shared] so Android (Compose), iOS
 * (SwiftUI) and the legacy Angular bridge can all agree on the same set of
 * destinations.
 *
 * We deliberately avoid pulling in `androidx.navigation-compose` here — it's
 * Android-only and a Wasm-compatible KMP navigation library would be one more
 * alpha-stage dependency. State-based navigation through a [Navigator] is
 * straightforward to reason about and trivial to unit-test.
 */
sealed class Screen {
    /**
     * Sealed **class** (not interface) on purpose: Kotlin/Native exports a
     * sealed class as a regular Swift class hierarchy whose subtypes are all
     * `AnyObject`, which is required by [com.majchrosoft.homelibrary.StateFlowCollectorHelper]
     * and the SwiftUI `StateFlowPublisher<Output: AnyObject>` bridge. A sealed
     * interface would export as a non-`AnyObject` Swift protocol and break
     * `StateFlowPublisher<Screen>`.
     *
     * The signed-in user's own library — list with search + bookcase filter.
     */
    data object Library : Screen()

    /** Detail view for a single item. */
    data class ItemDetail(
        val itemId: String,
    ) : Screen()

    /** Add (when [itemId] is null) or edit an item. */
    data class ItemEdit(
        val itemId: String? = null,
    ) : Screen()

    /** Manage bookcases (list / add / edit / delete). */
    data object Bookcases : Screen()

    /** Add (when [bookcaseId] is null) or edit a bookcase. */
    data class BookcaseEdit(
        val bookcaseId: String? = null,
    ) : Screen()

    /** User profile + settings + sign out. */
    data object Profile : Screen()
}

/**
 * Tiny stack-backed navigator. One [current] StateFlow, plus [push] / [back]
 * / [replace] operations. Hosts subscribe to [current] and swap composables /
 * views accordingly.
 *
 * Registered as a Koin `single` so all three platform hosts pull the same
 * instance.
 */
class Navigator(
    initial: Screen = Screen.Library,
) {
    private val stack: ArrayDeque<Screen> = ArrayDeque<Screen>().apply { addLast(initial) }
    private val _current: MutableStateFlow<Screen> = MutableStateFlow(initial)
    val current: StateFlow<Screen> = _current.asStateFlow()

    /** Whether [back] would unwind something other than the root. */
    val canGoBack: Boolean get() = stack.size > 1

    fun push(screen: Screen) {
        stack.addLast(screen)
        _current.value = screen
    }

    /** Replaces the current top of the stack — useful for "edit → detail" transitions. */
    fun replace(screen: Screen) {
        if (stack.isNotEmpty()) stack.removeLast()
        stack.addLast(screen)
        _current.value = screen
    }

    /** Pops one entry; no-op when only the root remains. */
    fun back() {
        if (stack.size <= 1) return
        stack.removeLast()
        _current.value = stack.last()
    }

    /** Drops everything except the root [Library] screen. */
    fun popToRoot() {
        while (stack.size > 1) stack.removeLast()
        _current.value = stack.last()
    }
}
