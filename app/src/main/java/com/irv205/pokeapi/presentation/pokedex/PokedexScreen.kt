package com.irv205.pokeapi.presentation.pokedex

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irv205.pokeapi.core.common.toDisplayName
import com.irv205.pokeapi.domain.model.PokemonSummary
import com.irv205.pokeapi.presentation.components.EmptyState
import com.irv205.pokeapi.presentation.components.ErrorState
import com.irv205.pokeapi.presentation.components.FavoriteButton
import com.irv205.pokeapi.presentation.components.LoadingState
import com.irv205.pokeapi.presentation.components.PokemonImage

@Composable
fun PokedexScreen(
    onPokemonClick: (Int) -> Unit,
    viewModel: PokedexViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PokedexContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onClearQuery = viewModel::onClearQuery,
        onRetry = viewModel::retry,
        onLoadMore = viewModel::loadNextPage,
        onPokemonClick = onPokemonClick,
        onFavoriteClick = viewModel::onFavoriteClick,
    )
}

@Composable
fun PokedexContent(
    state: PokedexUiState,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPokemonClick: (Int) -> Unit,
    onFavoriteClick: (PokemonSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PokedexHeader(pokemonCount = state.pokemon.size)

        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Buscar por nombre o ID") },
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Search, contentDescription = null)
            },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = onClearQuery) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Limpiar búsqueda",
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        when {
            state.isLoading && state.pokemon.isEmpty() -> {
                LoadingState()
            }

            state.error != null && state.pokemon.isEmpty() -> {
                ErrorState(message = state.error, onRetry = onRetry)
            }

            state.isEmptySearchResult -> {
                EmptyState(
                    title = "Sin resultados",
                    message = "No encontramos Pokémon para \"${state.query}\". Prueba con otro nombre o ID.",
                )
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = state.visiblePokemon,
                        key = { it.id },
                    ) { pokemon ->
                        PokemonCard(
                            pokemon = pokemon,
                            isFavorite = pokemon.id in state.favoriteIds,
                            onClick = { onPokemonClick(pokemon.id) },
                            onFavoriteClick = { onFavoriteClick(pokemon) },
                        )
                    }

                    if (!state.isSearching) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            PokedexFooter(
                                state = state,
                                onRetry = onRetry,
                                onLoadMore = onLoadMore,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PokedexHeader(pokemonCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Pokédex",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = when (pokemonCount) {
                    0 -> "Explora el mundo Pokémon"
                    1 -> "1 Pokémon cargado"
                    else -> "$pokemonCount Pokémon cargados"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Text(
                text = "#001",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PokemonCard(
    pokemon: PokemonSummary,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                PokemonImage(
                    imageUrl = pokemon.imageUrl,
                    contentDescription = pokemon.name,
                    modifier = Modifier.size(104.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = pokemon.name.toDisplayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = "#${pokemon.id.toString().padStart(3, '0')}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FavoriteButton(
                isFavorite = isFavorite,
                onClick = onFavoriteClick,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun PokedexFooter(
    state: PokedexUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.isAppending -> {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }

            state.error != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = onRetry) {
                        Text("Reintentar")
                    }
                }
            }

            state.endReached -> {
                Text(
                    text = "¡Has visto todos los Pokémon disponibles!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                LaunchedEffect(state.pokemon.size) {
                    onLoadMore()
                }
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        }
    }
}
