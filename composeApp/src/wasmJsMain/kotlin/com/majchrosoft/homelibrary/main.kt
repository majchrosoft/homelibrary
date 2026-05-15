package com.majchrosoft.homelibrary

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.majchrosoft.homelibrary.di.initKoin
import com.majchrosoft.homelibrary.ui.App
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    Napier.base(DebugAntilog())
    initKoin()

    ComposeViewport(document.body!!) { App() }
}
