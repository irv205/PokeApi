package com.irv205.pokeapi.data.mock

import kotlinx.serialization.Serializable

@Serializable
data class MockService(
    val id: String,
    val name: String,
    val method: String = "GET",
    val pathPattern: String,
    val enabled: Boolean = true,
    val selectedScenarioId: String? = null,
    val scenarios: List<MockScenario> = emptyList(),
) {
    fun selectedScenario(): MockScenario? =
        scenarios.firstOrNull { it.id == selectedScenarioId } ?: scenarios.firstOrNull()
}
