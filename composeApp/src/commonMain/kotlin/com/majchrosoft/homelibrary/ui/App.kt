package com.majchrosoft.homelibrary.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.majchrosoft.homelibrary.presentation.auth.AuthViewModel
import com.majchrosoft.homelibrary.presentation.navigation.Navigator
import com.majchrosoft.homelibrary.presentation.navigation.Screen
import com.majchrosoft.homelibrary.ui.auth.SignInScreen
import com.majchrosoft.homelibrary.ui.bookcase.BookcaseEditScreen
import com.majchrosoft.homelibrary.ui.bookcase.BookcasesScreen
import com.majchrosoft.homelibrary.ui.item.ItemDetailScreen
import com.majchrosoft.homelibrary.ui.item.ItemEditScreen
import com.majchrosoft.homelibrary.ui.library.libraryScreen
import com.majchrosoft.homelibrary.ui.profile.ProfileScreen
import com.majchrosoft.homelibrary.ui.theme.HomeLibraryTheme
import org.koin.compose.koinInject

/**
 * Root composable shared across Android and Web. The iOS app uses SwiftUI and
 * does NOT use this — it consumes the shared ViewModels and Navigator directly
 * via the exported `Shared` framework.
 *
 * Routing is state-based: the [Navigator] is a Koin singleton holding a
 * `StateFlow<Screen>` and we just swap composables when [Screen] changes.
 * No `androidx.navigation` here — keeping it pure-common so Web (once the
 * wasmJs target is re-enabled) gets the same logic with zero changes.
 */
@Composable
fun App() {
    HomeLibraryTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                val authViewModel = koinInject<AuthViewModel>()
                val authState by authViewModel.state.collectAsState()

                if (authState.user == null) {
                    SignInScreen(viewModel = authViewModel)
                } else {
                    AuthedRoot()
                }
            }
        }
    }
}

@Composable
private fun AuthedRoot() {
    val navigator = koinInject<Navigator>()
    val current by navigator.current.collectAsState()

    when (val screen = current) {
        Screen.Library -> libraryScreen()
        Screen.Bookcases -> BookcasesScreen()
        is Screen.BookcaseEdit -> BookcaseEditScreen(bookcaseId = screen.bookcaseId)
        is Screen.ItemDetail -> ItemDetailScreen(itemId = screen.itemId)
        is Screen.ItemEdit -> ItemEditScreen(itemId = screen.itemId)
        Screen.Profile -> ProfileScreen()
    }
}
