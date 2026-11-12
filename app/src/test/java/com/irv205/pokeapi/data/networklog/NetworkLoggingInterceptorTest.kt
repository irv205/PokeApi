package com.irv205.pokeapi.data.networklog

import com.irv205.pokeapi.data.mock.ApiEnvironment
import com.irv205.pokeapi.data.mock.InMemoryMockConfigProvider
import com.irv205.pokeapi.data.mock.MockInterceptor
import com.irv205.pokeapi.data.mock.MutableEnvironmentProvider
import com.irv205.pokeapi.data.mock.PokeMockFixtures
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkLoggingInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var recorder: NetworkCallRecorder

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        recorder = NetworkCallRecorder()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `records method endpoint status headers and body of a real call`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("X-Request-Id", "abc-123")
                .setBody("""{"created":true}"""),
        )

        val client = OkHttpClient.Builder()
            .addInterceptor(NetworkLoggingInterceptor(recorder))
            .build()

        val request = Request.Builder()
            .url(server.url("/api/v2/pokemon?limit=20&offset=0"))
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(201, response.code)
            response.body?.string()
        }

        val call = recorder.calls.value.single()
        assertEquals("GET", call.method)
        assertEquals(server.url("/api/v2/pokemon?limit=20&offset=0").toString(), call.url)
        assertEquals("/api/v2/pokemon", call.path)
        assertEquals("limit=20&offset=0", call.query)
        assertEquals(201, call.statusCode)
        assertEquals(NetworkCallSource.NETWORK, call.source)
        assertTrue(call.requestHeaders.any { it.name == "Accept" && it.value == "application/json" })
        assertTrue(call.responseHeaders.any { it.name == "X-Request-Id" && it.value == "abc-123" })
        assertEquals("""{"created":true}""", call.responseBody)
        assertNull(call.error)
    }

    @Test
    fun `records calls served by the mock interceptor`() {
        val environment = MutableEnvironmentProvider(ApiEnvironment.MOCK)
        val config = InMemoryMockConfigProvider(services = PokeMockFixtures.services())

        val client = OkHttpClient.Builder()
            .addInterceptor(NetworkLoggingInterceptor(recorder))
            .addInterceptor(MockInterceptor(environment, config))
            .build()

        client.newCall(
            Request.Builder()
                .url(server.url("/api/v2/pokemon?limit=20&offset=0"))
                .build(),
        ).execute().use { response ->
            assertEquals(200, response.code)
            response.body?.string()
        }

        val call = recorder.calls.value.single()
        assertEquals(NetworkCallSource.MOCK, call.source)
        assertEquals("Lista de Pokemon", call.mockService)
        assertEquals("Primera pagina (200)", call.mockScenario)
        assertEquals(200, call.statusCode)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `records errors thrown before a response is produced`() {
        val environment = MutableEnvironmentProvider(ApiEnvironment.MOCK)
        val config = InMemoryMockConfigProvider(
            services = PokeMockFixtures.services(),
            fallbackToNetwork = false,
        )

        val client = OkHttpClient.Builder()
            .addInterceptor(NetworkLoggingInterceptor(recorder))
            .addInterceptor(MockInterceptor(environment, config))
            .build()

        val request = Request.Builder()
            .url(server.url("/api/v2/unknown"))
            .build()

        val thrown = runCatching {
            client.newCall(request).execute().use { it.body?.string() }
        }.exceptionOrNull()

        assertTrue(thrown is IOException)
        val call = recorder.calls.value.single()
        assertTrue(call.isError)
        assertNull(call.statusCode)
        assertNotNull(call.error)
    }
}
