package com.majchrosoft.homelibrary

interface Platform {
    val name: String
}

expect fun platform(): Platform
