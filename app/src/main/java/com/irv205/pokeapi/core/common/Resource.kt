package com.irv205.pokeapi.core.common

/**
 * Wrapper used by the data layer to expose network results in a predictable way.
 * The presentation layer maps these states to its own UI state.
 */
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>
}

inline fun <T> Resource<T>.onSuccess(block: (T) -> Unit): Resource<T> {
    if (this is Resource.Success) block(data)
    return this
}

inline fun <T> Resource<T>.onError(block: (String) -> Unit): Resource<T> {
    if (this is Resource.Error) block(message)
    return this
}
