package com.irv205.pokeapi.presentation.mock

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.irv205.pokeapi.data.mock.InMemoryMockConfigProvider
import com.irv205.pokeapi.data.mock.MockScenario
import com.irv205.pokeapi.data.mock.MockService
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MockServiceDetailUiState(
    val service: MockService? = null,
    val deleted: Boolean = false,
)

@HiltViewModel
class MockServiceDetailViewModel @Inject constructor(
    private val configProvider: InMemoryMockConfigProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val serviceId: String = savedStateHandle.get<String>("serviceId").orEmpty()

    private val _uiState = MutableStateFlow(readState())
    val uiState: StateFlow<MockServiceDetailUiState> = _uiState.asStateFlow()

    fun updateService(
        name: String,
        method: String,
        pathPattern: String,
        enabled: Boolean,
    ) {
        val cleanName = name.trim()
        val cleanPath = pathPattern.trim()
        if (cleanName.isEmpty() || cleanPath.isEmpty()) return

        configProvider.updateService(serviceId) { service ->
            service.copy(
                name = cleanName,
                method = method.trim().uppercase().ifBlank { "GET" },
                pathPattern = cleanPath,
                enabled = enabled,
            )
        }
        refresh()
    }

    fun selectScenario(scenarioId: String) {
        configProvider.selectScenario(serviceId, scenarioId)
        refresh()
    }

    fun newScenario(): MockScenario = MockScenario(
        id = "scenario_${UUID.randomUUID()}",
        name = "Nueva respuesta",
        responseBody = "{}",
    )

    fun saveScenario(scenario: MockScenario) {
        val normalized = scenario.copy(
            id = scenario.id.ifBlank { "scenario_${UUID.randomUUID()}" },
            name = scenario.name.trim().ifBlank { "Respuesta ${scenario.statusCode}" },
            contentType = scenario.contentType.trim().ifBlank { "application/json" },
            statusCode = scenario.statusCode.coerceIn(100, 599),
            delayMs = scenario.delayMs.coerceAtLeast(0L),
        )
        configProvider.upsertScenario(serviceId, normalized)
        refresh()
    }

    fun deleteScenario(scenarioId: String) {
        configProvider.deleteScenario(serviceId, scenarioId)
        refresh()
    }

    fun deleteService() {
        configProvider.deleteService(serviceId)
        _uiState.value = MockServiceDetailUiState(service = null, deleted = true)
    }

    fun refresh() {
        _uiState.value = readState()
    }

    private fun readState(): MockServiceDetailUiState = MockServiceDetailUiState(
        service = configProvider.findService(serviceId),
    )
}
