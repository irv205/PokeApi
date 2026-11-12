package com.irv205.pokeapi.presentation.mock

import com.irv205.pokeapi.data.mock.ApiEnvironment
import com.irv205.pokeapi.data.mock.InMemoryMockConfigProvider
import com.irv205.pokeapi.data.mock.MutableEnvironmentProvider
import com.irv205.pokeapi.data.mock.PokeMockFixtures
import com.irv205.pokeapi.data.networklog.NetworkCallRecord
import com.irv205.pokeapi.data.networklog.NetworkCallRecorder
import com.irv205.pokeapi.data.networklog.NetworkCallSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockConfigViewModelTest {

    private lateinit var environment: MutableEnvironmentProvider
    private lateinit var config: InMemoryMockConfigProvider
    private lateinit var recorder: NetworkCallRecorder
    private lateinit var viewModel: MockConfigViewModel

    @Before
    fun setUp() {
        environment = MutableEnvironmentProvider(ApiEnvironment.PROD)
        config = InMemoryMockConfigProvider(services = PokeMockFixtures.services())
        recorder = NetworkCallRecorder()
        viewModel = MockConfigViewModel(environment, config, recorder)
    }

    @Test
    fun `enabling mocks changes the environment`() {
        viewModel.setMockEnabled(true)

        assertEquals(ApiEnvironment.MOCK, environment.current)
        assertTrue(viewModel.uiState.value.isMockEnabled)
    }

    @Test
    fun `fallback changes the provider`() {
        viewModel.setFallbackToNetwork(true)

        assertTrue(config.fallbackToNetwork)
        assertTrue(viewModel.uiState.value.fallbackToNetwork)
    }

    @Test
    fun `service can be disabled and scenario selected`() {
        val service = viewModel.uiState.value.services.first { it.id == "pokemon_page" }

        viewModel.setServiceEnabled(service.id, false)
        viewModel.selectScenario(service.id, "server_error")

        val updated = viewModel.uiState.value.services.first { it.id == service.id }
        assertFalse(updated.enabled)
        assertEquals("server_error", updated.selectedScenarioId)
        assertEquals("server_error", updated.selectedScenario()?.id)
        assertEquals(500, updated.selectedScenario()?.statusCode)
    }

    @Test
    fun `network calls are exposed and can be cleared`() {
        val call = sampleCall()

        recorder.add(call)

        assertEquals(listOf(call), viewModel.networkCalls.value)

        viewModel.clearNetworkCalls()

        assertTrue(viewModel.networkCalls.value.isEmpty())
    }

    @Test
    fun `reset fixtures restores services and fallback`() {
        viewModel.setFallbackToNetwork(true)
        viewModel.setServiceEnabled("pokemon_page", false)

        viewModel.resetFixtures()

        assertFalse(config.fallbackToNetwork)
        assertTrue(viewModel.uiState.value.services.all { it.enabled })
        assertEquals(
            PokeMockFixtures.services().map { it.id },
            viewModel.uiState.value.services.map { it.id },
        )
    }

    @Test
    fun `exportConfigJson returns portable mock configuration`() {
        val json = viewModel.exportConfigJson()

        assertTrue(json.contains("\"id\""))
        assertTrue(json.contains("\"scenarios\""))
        assertTrue(json.contains("pokemon_page"))
    }

    @Test
    fun `importConfigJson replaces services when json is valid`() {
        val exported = viewModel.exportConfigJson()
        viewModel.setServiceEnabled("pokemon_page", false)

        val error = viewModel.importConfigJson(exported)

        assertNull(error)
        assertEquals(PokeMockFixtures.services().size, viewModel.uiState.value.services.size)
        assertTrue(viewModel.uiState.value.services.first { it.id == "pokemon_page" }.enabled)
    }

    @Test
    fun `importConfigJson reports invalid json`() {
        val before = viewModel.uiState.value.services

        val error = viewModel.importConfigJson("{not-json")

        assertNotNull(error)
        assertEquals(before, viewModel.uiState.value.services)
    }

    @Test
    fun `importConfigJson rejects empty content`() {
        val error = viewModel.importConfigJson("   ")

        assertNotNull(error)
    }

    private fun sampleCall() = NetworkCallRecord(
        id = 1,
        startedAtEpochMs = 1_700_000_000_000,
        durationMs = 12,
        method = "GET",
        url = "https://pokeapi.co/api/v2/pokemon?limit=20",
        path = "/api/v2/pokemon",
        query = "limit=20",
        requestHeaders = emptyList(),
        requestBody = null,
        statusCode = 200,
        statusMessage = "OK",
        responseHeaders = emptyList(),
        responseBody = """{"count":1302}""",
        responseBodyTruncated = false,
        source = NetworkCallSource.MOCK,
        mockService = "Lista de Pokemon",
        mockScenario = "Primera pagina",
        error = null,
    )
}
