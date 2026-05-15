package com.majchrosoft.homelibrary

import com.majchrosoft.homelibrary.di.initKoin

/**
 * Called from `iosApp/iosAppApp.swift` on launch — keeps DI bootstrap symmetric
 * with [com.majchrosoft.homelibrary.android.HomeLibraryApplication.onCreate].
 */
@Suppress("unused") // Called from Swift.
fun startKoinForIos() {
    initKoin()
}
