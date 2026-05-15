package com.majchrosoft.homelibrary

import com.majchrosoft.homelibrary.domain.model.ItemQuality
import com.majchrosoft.homelibrary.domain.model.ItemType

/**
 * Kotlin enums are exported to Swift, but the synthetic `entries`/`values()`
 * accessor is awkward to use from Swift (returns `KotlinArray<MyEnum>`). We
 * expose a plain `List` so SwiftUI's `ForEach` can iterate it directly.
 *
 * Add a new accessor here whenever a domain enum needs to drive a Picker.
 */
@Suppress("unused") // Called from Swift.
fun itemTypeValues(): List<ItemType> = ItemType.entries

@Suppress("unused") // Called from Swift.
fun itemQualityValues(): List<ItemQuality> = ItemQuality.entries
