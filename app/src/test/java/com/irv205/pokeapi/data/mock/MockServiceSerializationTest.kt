package com.irv205.pokeapi.data.mock

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class MockServiceSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `imports services and scenarios from json`() {
        val raw = """
            [
              {
                "id": "pokemon_page",
                "name": "E2E error",
                "method": "GET",
                "pathPattern": "/api/v2/pokemon",
                "enabled": true,
                "selectedScenarioId": "error",
                "scenarios": [
                  {
                    "id": "error",
                    "name": "E2E 500",
                    "statusCode": 500,
                    "contentType": "application/json",
                    "responseBody": "{\"detail\":\"boom\"}",
                    "delayMs": 250
                  }
                ]
              }
            ]
        """.trimIndent()

        val services = json.decodeFromString<List<MockService>>(raw)

        assertEquals(1, services.size)
        val service = services.single()
        assertEquals("pokemon_page", service.id)
        assertFalse(service.scenarios.isEmpty())
        assertEquals("error", service.selectedScenario()?.id)
        assertEquals(500, service.selectedScenario()?.statusCode)
        assertEquals(250L, service.selectedScenario()?.delayMs)
    }

    @Test
    fun `exports and re-imports the complete fixture catalog`() {
        val exported = Json.encodeToString(PokeMockFixtures.services())
        val imported = json.decodeFromString<List<MockService>>(exported)

        assertFalse(imported.isEmpty())
        imported.forEach { service ->
            assertNotNull(service.selectedScenario())
        }
        assertEquals(
            PokeMockFixtures.services().map { it.id },
            imported.map { it.id },
        )
    }
}
