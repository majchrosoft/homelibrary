package com.majchrosoft.homelibrary.di

import com.russhwolf.settings.Settings

/**
 * Read-only in-memory [Settings] used on JVM (tests / desktop).
 * Does not persist — suitable for the `Settings` dependency when the app
 * only needs read access (SessionManager reads the bearer token).
 */
object NoOpSettings : Settings {
    private val store = mutableMapOf<String, String>()

    override val keys: Set<String> get() = store.keys

    override val size: Int get() = store.size

    override fun clear() { store.clear() }

    override fun remove(key: String) { store.remove(key) }

    override fun hasKey(key: String): Boolean = key in store

    override fun getInt(key: String, defaultValue: Int): Int =
        store[key]?.toIntOrNull() ?: defaultValue

    override fun getIntOrNull(key: String): Int? = store[key]?.toIntOrNull()

    override fun putInt(key: String, value: Int) { store[key] = value.toString() }

    override fun getLong(key: String, defaultValue: Long): Long =
        store[key]?.toLongOrNull() ?: defaultValue

    override fun getLongOrNull(key: String): Long? = store[key]?.toLongOrNull()

    override fun putLong(key: String, value: Long) { store[key] = value.toString() }

    override fun getString(key: String, defaultValue: String): String =
        store[key] ?: defaultValue

    override fun getStringOrNull(key: String): String? = store[key]

    override fun putString(key: String, value: String) { store[key] = value }

    override fun getDouble(key: String, defaultValue: Double): Double =
        store[key]?.toDoubleOrNull() ?: defaultValue

    override fun getDoubleOrNull(key: String): Double? = store[key]?.toDoubleOrNull()

    override fun putDouble(key: String, value: Double) { store[key] = value.toString() }

    override fun getFloat(key: String, defaultValue: Float): Float =
        store[key]?.toFloatOrNull() ?: defaultValue

    override fun getFloatOrNull(key: String): Float? = store[key]?.toFloatOrNull()

    override fun putFloat(key: String, value: Float) { store[key] = value.toString() }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        store[key]?.toBooleanStrictOrNull() ?: defaultValue

    override fun getBooleanOrNull(key: String): Boolean? = store[key]?.toBooleanStrictOrNull()

    override fun putBoolean(key: String, value: Boolean) { store[key] = value.toString() }

}
