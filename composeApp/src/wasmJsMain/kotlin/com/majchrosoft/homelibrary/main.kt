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
    Napier.d { "Wasm main() started" }
    initKoin()

    ComposeViewport(document.body!!) {
        Napier.d { "ComposeViewport attached" }
        val loadingElement = document.getElementById("loading")
        loadingElement?.apply {
            setAttribute("style", "display: none !important;")
        }
        App()
    }
}
