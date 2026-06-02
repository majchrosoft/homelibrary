package com.majchrosoft.homelibrary

// Removed kotlinx.browser.window for now to fix compilation until dependency is fixed
// import kotlinx.browser.window

private class WasmJsPlatform : Platform {
    override val name: String = "Web"
}

actual fun platform(): Platform = WasmJsPlatform()
