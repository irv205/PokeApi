package com.irv205.pokeapi.data.mock

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class MockInterceptor(
    private val environment: EnvironmentProvider,
    private val config: MockConfigProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // PROD: no se toca la request ni se evalúan los mocks.
        if (environment.current != ApiEnvironment.MOCK) {
            return chain.proceed(request)
        }

        val match = MockRequestMatcher.resolve(config.services, request)
            ?: return handleNoMatch(chain, request)

        val scenario = match.scenario

        if (scenario.delayMs > 0) {
            runCatching { Thread.sleep(scenario.delayMs) }
        }

        val mediaType = scenario.contentType
            .ifBlank { "application/json" }
            .toMediaTypeOrNull()

        val isEmptyBody = scenario.statusCode == 204 ||
            scenario.statusCode == 304 ||
            request.method.equals("HEAD", ignoreCase = true)

        val body = if (isEmptyBody) {
            "".toResponseBody(null)
        } else {
            scenario.responseBody.toResponseBody(mediaType)
        }

        val builder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(scenario.statusCode)
            .message(statusMessage(scenario.statusCode))
            .body(body)
            .header("Content-Type", mediaType?.toString() ?: "application/json")
            .header("x-mocked", "true")
            .header("x-mock-service", sanitizeHeaderValue(match.service.name))
            .header("x-mock-scenario", sanitizeHeaderValue(scenario.name))

        scenario.responseHeaders.toHeaderPairs().forEach { (name, value) ->
            builder.header(name, value)
        }

        return builder.build()
    }

    private fun handleNoMatch(chain: Interceptor.Chain, request: Request): Response {
        if (config.fallbackToNetwork) {
            return chain.proceed(request)
        }
        throw IOException(
            "No mock matched: ${request.method} ${request.url}. " +
                "Revisa pathPattern, method, requestQuery y requestBody.",
        )
    }

    private fun statusMessage(code: Int): String = when (code) {
        200 -> "OK"
        201 -> "Created"
        204 -> "No Content"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        403 -> "Forbidden"
        404 -> "Not Found"
        500 -> "Internal Server Error"
        else -> "Mocked"
    }

    private fun sanitizeHeaderValue(value: String): String =
        value.replace("[^\u0020-\u007e]".toRegex(), "?")
}

private fun String.toHeaderPairs(): List<Pair<String, String>> =
    lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { line ->
            val index = line.indexOf(':')
            if (index <= 0) null
            else line.substring(0, index).trim() to line.substring(index + 1).trim()
        }
        .toList()
