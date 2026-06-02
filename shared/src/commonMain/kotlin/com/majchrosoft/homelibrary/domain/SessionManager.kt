package com.majchrosoft.homelibrary.domain

import com.majchrosoft.homelibrary.domain.model.User
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SessionManager(private val settings: Settings) {
    private val json = Json { ignoreUnknownKeys = true }

    var user: User?
        get() {
            val userJson = settings.getStringOrNull(KEY_USER) ?: return null
            return try {
                json.decodeFromString<User>(userJson)
            } catch (e: Exception) {
                null
            }
        }
        set(value) {
            if (value != null) {
                settings[KEY_USER] = json.encodeToString(value)
            } else {
                settings.remove(KEY_USER)
            }
        }

    var bearerToken: String?
        get() = settings[KEY_TOKEN]
        set(value) {
            if (value != null) {
                settings[KEY_TOKEN] = value
            } else {
                settings.remove(KEY_TOKEN)
            }
        }

    fun clear() {
        settings.remove(KEY_USER)
        settings.remove(KEY_TOKEN)
    }

    companion object {
        private const val KEY_USER = "session_user"
        private const val KEY_TOKEN = "session_token"
    }
}
