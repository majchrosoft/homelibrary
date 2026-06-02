package com.majchrosoft.homelibrary.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tiny multiplatform ViewModel base. Android-specific lifecycle integration is
 * handled in the androidMain side (the AndroidX ViewModel wrapper). For iOS &
 * Web the consumer is responsible for calling [clear] when the screen is destroyed.
 */
abstract class MviViewModel<State : Any, Intent : Any> {
    private val job: Job = SupervisorJob()
    protected val scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate + job)

    private val _state: MutableStateFlow<State> by lazy { MutableStateFlow(initialState()) }
    val state: StateFlow<State> get() = _state.asStateFlow()

    abstract fun initialState(): State

    abstract fun handleIntent(intent: Intent)

    fun dispatch(intent: Intent) = handleIntent(intent)

    protected fun setState(reducer: (State) -> State) {
        val oldState = _state.value
        val newState = reducer(oldState)
        if (oldState != newState) {
            _state.value = newState
        }
    }

    open fun clear() {
        scope.cancel()
    }
}
