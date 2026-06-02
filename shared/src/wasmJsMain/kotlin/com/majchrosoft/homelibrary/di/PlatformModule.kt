package com.majchrosoft.homelibrary.di

import com.majchrosoft.homelibrary.data.firebase.WasmAuthRepository
import com.majchrosoft.homelibrary.data.firebase.WasmBookcaseRepository
import com.majchrosoft.homelibrary.data.firebase.WasmItemRepository
import com.majchrosoft.homelibrary.domain.repository.AuthRepository
import com.majchrosoft.homelibrary.domain.repository.BookcaseRepository
import com.majchrosoft.homelibrary.domain.repository.ItemRepository
import com.majchrosoft.homelibrary.domain.SessionManager
import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import kotlinx.browser.window
import org.koin.dsl.module

actual fun platformModule() =
    module {
        single { StorageSettings(window.localStorage) as Settings }
        single { SessionManager(get()) }

        single<AuthRepository> { WasmAuthRepository(get()) }
        single<ItemRepository> { WasmItemRepository() }
        single<BookcaseRepository> { WasmBookcaseRepository() }
    }
