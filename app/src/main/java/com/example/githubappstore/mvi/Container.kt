package com.example.githubappstore.mvi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Minimal MVI-style container backed by a [StateFlow]. Intentionally tiny and
 * dependency-free (no MVIKotlin): holds a single [STATE] and exposes an
 * immutable [state] stream plus an [update] transform.
 */
interface Container<STATE> {
    val state: StateFlow<STATE>
    fun update(transform: (STATE) -> STATE)
}

fun <STATE> container(initial: STATE): Container<STATE> = object : Container<STATE> {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<STATE> = _state.asStateFlow()
    override fun update(transform: (STATE) -> STATE) {
        _state.value = transform(_state.value)
    }
}
