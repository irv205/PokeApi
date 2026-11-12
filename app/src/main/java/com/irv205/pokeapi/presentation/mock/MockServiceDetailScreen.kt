package com.irv205.pokeapi.presentation.mock

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irv205.pokeapi.data.mock.MockScenario
import com.irv205.pokeapi.data.mock.MockService

@Composable
fun MockServiceDetailScreen(
    onBack: () -> Unit,
    viewModel: MockServiceDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var editorScenario by remember { mutableStateOf<MockScenario?>(null) }
    var editorIsNew by remember { mutableStateOf(false) }
    var scenarioToDelete by remember { mutableStateOf<MockScenario?>(null) }
    var showDeleteService by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    if (state.deleted) return

    val editor = editorScenario
    if (editor != null) {
        BackHandler { editorScenario = null }
        ScenarioEditorScreen(
            initial = editor,
            isNew = editorIsNew,
            onCancel = { editorScenario = null },
            onSave = { updated ->
                viewModel.saveScenario(updated)
                editorScenario = null
            },
        )
        return
    }

    val service = state.service
    if (service == null) {
        MissingServiceContent(onBack = onBack)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        DetailTopBar(
            title = service.name,
            subtitle = "${service.method} ${service.pathPattern}",
            onBack = onBack,
            backTestTag = "service_detail_back",
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("service_detail_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ServiceEditorCard(
                    service = service,
                    onSave = viewModel::updateService,
                )
            }

            item {
                ResponsesHeader(count = service.scenarios.size)
            }

            if (service.scenarios.isEmpty()) {
                item { EmptyResponsesCard() }
            }

            items(
                items = service.scenarios,
                key = { it.id },
            ) { scenario ->
                ScenarioCard(
                    scenario = scenario,
                    isActive = service.selectedScenario()?.id == scenario.id,
                    onSelect = { viewModel.selectScenario(scenario.id) },
                    onEdit = {
                        editorScenario = scenario
                        editorIsNew = false
                    },
                    onDelete = { scenarioToDelete = scenario },
                )
            }

            item {
                DeleteServiceSection(onDelete = { showDeleteService = true })
            }
        }

        AddResponseBar(
            onAdd = {
                editorScenario = viewModel.newScenario()
                editorIsNew = true
            },
        )
    }

    scenarioToDelete?.let { scenario ->
        AlertDialog(
            onDismissRequest = { scenarioToDelete = null },
            title = { Text("Eliminar respuesta") },
            text = {
                Text("¿Eliminar \"${scenario.name}\"? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteScenario(scenario.id)
                        scenarioToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_scenario"),
                ) {
                    Text(
                        text = "Eliminar",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { scenarioToDelete = null }) {
                    Text("Cancelar")
                }
            },
        )
    }

    if (showDeleteService) {
        AlertDialog(
            onDismissRequest = { showDeleteService = false },
            title = { Text("Eliminar servicio") },
            text = {
                Text(
                    "¿Eliminar \"${service.name}\" y todas sus respuestas? " +
                        "Esta acción no se puede deshacer.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteService = false
                        viewModel.deleteService()
                    },
                    modifier = Modifier.testTag("confirm_delete_service"),
                ) {
                    Text(
                        text = "Eliminar",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteService = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun DetailTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    backTestTag: String,
) {
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
                    modifier = Modifier.testTag(backTestTag),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun MissingServiceContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        DetailTopBar(
            title = "Servicio",
            subtitle = "Detalle del mock",
            onBack = onBack,
            backTestTag = "service_detail_back",
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Servicio no encontrado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Puede que se haya eliminado desde otra pantalla.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Volver")
                }
            }
        }
    }
}

@Composable
private fun ServiceEditorCard(
    service: MockService,
    onSave: (name: String, method: String, pathPattern: String, enabled: Boolean) -> Unit,
) {
    var name by rememberSaveable(service.id) { mutableStateOf(service.name) }
    var method by rememberSaveable(service.id) { mutableStateOf(service.method) }
    var pathPattern by rememberSaveable(service.id) { mutableStateOf(service.pathPattern) }
    var enabled by rememberSaveable(service.id) { mutableStateOf(service.enabled) }

    val canSave = name.isNotBlank() && pathPattern.isNotBlank()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Servicio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Define cómo se identifica la llamada. Las respuestas se configuran debajo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("service_name"),
                label = { Text("Nombre") },
                singleLine = true,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = method,
                    onValueChange = { method = it.uppercase() },
                    modifier = Modifier
                        .weight(0.38f)
                        .testTag("service_method"),
                    label = { Text("Método") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = pathPattern,
                    onValueChange = { pathPattern = it },
                    modifier = Modifier
                        .weight(0.62f)
                        .testTag("service_path"),
                    label = { Text("Path pattern") },
                    singleLine = true,
                )
            }
            Text(
                text = "Path acepta rutas exactas, plantillas como {id} o regex.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Servicio habilitado",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Si está apagado, las llamadas no se mockean.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    modifier = Modifier.testTag("service_enabled"),
                )
            }

            Button(
                onClick = { onSave(name, method, pathPattern, enabled) },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_service"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Guardar servicio")
            }
        }
    }
}

@Composable
private fun ResponsesHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Respuestas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "La respuesta activa se usa cuando ningún filtro de request coincide.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun EmptyResponsesCard() {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sin respuestas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Agrega al menos una respuesta para que este servicio pueda mockear llamadas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScenarioCard(
    scenario: MockScenario,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val filterSummary = requestFilterSummary(scenario)
    val statusColor = scenarioStatusColor(scenario.statusCode)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            },
        ),
        modifier = Modifier.testTag("scenario_card_${scenario.id}"),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                RadioButton(
                    selected = isActive,
                    onClick = onSelect,
                    modifier = Modifier.testTag("scenario_active_${scenario.id}"),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp),
                ) {
                    Text(
                        text = scenario.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HttpStatusBadge(
                            statusCode = scenario.statusCode,
                            color = statusColor,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${scenario.delayMs} ms · ${scenario.contentType}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = filterSummary ?: "Fallback · sin filtros de request",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_scenario_${scenario.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Editar")
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_scenario_${scenario.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Eliminar ${scenario.name}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun HttpStatusBadge(
    statusCode: Int,
    color: Color,
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = statusCode.toString(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun scenarioStatusColor(statusCode: Int): Color = when {
    statusCode in 200..299 -> MaterialTheme.colorScheme.primary
    statusCode in 400..499 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}

private fun requestFilterSummary(scenario: MockScenario): String? {
    val parts = buildList {
        scenario.requestQuery.trim().takeIf { it.isNotEmpty() }?.let {
            add("query: $it")
        }
        scenario.requestBody.trim().takeIf { it.isNotEmpty() }?.let {
            add("body contiene: $it")
        }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
private fun DeleteServiceSection(onDelete: () -> Unit) {
    OutlinedButton(
        onClick = onDelete,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag("delete_service"),
        shape = RoundedCornerShape(14.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Eliminar servicio",
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun AddResponseBar(onAdd: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column {
            HorizontalDivider()
            Button(
                onClick = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("add_response"),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar respuesta")
            }
        }
    }
}

@Composable
private fun ScenarioEditorScreen(
    initial: MockScenario,
    isNew: Boolean,
    onCancel: () -> Unit,
    onSave: (MockScenario) -> Unit,
) {
    var name by rememberSaveable(initial.id) { mutableStateOf(initial.name) }
    var statusCode by rememberSaveable(initial.id) {
        mutableStateOf(initial.statusCode.toString())
    }
    var delayMs by rememberSaveable(initial.id) { mutableStateOf(initial.delayMs.toString()) }
    var contentType by rememberSaveable(initial.id) { mutableStateOf(initial.contentType) }
    var queryEnabled by rememberSaveable(initial.id) {
        mutableStateOf(initial.requestQuery.isNotBlank())
    }
    var requestQuery by rememberSaveable(initial.id) { mutableStateOf(initial.requestQuery) }
    var requestBody by rememberSaveable(initial.id) { mutableStateOf(initial.requestBody) }
    var responseBody by rememberSaveable(initial.id) { mutableStateOf(initial.responseBody) }

    val parsedStatus = statusCode.toIntOrNull()
    val statusIsValid = parsedStatus != null && parsedStatus in 100..599
    val canSave = name.isNotBlank() && statusIsValid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
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
                        onClick = onCancel,
                        modifier = Modifier.testTag("cancel_scenario"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancelar",
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isNew) "Nueva respuesta" else "Editar respuesta",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (isNew) {
                                "Configura request y response"
                            } else {
                                name
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(
                        onClick = {
                            onSave(
                                initial.copy(
                                    name = name,
                                    statusCode = parsedStatus ?: initial.statusCode,
                                    delayMs = delayMs.toLongOrNull() ?: 0L,
                                    contentType = contentType,
                                    requestQuery = if (queryEnabled) requestQuery else "",
                                    requestBody = requestBody,
                                    responseHeaders = initial.responseHeaders,
                                    responseBody = responseBody,
                                ),
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier.testTag("save_scenario"),
                    ) {
                        Text("Guardar")
                    }
                }
                HorizontalDivider()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scenario_name"),
                label = { Text("Nombre de la respuesta") },
                singleLine = true,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = statusCode,
                    onValueChange = { value ->
                        if (value.all(Char::isDigit)) statusCode = value
                    },
                    modifier = Modifier
                        .weight(0.5f)
                        .testTag("scenario_status"),
                    label = { Text("Status HTTP") },
                    singleLine = true,
                    isError = statusCode.isNotEmpty() && !statusIsValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = delayMs,
                    onValueChange = { value ->
                        if (value.all(Char::isDigit)) delayMs = value
                    },
                    modifier = Modifier
                        .weight(0.5f)
                        .testTag("scenario_delay"),
                    label = { Text("Delay (ms)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            OutlinedTextField(
                value = contentType,
                onValueChange = { contentType = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scenario_content_type"),
                label = { Text("Content-Type") },
                singleLine = true,
            )

            Text(
                text = "Request",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Los filtros son opcionales. Si no aplican, esta respuesta actúa " +
                    "como fallback activo cuando ninguna otra coincide.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = queryEnabled,
                    onCheckedChange = { queryEnabled = it },
                    modifier = Modifier.testTag("scenario_query_enabled"),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Filtrar por query params",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Actívalo para aplicar esta respuesta solo con ciertos parámetros.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = requestQuery,
                onValueChange = { requestQuery = it },
                enabled = queryEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scenario_request_query"),
                label = { Text("Query params") },
                placeholder = { Text("limit=20&offset=0") },
                supportingText = {
                    Text("Todos los pares deben coincidir. Un par por línea o separados por &.")
                },
                minLines = 2,
            )

            OutlinedTextField(
                value = requestBody,
                onValueChange = { requestBody = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scenario_request_body"),
                label = { Text("Request body (JSON o texto)") },
                placeholder = { Text("{\"key\":\"value\"}") },
                supportingText = {
                    Text("Puedes pegar JSON o texto; el match verifica que esté contenido en el body del request.")
                },
                minLines = 4,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                ),
            )

            Text(
                text = "Response",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            OutlinedTextField(
                value = responseBody,
                onValueChange = { responseBody = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scenario_response_body"),
                label = { Text("Response body (JSON)") },
                placeholder = { Text("{\n  \"detail\": \"Mensaje de error\"\n}") },
                supportingText = { Text("Pega el JSON o texto exacto que devolverá el mock.") },
                minLines = 10,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                ),
            )
        }
    }
}
