package com.majchrosoft.homelibrary

private class JvmPlatform : Platform {
    override val name: String = "JVM"
}

actual fun platform(): Platform = JvmPlatform()
