package com.irv205.pokeapi.presentation.evolution

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.irv205.pokeapi.core.common.Resource
import com.irv205.pokeapi.domain.model.EvolutionChain
import com.irv205.pokeapi.domain.usecase.GetEvolutionChainUseCase
import com.irv205.pokeapi.presentation.navigation.EvolutionRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EvolutionUiState(
    val isLoading: Boolean = true,
    val chain: EvolutionChain? = null,
    val error: String? = null,
)

@HiltViewModel
class EvolutionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEvolutionChain: GetEvolutionChainUseCase,
) : ViewModel() {

    private val route: EvolutionRoute = savedStateHandle.toRoute()
    val currentPokemonId: Int = route.pokemonId

    private val _uiState = MutableStateFlow(EvolutionUiState())
    val uiState: StateFlow<EvolutionUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getEvolutionChain(route.chainId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, chain = result.data, error = null)
                }

                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }

                Resource.Loading -> Unit
            }
        }
    }
}
