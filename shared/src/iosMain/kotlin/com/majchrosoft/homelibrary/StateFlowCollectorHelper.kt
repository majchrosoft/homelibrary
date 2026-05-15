package com.majchrosoft.homelibrary

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

/**
 * Suspend bridge so Swift can `await` collection of a Kotlin Flow with a
 * Swift closure as the collector. Used by `StateFlowPublisher.swift`.
 */
@Suppress("unused") // Called from Swift.
suspend fun <T> collectFlow(flow: Flow<T>, collector: (T) -> Unit) {
    flow.collect { collector(it) }
}
