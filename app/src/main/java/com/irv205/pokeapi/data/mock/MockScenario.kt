package com.irv205.pokeapi.data.mock

import kotlinx.serialization.Serializable
import okhttp3.Request

@Serializable
data class MockScenario(
    val id: String,
    val name: String,
    val statusCode: Int = 200,
    val contentType: String = "application/json",
    val requestQuery: String = "",
    val requestBody: String = "",
    val responseHeaders: String = "",
    val responseBody: String = "",
    val delayMs: Long = 0L,
) {
    fun matchesRequest(request: Request): Boolean {
        val querySpec = requestQuery.toQueryPairs()
        val bodySpec = requestBody.trim()
        val hasQuery = querySpec.isNotEmpty()
        val hasBody = bodySpec.isNotEmpty()

        if (!hasQuery && !hasBody) return false

        if (hasQuery && !request.matchesQuery(querySpec)) return false
        if (hasBody && !request.matchesBody(bodySpec)) return false

        return true
    }
}
