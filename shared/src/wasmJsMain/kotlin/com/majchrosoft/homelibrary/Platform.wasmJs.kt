package com.majchrosoft.homelibrary

import kotlinx.browser.window

private class WasmJsPlatform : Platform {
    override val name: String = "Web (${window.navigator.userAgent})"
}

actual fun platform(): Platform = WasmJsPlatform()
