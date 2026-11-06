package com.irv205.pokeapi.presentation.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irv205.pokeapi.core.common.Resource
import com.irv205.pokeapi.core.common.toDisplayName
import com.irv205.pokeapi.domain.model.PokemonDetailPage
import com.irv205.pokeapi.presentation.components.AnimatedStatBar
import com.irv205.pokeapi.presentation.components.ErrorState
import com.irv205.pokeapi.presentation.components.EvolutionChainContent
import com.irv205.pokeapi.presentation.components.FavoriteButton
import com.irv205.pokeapi.presentation.components.LoadingState
import com.irv205.pokeapi.presentation.components.PokemonImage
import com.irv205.pokeapi.presentation.components.SectionTitle
import com.irv205.pokeapi.presentation.components.TypeChip
import com.irv205.pokeapi.ui.theme.pokemonTypeColor
import com.irv205.pokeapi.ui.theme.readableContentColor
import java.util.Locale

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onPokemonClick: (Int) -> Unit,
    onOpenEvolution: (Int, Int) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DetailContent(
        state = state,
        onBack = onBack,
        onTabSelected = viewModel::selectTab,
        onRetry = viewModel::load,
        onRetryEvolution = viewModel::retryEvolution,
        onToggleFavorite = viewModel::toggleFavorite,
        onPokemonClick = onPokemonClick,
        onOpenEvolution = onOpenEvolution,
    )
}

@Composable
fun DetailContent(
    state: DetailUiState,
    onBack: () -> Unit,
    onTabSelected: (DetailTab) -> Unit,
    onRetry: () -> Unit,
    onRetryEvolution: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPokemonClick: (Int) -> Unit,
    onOpenEvolution: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading && state.page == null -> {
            Column(modifier = modifier.fillMaxSize()) {
                SimpleTopBar(onBack = onBack)
                LoadingState(message = "Cargando ficha del Pokémon...")
            }
        }

        state.error != null && state.page == null -> {
            Column(modifier = modifier.fillMaxSize()) {
                SimpleTopBar(onBack = onBack)
                ErrorState(message = state.error, onRetry = onRetry)
            }
        }

        state.page != null -> {
            val page = state.page
            Column(modifier = modifier.fillMaxSize()) {
                DetailHeader(
                    page = page,
                    isFavorite = state.isFavorite,
                    onBack = onBack,
                    onToggleFavorite = onToggleFavorite,
                )

                TabRow(
                    selectedTabIndex = state.selectedTab.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    DetailTab.entries.forEach { tab ->
                        Tab(
                            selected = state.selectedTab == tab,
                            onClick = { onTabSelected(tab) },
                            text = {
                                Text(
                                    text = tab.label,
                                    fontWeight = if (state.selectedTab == tab) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Medium
                                    },
                                )
                            },
                        )
                    }
                }

                AnimatedContent(
                    targetState = state.selectedTab,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(150))
                    },
                    label = "detail_tab_content",
                ) { tab ->
                    when (tab) {
                        DetailTab.About -> AboutTab(page = page)
                        DetailTab.Stats -> StatsTab(page = page)
                        DetailTab.Evolution -> EvolutionTab(
                            page = page,
                            evolution = state.evolution,
                            onPokemonClick = onPokemonClick,
                            onRetryEvolution = onRetryEvolution,
                            onOpenEvolution = onOpenEvolution,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
            )
        }
        Text(
            text = "Detalle",
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun DetailHeader(
    page: PokemonDetailPage,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val typeColor = page.pokemon.primaryTypeName.pokemonTypeColor()
    val onTypeColor = typeColor.readableContentColor()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        typeColor,
                        typeColor.copy(alpha = 0.95f),
                        typeColor.copy(alpha = 0.82f),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = onTypeColor,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (page.species.isLegendary || page.species.isMythical) {
                    LegendaryBadge(
                        text = if (page.species.isMythical) "Mítico" else "Legendario",
                        contentColor = onTypeColor,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
                FavoriteButton(
                    isFavorite = isFavorite,
                    onClick = onToggleFavorite,
                    unselectedTint = onTypeColor,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${page.pokemon.id.toString().padStart(3, '0')}",
                        style = MaterialTheme.typography.labelLarge,
                        color = onTypeColor.copy(alpha = 0.82f),
                    )
                    Text(
                        text = page.pokemon.name.toDisplayName(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = onTypeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        page.pokemon.types.forEach { type ->
                            TypeChip(typeName = type.name)
                        }
                    }
                }

                PokemonImage(
                    imageUrl = page.pokemon.imageUrl,
                    contentDescription = page.pokemon.name,
                    modifier = Modifier.size(148.dp),
                )
            }
        }
    }
}

@Composable
private fun LegendaryBadge(text: String, contentColor: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = contentColor.copy(alpha = 0.18f),
        contentColor = contentColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AboutTab(
    page: PokemonDetailPage,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InfoCard(title = "Descripción") {
            Text(
                text = page.species.flavorText.ifBlank { "Sin descripción disponible." },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        InfoCard(title = "Datos básicos") {
            InfoRow(label = "Altura", value = formatHeight(page.pokemon.height))
            InfoRow(label = "Peso", value = formatWeight(page.pokemon.weight))
            InfoRow(
                label = "Categoría",
                value = page.species.genus.ifBlank { "Desconocida" },
            )
            InfoRow(
                label = "Hábitat",
                value = page.species.habitat?.toDisplayName() ?: "Desconocido",
            )
        }

        if (page.pokemon.abilities.isNotEmpty()) {
            AbilitiesCard(page)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbilitiesCard(page: PokemonDetailPage) {
    InfoCard(title = "Habilidades") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            page.pokemon.abilities.forEach { ability ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(
                        text = buildString {
                            append(ability.name.toDisplayName())
                            if (ability.isHidden) append(" · Oculta")
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsTab(
    page: PokemonDetailPage,
    modifier: Modifier = Modifier,
) {
    val typeColor = page.pokemon.primaryTypeName.pokemonTypeColor()
    val total = page.pokemon.stats.sumOf { it.baseStat }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionTitle("Estadísticas base")
                page.pokemon.stats.forEach { stat ->
                    AnimatedStatBar(
                        label = stat.name,
                        value = stat.baseStat,
                        color = typeColor,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Total",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = total.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = typeColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun EvolutionTab(
    page: PokemonDetailPage,
    evolution: Resource<com.irv205.pokeapi.domain.model.EvolutionChain>,
    onPokemonClick: (Int) -> Unit,
    onRetryEvolution: () -> Unit,
    onOpenEvolution: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (evolution) {
        is Resource.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is Resource.Error -> {
            ErrorState(
                message = evolution.message,
                onRetry = onRetryEvolution,
                modifier = modifier,
            )
        }

        is Resource.Success -> {
            val chainId = page.species.evolutionChainId
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EvolutionChainContent(
                    chain = evolution.data,
                    currentPokemonId = page.pokemon.id,
                    onPokemonClick = onPokemonClick,
                )
                if (chainId != null) {
                    Button(
                        onClick = { onOpenEvolution(page.pokemon.id, chainId) },
                        modifier = Modifier.padding(top = 20.dp),
                    ) {
                        Text("Ver pantalla completa")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(title)
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
    }
}

private fun formatHeight(decimeters: Int): String =
    String.format(Locale.getDefault(), "%.1f m", decimeters / 10f)

private fun formatWeight(hectograms: Int): String =
    String.format(Locale.getDefault(), "%.1f kg", hectograms / 10f)
