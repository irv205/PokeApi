package com.irv205.pokeapi.core.network

import com.irv205.pokeapi.core.common.Resource
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/**
 * Centralized error handling for every network call.
 * Cancellation is always rethrown so structured concurrency keeps working.
 */
suspend fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher,
    block: suspend () -> T,
): Resource<T> = withContext(dispatcher) {
    try {
        Resource.Success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (http: HttpException) {
        Resource.Error(http.toUserMessage(), http)
    } catch (io: IOException) {
        Resource.Error("Sin conexión. Revisa tu red e inténtalo de nuevo.", io)
    } catch (serialization: SerializationException) {
        Resource.Error("La respuesta de PokeAPI no tiene el formato esperado.", serialization)
    } catch (throwable: Throwable) {
        Resource.Error("Ocurrió un error inesperado. Inténtalo de nuevo.", throwable)
    }
}

private fun HttpException.toUserMessage(): String = when (code()) {
    404 -> "No encontramos ese Pokémon."
    429 -> "Demasiadas solicitudes. Espera un momento e inténtalo de nuevo."
    in 500..599 -> "El servidor de PokeAPI no está disponible en este momento."
    else -> "Error de red (${code()}). Inténtalo de nuevo."
}
