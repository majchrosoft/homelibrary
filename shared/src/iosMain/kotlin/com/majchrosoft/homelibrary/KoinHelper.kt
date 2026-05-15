package com.majchrosoft.homelibrary

import com.majchrosoft.homelibrary.presentation.auth.AuthViewModel
import com.majchrosoft.homelibrary.presentation.bookcase.BookcaseEditViewModel
import com.majchrosoft.homelibrary.presentation.bookcase.BookcasesViewModel
import com.majchrosoft.homelibrary.presentation.catalog.SharedCatalogViewModel
import com.majchrosoft.homelibrary.presentation.item.ItemDetailViewModel
import com.majchrosoft.homelibrary.presentation.item.ItemEditViewModel
import com.majchrosoft.homelibrary.presentation.library.LibraryViewModel
import com.majchrosoft.homelibrary.presentation.navigation.Navigator
import com.majchrosoft.homelibrary.presentation.profile.ProfileViewModel
import org.koin.mp.KoinPlatform
import org.koin.core.parameter.parametersOf

/**
 * Top-level Koin resolution helpers exposed to Swift. Top-level functions become
 * `KoinHelperKt.resolveAuthViewModel()` / `KoinHelperKt.resolveLibraryViewModel()`
 * — that's what the SwiftUI hosts call.
 *
 * Keep this file the single Swift entry point for resolving shared ViewModels.
 */
@Suppress("unused") // Called from Swift.
fun resolveAuthViewModel(): AuthViewModel = KoinPlatform.getKoin().get()

@Suppress("unused") // Called from Swift.
fun resolveLibraryViewModel(): LibraryViewModel = KoinPlatform.getKoin().get()

@Suppress("unused") // Called from Swift.
fun resolveBookcasesViewModel(): BookcasesViewModel = KoinPlatform.getKoin().get()

@Suppress("unused") // Called from Swift.
fun resolveSharedCatalogViewModel(): SharedCatalogViewModel = KoinPlatform.getKoin().get()

@Suppress("unused") // Called from Swift.
fun resolveProfileViewModel(): ProfileViewModel = KoinPlatform.getKoin().get()

@Suppress("unused") // Called from Swift.
fun resolveNavigator(): Navigator = KoinPlatform.getKoin().get()

/**
 * Parameterized resolvers — Koin's `parametersOf(...)` doesn't translate
 * directly to Swift, so we provide one factory per screen-with-id.
 */
@Suppress("unused") // Called from Swift.
fun resolveItemDetailViewModel(itemId: String): ItemDetailViewModel =
    KoinPlatform.getKoin().get { parametersOf(itemId) }

@Suppress("unused") // Called from Swift.
fun resolveItemEditViewModel(itemId: String?): ItemEditViewModel =
    KoinPlatform.getKoin().get { parametersOf(itemId) }

@Suppress("unused") // Called from Swift.
fun resolveBookcaseEditViewModel(bookcaseId: String?): BookcaseEditViewModel =
    KoinPlatform.getKoin().get { parametersOf(bookcaseId) }
