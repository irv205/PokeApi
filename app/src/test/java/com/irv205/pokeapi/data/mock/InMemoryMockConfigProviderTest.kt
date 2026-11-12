package com.irv205.pokeapi.data.mock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryMockConfigProviderTest {

    private fun provider() = InMemoryMockConfigProvider(
        services = PokeMockFixtures.services(),
    )

    @Test
    fun `updateService replaces editable fields and keeps scenarios`() {
        val provider = provider()
        val before = provider.findService("pokemon_page")!!

        provider.updateService("pokemon_page") { service ->
            service.copy(
                name = "Lista editada",
                method = "post",
                pathPattern = "/api/v2/pokemon/{id}",
                enabled = false,
            )
        }

        val after = provider.findService("pokemon_page")!!
        assertEquals("Lista editada", after.name)
        assertEquals("post", after.method)
        assertEquals("/api/v2/pokemon/{id}", after.pathPattern)
        assertFalse(after.enabled)
        assertEquals(before.scenarios, after.scenarios)
    }

    @Test
    fun `upsertScenario adds a new scenario and keeps the current selection`() {
        val provider = provider()
        val scenario = MockScenario(
            id = "custom_400",
            name = "Error personalizado (400)",
            statusCode = 400,
            responseBody = """{"detail":"Falta el parametro limit"}""",
        )

        provider.upsertScenario("pokemon_page", scenario)

        val service = provider.findService("pokemon_page")!!
        assertEquals(6, service.scenarios.size)
        assertEquals("first_page", service.selectedScenarioId)
        assertEquals(scenario, service.scenarios.last())
    }

    @Test
    fun `upsertScenario updates an existing scenario in place`() {
        val provider = provider()
        val original = provider.findService("pokemon_page")!!
            .scenarios
            .first { it.id == "server_error" }

        provider.upsertScenario(
            "pokemon_page",
            original.copy(statusCode = 503, responseBody = """{"detail":"Mantenimiento"}"""),
        )

        val updated = provider.findService("pokemon_page")!!
            .scenarios
            .first { it.id == "server_error" }
        assertEquals(503, updated.statusCode)
        assertEquals("""{"detail":"Mantenimiento"}""", updated.responseBody)
        assertEquals(
            1,
            provider.findService("pokemon_page")!!.scenarios.count { it.id == "server_error" },
        )
    }

    @Test
    fun `adding the first scenario selects it automatically`() {
        val provider = InMemoryMockConfigProvider(
            services = listOf(
                MockService(
                    id = "empty_service",
                    name = "Servicio vacio",
                    pathPattern = "/api/v2/empty",
                ),
            ),
        )
        val scenario = MockScenario(id = "only", name = "Unica", responseBody = "{}")

        provider.upsertScenario("empty_service", scenario)

        assertEquals("only", provider.findService("empty_service")?.selectedScenarioId)
    }

    @Test
    fun `deleting the selected scenario selects the first remaining one`() {
        val provider = provider()
        provider.selectScenario("pokemon_page", "server_error")

        provider.deleteScenario("pokemon_page", "server_error")

        val service = provider.findService("pokemon_page")!!
        assertEquals(4, service.scenarios.size)
        assertNull(service.scenarios.firstOrNull { it.id == "server_error" })
        assertEquals(service.scenarios.first().id, service.selectedScenarioId)
    }

    @Test
    fun `deleting a non selected scenario keeps the selection`() {
        val provider = provider()
        provider.selectScenario("pokemon_page", "server_error")

        provider.deleteScenario("pokemon_page", "quiz_pool")

        assertEquals("server_error", provider.findService("pokemon_page")?.selectedScenarioId)
    }

    @Test
    fun `selectScenario ignores unknown ids`() {
        val provider = provider()

        provider.selectScenario("pokemon_page", "does_not_exist")

        assertEquals("first_page", provider.findService("pokemon_page")?.selectedScenarioId)
    }

    @Test
    fun `deleteService removes the service`() {
        val provider = provider()

        provider.deleteService("pokemon_page")

        assertNull(provider.findService("pokemon_page"))
        assertTrue(provider.services.none { it.id == "pokemon_page" })
    }

    @Test
    fun `setServiceEnabled toggles only the requested service`() {
        val provider = provider()

        provider.setServiceEnabled("pokemon_page", false)

        assertFalse(provider.findService("pokemon_page")!!.enabled)
        assertTrue(
            provider.services
                .filterNot { it.id == "pokemon_page" }
                .all { it.enabled },
        )
    }
}
