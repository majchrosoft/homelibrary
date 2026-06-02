package com.majchrosoft.homelibrary.di

import com.majchrosoft.homelibrary.presentation.auth.AuthViewModel
import com.majchrosoft.homelibrary.presentation.bookcase.BookcaseEditViewModel
import com.majchrosoft.homelibrary.presentation.bookcase.BookcasesViewModel
import com.majchrosoft.homelibrary.presentation.catalog.SharedCatalogViewModel
import com.majchrosoft.homelibrary.presentation.item.ItemDetailViewModel
import com.majchrosoft.homelibrary.presentation.item.ItemEditViewModel
import com.majchrosoft.homelibrary.presentation.library.LibraryViewModel
import com.majchrosoft.homelibrary.presentation.navigation.Navigator
import com.majchrosoft.homelibrary.presentation.profile.ProfileViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun sharedModule(): Module =
    module {
        includes(platformModule())

        // App-scoped — every host pulls the same Navigator instance and observes
        // [Navigator.current] to swap composables / SwiftUI views.
        single { Navigator() }

        // App-scoped MVI singletons (one per app session).
        singleOf(::AuthViewModel)
        singleOf(::LibraryViewModel)
        singleOf(::BookcasesViewModel)
        singleOf(::SharedCatalogViewModel)
        singleOf(::ProfileViewModel)

        // Per-screen ViewModels — parameterized by the entity id, so we register
        // them as `factory` blocks. The caller passes the id at resolution time
        // via Koin's `parameters { parametersOf(...) }` API.
        factory { (itemId: String) ->
            ItemDetailViewModel(itemId, get(), get(), get())
        }
        factory { (itemId: String?) ->
            ItemEditViewModel(itemId, get(), get(), get())
        }
        factory { (bookcaseId: String?) ->
            BookcaseEditViewModel(bookcaseId, get(), get())
        }
    }

/**
 * Single entry point used by all platform hosts (Android, iOS, Web).
 * Platform-specific configuration is appended via [extra].
 */
fun initKoin(extra: KoinAppDeclaration? = null) =
    startKoin {
        extra?.invoke(this)
        modules(sharedModule())
    }

@Suppress("unused") // Exported for use from Swift's KoinHelper bridge.
fun parameters(vararg args: Any?) = parametersOf(*args)
