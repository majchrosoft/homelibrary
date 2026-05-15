package com.majchrosoft.homelibrary

import platform.UIKit.UIDevice

private class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun platform(): Platform = IOSPlatform()
