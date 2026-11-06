package com.irv205.pokeapi.presentation.evolution

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irv205.pokeapi.presentation.components.ErrorState
import com.irv205.pokeapi.presentation.components.EvolutionChainContent
import com.irv205.pokeapi.presentation.components.LoadingState

@Composable
fun EvolutionScreen(
    onBack: () -> Unit,
    onPokemonClick: (Int) -> Unit,
    viewModel: EvolutionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chain = state.chain
    val error = state.error

    Column(modifier = Modifier.fillMaxSize()) {
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
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    text = "Cadena de evolución",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Toca un Pokémon para ver su detalle",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when {
            state.isLoading && chain == null -> {
                LoadingState(message = "Cargando evoluciones...")
            }

            error != null && chain == null -> {
                ErrorState(
                    message = error,
                    onRetry = viewModel::load,
                )
            }

            chain != null -> {
                EvolutionChainContent(
                    chain = chain,
                    currentPokemonId = viewModel.currentPokemonId,
                    onPokemonClick = onPokemonClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                )
            }
        }
    }
}
