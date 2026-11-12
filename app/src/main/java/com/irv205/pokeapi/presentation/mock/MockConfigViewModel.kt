package com.irv205.pokeapi.presentation.mock

import androidx.lifecycle.ViewModel
import com.irv205.pokeapi.data.mock.ApiEnvironment
import com.irv205.pokeapi.data.mock.InMemoryMockConfigProvider
import com.irv205.pokeapi.data.mock.MockService
import com.irv205.pokeapi.data.mock.MutableEnvironmentProvider
import com.irv205.pokeapi.data.mock.PokeMockFixtures
import com.irv205.pokeapi.data.networklog.NetworkCallRecord
import com.irv205.pokeapi.data.networklog.NetworkCallRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class MockConfigUiState(
    val environment: ApiEnvironment = ApiEnvironment.PROD,
    val fallbackToNetwork: Boolean = false,
    val services: List<MockService> = emptyList(),
) {
    val isMockEnabled: Boolean get() = environment == ApiEnvironment.MOCK
    val enabledServices: Int get() = services.count { it.enabled }
}

@HiltViewModel
class MockConfigViewModel @Inject constructor(
    private val environmentProvider: MutableEnvironmentProvider,
    private val configProvider: InMemoryMockConfigProvider,
    private val networkCallRecorder: NetworkCallRecorder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(readState())
    val uiState: StateFlow<MockConfigUiState> = _uiState.asStateFlow()

    /** Llamadas capturadas por [com.irv205.pokeapi.data.networklog.NetworkLoggingInterceptor]. */
    val networkCalls: StateFlow<List<NetworkCallRecord>> = networkCallRecorder.calls

    private val mockJson = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun setMockEnabled(enabled: Boolean) {
        environmentProvider.current = if (enabled) ApiEnvironment.MOCK else ApiEnvironment.PROD
        refreshState()
    }

    fun setFallbackToNetwork(enabled: Boolean) {
        configProvider.fallbackToNetwork = enabled
        refreshState()
    }

    fun setServiceEnabled(serviceId: String, enabled: Boolean) {
        updateService(serviceId) { it.copy(enabled = enabled) }
    }

    fun selectScenario(serviceId: String, scenarioId: String) {
        updateService(serviceId) { it.copy(selectedScenarioId = scenarioId) }
    }

    fun resetFixtures() {
        configProvider.services = PokeMockFixtures.services()
        configProvider.fallbackToNetwork = false
        refreshState()
    }

    fun clearNetworkCalls() {
        networkCallRecorder.clear()
    }

    /** Relee la configuración; útil al volver desde el detalle de un servicio. */
    fun refresh() {
        refreshState()
    }

    /** Exporta la configuración actual como JSON portable (`List<MockService>`). */
    fun exportConfigJson(): String = mockJson.encodeToString(configProvider.services)

    /**
     * Importa configuración desde JSON (`List<MockService>`).
     * @return `null` si se aplicó correctamente, o un mensaje de error.
     */
    fun importConfigJson(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "El JSON está vacío."

        return runCatching {
            mockJson.decodeFromString<List<MockService>>(trimmed)
        }.fold(
            onSuccess = { services ->
                if (services.isEmpty()) {
                    "El JSON no contiene servicios."
                } else {
                    configProvider.services = services
                    refreshState()
                    null
                }
            },
            onFailure = { error ->
                "JSON inválido: ${error.message ?: "formato no reconocido"}"
            },
        )
    }

    private fun updateService(
        serviceId: String,
        transform: (MockService) -> MockService,
    ) {
        configProvider.services = configProvider.services.map { service ->
            if (service.id == serviceId) transform(service) else service
        }
        refreshState()
    }

    private fun refreshState() {
        _uiState.value = readState()
    }

    private fun readState() = MockConfigUiState(
        environment = environmentProvider.current,
        fallbackToNetwork = configProvider.fallbackToNetwork,
        services = configProvider.services,
    )
}
