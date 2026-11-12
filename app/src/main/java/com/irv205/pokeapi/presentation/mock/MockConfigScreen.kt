package com.irv205.pokeapi.presentation.mock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irv205.pokeapi.data.mock.ApiEnvironment
import com.irv205.pokeapi.data.mock.MockScenario
import com.irv205.pokeapi.data.mock.MockService
import com.irv205.pokeapi.data.networklog.NetworkCallRecord
import com.irv205.pokeapi.data.networklog.NetworkCallSource
import com.irv205.pokeapi.data.networklog.NetworkHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.json.Json

private const val TAB_SETTINGS = 0
private const val TAB_CALLS = 1

@Composable
fun MockConfigScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    onOpenService: (String) -> Unit = {},
    viewModel: MockConfigViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val calls by viewModel.networkCalls.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_SETTINGS) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        MockTopBar(onBack = onBack)

        MockTabs(
            selectedTab = selectedTab,
            callCount = calls.size,
            onTabSelected = { selectedTab = it },
        )

        when (selectedTab) {
            TAB_SETTINGS -> {
                MockSettingsTab(
                    state = state,
                    onMockEnabledChange = viewModel::setMockEnabled,
                    onFallbackChange = viewModel::setFallbackToNetwork,
                    onResetFixtures = viewModel::resetFixtures,
                    onExportConfig = viewModel::exportConfigJson,
                    onImportConfig = viewModel::importConfigJson,
                    onServiceEnabledChange = viewModel::setServiceEnabled,
                    onScenarioSelected = viewModel::selectScenario,
                    onOpenService = onOpenService,
                    modifier = Modifier.weight(1f),
                )
                ApplyBar(onApplied = onApplied)
            }

            else -> {
                NetworkCallsTab(
                    calls = calls,
                    onClear = viewModel::clearNetworkCalls,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MockTopBar(onBack: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("mock_back"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mocks de red",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Herramienta de debug",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun MockTabs(
    selectedTab: Int,
    callCount: Int,
    onTabSelected: (Int) -> Unit,
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Tab(
            selected = selectedTab == TAB_SETTINGS,
            onClick = { onTabSelected(TAB_SETTINGS) },
            text = { Text("Configuración") },
            modifier = Modifier.testTag("mock_tab_config"),
        )
        Tab(
            selected = selectedTab == TAB_CALLS,
            onClick = { onTabSelected(TAB_CALLS) },
            text = {
                Text(if (callCount > 0) "Llamadas ($callCount)" else "Llamadas")
            },
            modifier = Modifier.testTag("mock_tab_calls"),
        )
    }
}

@Composable
private fun MockSettingsTab(
    state: MockConfigUiState,
    onMockEnabledChange: (Boolean) -> Unit,
    onFallbackChange: (Boolean) -> Unit,
    onResetFixtures: () -> Unit,
    onExportConfig: () -> String,
    onImportConfig: (String) -> String?,
    onServiceEnabledChange: (String, Boolean) -> Unit,
    onScenarioSelected: (String, String) -> Unit,
    onOpenService: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    var exportJson by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        item { EnvironmentStatusCard(state = state) }

        item {
            SettingsCard(
                state = state,
                onMockEnabledChange = onMockEnabledChange,
                onFallbackChange = onFallbackChange,
            )
        }

        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Portabilidad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Importa o exporta la configuración completa (List<MockService>) " +
                            "como JSON para reutilizarla en otros módulos o builds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                importText = ""
                                importError = null
                                showImportDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mock_import"),
                        ) {
                            Text("Importar JSON")
                        }
                        OutlinedButton(
                            onClick = { exportJson = onExportConfig() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mock_export"),
                        ) {
                            Text("Exportar JSON")
                        }
                    }
                    OutlinedButton(
                        onClick = onResetFixtures,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mock_reset"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restablecer fixtures")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Servicios",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${state.enabledServices}/${state.services.size} activos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(
            items = state.services,
            key = { it.id },
        ) { service ->
            MockServiceCard(
                service = service,
                onEnabledChange = { enabled ->
                    onServiceEnabledChange(service.id, enabled)
                },
                onScenarioSelected = { scenarioId ->
                    onScenarioSelected(service.id, scenarioId)
                },
                onOpenDetails = { onOpenService(service.id) },
            )
        }
        }

        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Importar mocks (JSON)") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Pega un JSON con la forma List<MockService>. " +
                                "Reemplazará la configuración actual.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        importError?.let { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        OutlinedTextField(
                            value = importText,
                            onValueChange = {
                                importText = it
                                importError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .testTag("mock_import_json"),
                            label = { Text("JSON") },
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val error = onImportConfig(importText)
                            if (error == null) {
                                showImportDialog = false
                                importText = ""
                            } else {
                                importError = error
                            }
                        },
                        modifier = Modifier.testTag("mock_import_confirm"),
                    ) {
                        Text("Importar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancelar")
                    }
                },
            )
        }

        exportJson?.let { json ->
            AlertDialog(
                onDismissRequest = { exportJson = null },
                title = { Text("Exportar mocks (JSON)") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Copia este JSON para reutilizarlo en otro módulo o build.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        SelectionContainer {
                            OutlinedTextField(
                                value = json,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .testTag("mock_export_json"),
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(json))
                        },
                        modifier = Modifier.testTag("mock_export_copy"),
                    ) {
                        Text("Copiar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { exportJson = null }) {
                        Text("Cerrar")
                    }
                },
            )
        }
    }
}

@Composable
private fun EnvironmentStatusCard(state: MockConfigUiState) {
    val isMock = state.isMockEnabled
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMock) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isMock) "MOCK activo" else "PROD activo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isMock) {
                    "Las llamadas de PokeAPI se responden desde los fixtures locales."
                } else {
                    "La app está consumiendo la API real de PokeAPI."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SettingsCard(
    state: MockConfigUiState,
    onMockEnabledChange: (Boolean) -> Unit,
    onFallbackChange: (Boolean) -> Unit,
) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            SettingRow(
                title = "Usar mocks locales",
                description = "Alterna entre la API real (PROD) y los fixtures locales (MOCK).",
                checked = state.isMockEnabled,
                onCheckedChange = onMockEnabledChange,
                testTag = "mock_environment_switch",
            )

            HorizontalDivider()

            SettingRow(
                title = "Fallback a la red",
                description = "Si no hay match, deja pasar la request. Recomendado solo para QA manual.",
                checked = state.fallbackToNetwork,
                onCheckedChange = onFallbackChange,
                testTag = "mock_fallback_switch",
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
        )
    }
}

@Composable
private fun MockServiceCard(
    service: MockService,
    onEnabledChange: (Boolean) -> Unit,
    onScenarioSelected: (String) -> Unit,
    onOpenDetails: () -> Unit,
) {
    val selectedScenario = service.selectedScenario()
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (service.enabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            },
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${service.method} ${service.pathPattern}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Switch(
                    checked = service.enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.testTag("mock_service_${service.id}"),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (service.scenarios.isNotEmpty()) {
                    Box {
                        TextButton(
                            onClick = { menuExpanded = true },
                            enabled = service.enabled,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = selectedScenario?.name ?: "Sin escenario",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Elegir escenario",
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            service.scenarios.forEach { scenario ->
                                ScenarioItem(
                                    scenario = scenario,
                                    selected = scenario.id == selectedScenario?.id,
                                    onClick = {
                                        onScenarioSelected(scenario.id)
                                        menuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = onOpenDetails,
                    modifier = Modifier.testTag("mock_service_details_${service.id}"),
                ) {
                    Text("${service.scenarios.size} respuestas")
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Ver detalle del servicio",
                    )
                }
            }
        }
    }
}

@Composable
private fun ScenarioItem(
    scenario: MockScenario,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Column {
                Text(
                    text = scenario.name,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    text = "HTTP ${scenario.statusCode} · ${scenario.delayMs} ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun NetworkCallsTab(
    calls: List<NetworkCallRecord>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Llamadas de red",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (calls.isEmpty()) {
                        "Aún no se ha registrado ninguna llamada"
                    } else {
                        "${calls.size} registradas · más recientes primero"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onClear,
                enabled = calls.isNotEmpty(),
                modifier = Modifier.testTag("clear_calls"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Limpiar")
            }
        }

        if (calls.isEmpty()) {
            EmptyCallsState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    items = calls,
                    key = { it.id },
                ) { call ->
                    NetworkCallCard(call = call)
                }
            }
        }
    }
}

@Composable
private fun EmptyCallsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Sin llamadas todavía",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Navega por la Pokédex o recarga el listado y aquí verás cada request y response.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NetworkCallCard(call: NetworkCallRecord) {
    var expanded by rememberSaveable(call.id) { mutableStateOf(false) }
    val statusColor = statusColor(call.statusCode)

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("network_call_${call.id}"),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MethodBadge(method = call.method)

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = call.endpoint,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(
                            label = call.statusLabel,
                            color = statusColor,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        SourceBadge(source = call.source)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${call.durationMs} ms · ${formatCallTime(call.startedAtEpochMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) "Contraer llamada" else "Expandir llamada",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (call.source == NetworkCallSource.MOCK && call.mockService != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = buildString {
                        append(call.mockService)
                        call.mockScenario?.let { append(" · ").append(it) }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (call.error != null && !expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = call.error,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (expanded) {
                CallDetails(call = call)
            }
        }
    }
}

@Composable
private fun CallDetails(call: NetworkCallRecord) {
    Column {
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailSection(title = "Endpoint") {
                    InfoRow(label = "Método", value = call.method)
                    InfoRow(
                        label = "URL",
                        value = call.url,
                        valueTestTag = "call_url",
                    )
                    InfoRow(label = "Path", value = call.path)
                    InfoRow(label = "Query", value = call.query ?: "(sin query)")
                }

                DetailSection(title = "Request headers") {
                    HeadersBlock(headers = call.requestHeaders)
                }

                DetailSection(title = "Request body") {
                    CodeBlock(
                        text = call.requestBody ?: "(sin body)",
                        testTag = "call_request_body",
                    )
                }

                DetailSection(title = "Response") {
                    InfoRow(
                        label = "Status",
                        value = "${call.statusCode ?: "—"} ${call.statusMessage.orEmpty()}".trim(),
                    )
                    if (call.source == NetworkCallSource.MOCK) {
                        InfoRow(label = "Servicio", value = call.mockService ?: "—")
                        InfoRow(label = "Escenario", value = call.mockScenario ?: "—")
                    }
                    call.error?.let { error ->
                        InfoRow(label = "Error", value = error)
                    }
                }

                DetailSection(title = "Response headers") {
                    HeadersBlock(headers = call.responseHeaders)
                }

                DetailSection(title = "Response body") {
                    CodeBlock(
                        text = formatBody(call.responseBody),
                        testTag = "call_response_body",
                    )
                    if (call.responseBodyTruncated) {
                        Text(
                            text = "Respuesta truncada a 64 KB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueTestTag: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(84.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier
                .weight(1f)
                .then(
                    if (valueTestTag != null) Modifier.testTag(valueTestTag) else Modifier,
                ),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun HeadersBlock(headers: List<NetworkHeader>) {
    if (headers.isEmpty()) {
        Text(
            text = "(sin headers)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        headers.forEach { header ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "${header.name}:",
                    modifier = Modifier.width(128.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = header.value,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CodeBlock(
    text: String,
    testTag: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .testTag(testTag),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun MethodBadge(method: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = method,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun StatusBadge(
    label: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SourceBadge(source: NetworkCallSource) {
    val isMock = source == NetworkCallSource.MOCK
    val color = if (isMock) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = if (isMock) "MOCK" else "RED",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun statusColor(code: Int?): androidx.compose.ui.graphics.Color = when {
    code == null -> MaterialTheme.colorScheme.error
    code in 200..299 -> MaterialTheme.colorScheme.primary
    code in 400..499 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}

private fun formatCallTime(epochMs: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(epochMs))

private val debugJson = Json { prettyPrint = true }

private fun formatBody(body: String?): String {
    if (body.isNullOrBlank()) return "(sin body)"
    return runCatching {
        debugJson.parseToJsonElement(body).toString()
    }.getOrDefault(body)
}

@Composable
private fun ApplyBar(onApplied: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column {
            HorizontalDivider()
            Button(
                onClick = onApplied,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("mock_apply"),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aplicar y recargar Pokédex")
            }
        }
    }
}
