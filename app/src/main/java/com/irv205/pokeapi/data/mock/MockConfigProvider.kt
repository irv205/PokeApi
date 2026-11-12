package com.irv205.pokeapi.data.mock

interface MockConfigProvider {
    val services: List<MockService>
    val fallbackToNetwork: Boolean get() = false
}

class InMemoryMockConfigProvider(
    override var services: List<MockService> = emptyList(),
    override var fallbackToNetwork: Boolean = false,
) : MockConfigProvider {

    fun findService(serviceId: String): MockService? =
        services.firstOrNull { it.id == serviceId }

    fun updateService(serviceId: String, transform: (MockService) -> MockService) {
        services = services.map { service ->
            if (service.id == serviceId) transform(service) else service
        }
    }

    fun deleteService(serviceId: String) {
        services = services.filterNot { it.id == serviceId }
    }

    fun upsertScenario(serviceId: String, scenario: MockScenario) {
        updateService(serviceId) { service ->
            val exists = service.scenarios.any { it.id == scenario.id }
            val scenarios = if (exists) {
                service.scenarios.map { current ->
                    if (current.id == scenario.id) scenario else current
                }
            } else {
                service.scenarios + scenario
            }
            val selectedId = service.selectedScenarioId
                ?.takeIf { id -> scenarios.any { it.id == id } }
                ?: scenarios.firstOrNull()?.id

            service.copy(
                scenarios = scenarios,
                selectedScenarioId = selectedId,
            )
        }
    }

    fun deleteScenario(serviceId: String, scenarioId: String) {
        updateService(serviceId) { service ->
            val scenarios = service.scenarios.filterNot { it.id == scenarioId }
            val selectedId = service.selectedScenarioId
                ?.takeIf { id -> id != scenarioId && scenarios.any { it.id == id } }
                ?: scenarios.firstOrNull()?.id

            service.copy(
                scenarios = scenarios,
                selectedScenarioId = selectedId,
            )
        }
    }

    fun selectScenario(serviceId: String, scenarioId: String) {
        updateService(serviceId) { service ->
            if (service.scenarios.none { it.id == scenarioId }) {
                service
            } else {
                service.copy(selectedScenarioId = scenarioId)
            }
        }
    }

    fun setServiceEnabled(serviceId: String, enabled: Boolean) {
        updateService(serviceId) { it.copy(enabled = enabled) }
    }
}
