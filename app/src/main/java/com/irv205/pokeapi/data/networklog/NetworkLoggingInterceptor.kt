package com.irv205.pokeapi.data.networklog

import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer

/**
 * Captura cada llamada que atraviesa OkHttp (real o mockeada) y la publica en
 * [NetworkCallRecorder] para la pantalla de debug.
 *
 * Debe instalarse como el interceptor más externo: así también ve las respuestas
 * que devuelve [com.irv205.pokeapi.data.mock.MockInterceptor].
 */
class NetworkLoggingInterceptor(
    private val recorder: NetworkCallRecorder,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startedAt = System.currentTimeMillis()
        val startNanos = System.nanoTime()
        val requestBody = readRequestBody(request.body)

        return try {
            val response = chain.proceed(request)
            record(
                request = request,
                requestBody = requestBody,
                response = response,
                error = null,
                startedAt = startedAt,
                durationMs = elapsedMillis(startNanos),
            )
            response
        } catch (exception: Exception) {
            record(
                request = request,
                requestBody = requestBody,
                response = null,
                error = exception,
                startedAt = startedAt,
                durationMs = elapsedMillis(startNanos),
            )
            throw exception
        }
    }

    private fun record(
        request: Request,
        requestBody: String?,
        response: Response?,
        error: Exception?,
        startedAt: Long,
        durationMs: Long,
    ) {
        // El logging nunca debe romper una llamada de red.
        runCatching {
            val body = response?.let(::readResponseBody)
            val isMock = response?.header(HEADER_MOCKED).equals("true", ignoreCase = true)

            recorder.add(
                NetworkCallRecord(
                    id = recorder.nextId(),
                    startedAtEpochMs = startedAt,
                    durationMs = durationMs,
                    method = request.method,
                    url = request.url.toString(),
                    path = request.url.encodedPath,
                    query = request.url.encodedQuery,
                    requestHeaders = request.headers.toNetworkHeaders(),
                    requestBody = requestBody,
                    statusCode = response?.code,
                    statusMessage = response?.message,
                    responseHeaders = response?.headers?.toNetworkHeaders().orEmpty(),
                    responseBody = body?.text,
                    responseBodyTruncated = body?.truncated ?: false,
                    source = if (isMock) NetworkCallSource.MOCK else NetworkCallSource.NETWORK,
                    mockService = response?.header(HEADER_MOCK_SERVICE),
                    mockScenario = response?.header(HEADER_MOCK_SCENARIO),
                    error = error?.let { "${it::class.java.simpleName}: ${it.message.orEmpty()}" },
                ),
            )
        }
    }

    private data class BodySnapshot(
        val text: String?,
        val truncated: Boolean,
    )

    private fun readResponseBody(response: Response): BodySnapshot {
        val body = response.body ?: return BodySnapshot(null, false)
        val contentLength = body.contentLength()
        val truncated = contentLength > MAX_BODY_BYTES
        val text = runCatching {
            response.peekBody(MAX_BODY_BYTES).string()
        }.getOrNull()
        return BodySnapshot(text, truncated)
    }

    private fun readRequestBody(body: RequestBody?): String? {
        if (body == null) return null
        if (body.isOneShot() || body.isDuplex()) return NON_INSPECTABLE_BODY
        return runCatching {
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        }.getOrNull()
    }

    private fun Headers.toNetworkHeaders(): List<NetworkHeader> =
        (0 until size).map { index ->
            NetworkHeader(name(index), value(index))
        }

    private fun elapsedMillis(startNanos: Long): Long =
        (System.nanoTime() - startNanos) / 1_000_000

    private companion object {
        const val HEADER_MOCKED = "x-mocked"
        const val HEADER_MOCK_SERVICE = "x-mock-service"
        const val HEADER_MOCK_SCENARIO = "x-mock-scenario"
        const val MAX_BODY_BYTES = 64L * 1024L
        const val NON_INSPECTABLE_BODY = "<cuerpo no inspeccionable>"
    }
}
