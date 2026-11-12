package com.irv205.pokeapi.data.networklog

data class NetworkHeader(
    val name: String,
    val value: String,
)

enum class NetworkCallSource {
    MOCK,
    NETWORK,
}

data class NetworkCallRecord(
    val id: Long,
    val startedAtEpochMs: Long,
    val durationMs: Long,
    val method: String,
    val url: String,
    val path: String,
    val query: String?,
    val requestHeaders: List<NetworkHeader>,
    val requestBody: String?,
    val statusCode: Int?,
    val statusMessage: String?,
    val responseHeaders: List<NetworkHeader>,
    val responseBody: String?,
    val responseBodyTruncated: Boolean,
    val source: NetworkCallSource,
    val mockService: String?,
    val mockScenario: String?,
    val error: String?,
) {
    val isError: Boolean get() = error != null || statusCode == null
    val statusLabel: String get() = statusCode?.toString() ?: "ERROR"
    val endpoint: String
        get() = if (query.isNullOrBlank()) path else "$path?$query"
}
