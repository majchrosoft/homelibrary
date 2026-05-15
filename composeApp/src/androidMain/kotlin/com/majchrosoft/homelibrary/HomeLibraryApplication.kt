package com.majchrosoft.homelibrary

import android.app.Application
import com.majchrosoft.homelibrary.di.initKoin
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext

class HomeLibraryApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) Napier.base(DebugAntilog())

        initKoin { androidContext(this@HomeLibraryApplication) }
    }
}
