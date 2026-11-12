package com.irv205.pokeapi.data.networklog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkCallRecorderTest {

    @Test
    fun `keeps the newest calls first and respects the limit`() {
        val recorder = NetworkCallRecorder(maxCalls = 2)

        recorder.add(call(id = 1))
        recorder.add(call(id = 2))
        recorder.add(call(id = 3))

        assertEquals(listOf(3L, 2L), recorder.calls.value.map { it.id })
    }

    @Test
    fun `clear removes every recorded call`() {
        val recorder = NetworkCallRecorder()
        recorder.add(call(id = 1))

        recorder.clear()

        assertTrue(recorder.calls.value.isEmpty())
    }

    private fun call(id: Long) = NetworkCallRecord(
        id = id,
        startedAtEpochMs = 0,
        durationMs = 1,
        method = "GET",
        url = "https://pokeapi.co/api/v2/pokemon",
        path = "/api/v2/pokemon",
        query = null,
        requestHeaders = emptyList(),
        requestBody = null,
        statusCode = 200,
        statusMessage = "OK",
        responseHeaders = emptyList(),
        responseBody = "{}",
        responseBodyTruncated = false,
        source = NetworkCallSource.MOCK,
        mockService = null,
        mockScenario = null,
        error = null,
    )
}
