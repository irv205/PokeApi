package com.irv205.pokeapi.data.mock

import okhttp3.Request
import okio.Buffer

data class MockMatch(
    val service: MockService,
    val scenario: MockScenario,
)

object MockRequestMatcher {

    private val REGEX_MARKERS = charArrayOf('\\', '(', ')', '[', ']', '|', '*', '+', '?', '^', '$')

    fun resolve(services: List<MockService>, request: Request): MockMatch? {
        val candidates = services.filter { it.enabled && it.matchesMethodAndPath(request) }
        if (candidates.isEmpty()) return null

        // 1. Escenario con constraints de query/body que coincida.
        for (service in candidates) {
            for (scenario in service.scenarios) {
                if (scenario.matchesRequest(request)) {
                    return MockMatch(service, scenario)
                }
            }
        }

        // 2. Fallback: escenario seleccionado del primer servicio candidato.
        val service = candidates.first()
        val scenario = service.selectedScenario() ?: return null
        return MockMatch(service, scenario)
    }

    fun MockService.matchesMethodAndPath(request: Request): Boolean {
        if (!method.equals(request.method, ignoreCase = true)) return false
        return compilePattern(pathPattern)?.containsMatchIn(request.url.encodedPath) == true
    }

    fun compilePattern(pattern: String): Regex? {
        val trimmed = pattern.trim()
        if (trimmed.isEmpty()) return null

        val normalized = if (trimmed.startsWith("/")) trimmed else "/$trimmed"

        // Si parece regex, se compila tal cual y se ancla al final.
        if (normalized.any { it in REGEX_MARKERS }) {
            return runCatching { Regex(normalized + "$") }.getOrNull()
        }

        // Si es plantilla, {name} -> [^/]+ y se escapa el resto.
        val builder = StringBuilder()
        var index = 0
        while (index < normalized.length) {
            val char = normalized[index]
            if (char == '{') {
                val end = normalized.indexOf('}', startIndex = index)
                if (end > index) {
                    builder.append("[^/]+")
                    index = end + 1
                    continue
                }
            }
            if (char.isLetterOrDigit() || char == '/' || char == '-' || char == '_') {
                builder.append(char)
            } else {
                builder.append('\\').append(char)
            }
            index++
        }
        return runCatching { Regex(builder.toString() + "$") }.getOrNull()
    }
}

fun String.toQueryPairs(): List<Pair<String, String>> =
    trim()
        .split('&', '\n', '\r')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) null
            else part.substring(0, index).trim() to part.substring(index + 1).trim()
        }

fun Request.matchesQuery(pairs: List<Pair<String, String>>): Boolean =
    pairs.all { (key, value) -> url.queryParameter(key) == value }

fun Request.matchesBody(contains: String): Boolean {
    val requestBody = body ?: return false
    val buffer = Buffer()
    return runCatching {
        requestBody.writeTo(buffer)
        buffer.readUtf8().contains(contains)
    }.getOrDefault(false)
}
