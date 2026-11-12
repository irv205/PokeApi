package com.irv205.pokeapi.presentation.mock

import androidx.lifecycle.SavedStateHandle
import com.irv205.pokeapi.data.mock.InMemoryMockConfigProvider
import com.irv205.pokeapi.data.mock.MockScenario
import com.irv205.pokeapi.data.mock.PokeMockFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockServiceDetailViewModelTest {

    private lateinit var config: InMemoryMockConfigProvider
    private lateinit var viewModel: MockServiceDetailViewModel

    @Before
    fun setUp() {
        config = InMemoryMockConfigProvider(services = PokeMockFixtures.services())
        viewModel = viewModel("pokemon_page")
    }

    private fun viewModel(serviceId: String) = MockServiceDetailViewModel(
        configProvider = config,
        savedStateHandle = SavedStateHandle(mapOf("serviceId" to serviceId)),
    )

    @Test
    fun `initial state exposes the requested service`() {
        val service = viewModel.uiState.value.service

        assertEquals("pokemon_page", service?.id)
        assertEquals("Lista de Pokemon", service?.name)
    }

    @Test
    fun `unknown service results in an empty state`() {
        val missing = viewModel("missing")

        assertNull(missing.uiState.value.service)
        assertFalse(missing.uiState.value.deleted)
    }

    @Test
    fun `updateService saves trimmed values and normalizes method`() {
        viewModel.updateService(
            name = "  Lista personalizada  ",
            method = " post ",
            pathPattern = " /api/v2/pokemon ",
            enabled = false,
        )

        val service = viewModel.uiState.value.service!!
        assertEquals("Lista personalizada", service.name)
        assertEquals("POST", service.method)
        assertEquals("/api/v2/pokemon", service.pathPattern)
        assertFalse(service.enabled)
    }

    @Test
    fun `updateService ignores blank name or path`() {
        viewModel.updateService("   ", "GET", "/api/v2/pokemon", true)
        viewModel.updateService("Nombre", "GET", "   ", true)

        val service = viewModel.uiState.value.service!!
        assertEquals("Lista de Pokemon", service.name)
        assertEquals("/api/v2/pokemon", service.pathPattern)
    }

    @Test
    fun `saveScenario adds a response and selects it when none was active`() {
        val service = viewModel.uiState.value.service!!
        val scenario = MockScenario(
            id = "custom_400",
            name = "  Error 400 personalizado ",
            statusCode = 400,
            contentType = " application/json ",
            requestQuery = "limit=0",
            responseBody = """{"detail":"limit invalido"}""",
        )

        viewModel.saveScenario(scenario)

        val updated = viewModel.uiState.value.service!!
        val added = updated.scenarios.first { it.id == "custom_400" }
        assertEquals(service.scenarios.size + 1, updated.scenarios.size)
        assertEquals("Error 400 personalizado", added.name)
        assertEquals("application/json", added.contentType)
        assertEquals("limit=0", added.requestQuery)
        assertEquals("""{"detail":"limit invalido"}""", added.responseBody)
    }

    @Test
    fun `saveScenario clamps status and delay to valid ranges`() {
        viewModel.saveScenario(
            MockScenario(
                id = "weird",
                name = "Raro",
                statusCode = 900,
                delayMs = -10,
            ),
        )

        val saved = viewModel.uiState.value.service!!.scenarios.first { it.id == "weird" }
        assertEquals(599, saved.statusCode)
        assertEquals(0L, saved.delayMs)
    }

    @Test
    fun `saveScenario updates an existing response`() {
        val current = viewModel.uiState.value.service!!
            .scenarios
            .first { it.id == "server_error" }

        viewModel.saveScenario(
            current.copy(statusCode = 503, responseBody = """{"detail":"Mantenimiento"}"""),
        )

        val updated = viewModel.uiState.value.service!!
            .scenarios
            .first { it.id == "server_error" }
        assertEquals(503, updated.statusCode)
        assertEquals("""{"detail":"Mantenimiento"}""", updated.responseBody)
    }

    @Test
    fun `selectScenario updates the active response`() {
        viewModel.selectScenario("server_error")

        val service = viewModel.uiState.value.service!!
        assertEquals("server_error", service.selectedScenarioId)
        assertEquals(500, service.selectedScenario()?.statusCode)
    }

    @Test
    fun `deleteScenario removes it and picks a new active response`() {
        viewModel.selectScenario("server_error")

        viewModel.deleteScenario("server_error")

        val service = viewModel.uiState.value.service!!
        assertEquals(4, service.scenarios.size)
        assertEquals(service.scenarios.first().id, service.selectedScenarioId)
    }

    @Test
    fun `deleteService marks the state as deleted`() {
        viewModel.deleteService()

        assertTrue(viewModel.uiState.value.deleted)
        assertNull(viewModel.uiState.value.service)
        assertNull(config.findService("pokemon_page"))
    }

    @Test
    fun `newScenario creates a blank editable response`() {
        val scenario = viewModel.newScenario()

        assertTrue(scenario.id.startsWith("scenario_"))
        assertEquals(200, scenario.statusCode)
        assertEquals("{}", scenario.responseBody)
    }
}
