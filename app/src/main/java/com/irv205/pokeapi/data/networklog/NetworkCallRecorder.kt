package com.irv205.pokeapi.data.networklog

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Registro en memoria de las llamadas que pasan por el cliente OkHttp de la app.
 *
 * Se mantiene fuera de disco a propósito: es una herramienta de debug y las
 * respuestas pueden contener información sensible.
 */
class NetworkCallRecorder(
    private val maxCalls: Int = DEFAULT_MAX_CALLS,
) {

    private val nextId = AtomicLong(0)
    private val _calls = MutableStateFlow<List<NetworkCallRecord>>(emptyList())

    val calls: StateFlow<List<NetworkCallRecord>> = _calls.asStateFlow()

    fun nextId(): Long = nextId.incrementAndGet()

    fun add(record: NetworkCallRecord) {
        _calls.update { current ->
            (listOf(record) + current).take(maxCalls.coerceAtLeast(1))
        }
    }

    fun clear() {
        _calls.value = emptyList()
    }

    companion object {
        const val DEFAULT_MAX_CALLS = 100
    }
}
