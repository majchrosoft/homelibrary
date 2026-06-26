package com.majchrosoft.homelibrary.di

import com.majchrosoft.homelibrary.data.firebase.FirebaseAuthRepository
import com.majchrosoft.homelibrary.data.firebase.FirebaseBookcaseRepository
import com.majchrosoft.homelibrary.data.firebase.FirebaseItemRepository
import com.majchrosoft.homelibrary.domain.SessionManager
import com.majchrosoft.homelibrary.domain.repository.AuthRepository
import com.majchrosoft.homelibrary.domain.repository.BookcaseRepository
import com.majchrosoft.homelibrary.domain.repository.ItemRepository
import com.russhwolf.settings.Settings
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.database.database
import org.koin.dsl.module

actual fun platformModule() =
    module {
        single<Settings> { com.majchrosoft.homelibrary.di.NoOpSettings }
        single { SessionManager(get()) }

        single { Firebase.auth }
        single { Firebase.database }

        single<AuthRepository> { FirebaseAuthRepository(get(), get()) }
        single<ItemRepository> { FirebaseItemRepository(get()) }
        single<BookcaseRepository> { FirebaseBookcaseRepository(get()) }
    }
