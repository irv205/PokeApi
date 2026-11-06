package com.irv205.pokeapi.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.irv205.pokeapi.core.common.Resource
import com.irv205.pokeapi.domain.model.EvolutionChain
import com.irv205.pokeapi.domain.model.FavoritePokemon
import com.irv205.pokeapi.domain.model.PokemonDetailPage
import com.irv205.pokeapi.domain.usecase.GetEvolutionChainUseCase
import com.irv205.pokeapi.domain.usecase.GetPokemonDetailPageUseCase
import com.irv205.pokeapi.domain.usecase.ObserveFavoriteIdsUseCase
import com.irv205.pokeapi.domain.usecase.ToggleFavoriteUseCase
import com.irv205.pokeapi.presentation.navigation.DetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DetailTab(val label: String) {
    About("About"),
    Stats("Stats"),
    Evolution("Evolución"),
}

data class DetailUiState(
    val isLoading: Boolean = true,
    val page: PokemonDetailPage? = null,
    val evolution: Resource<EvolutionChain> = Resource.Loading,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val selectedTab: DetailTab = DetailTab.About,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPokemonDetailPage: GetPokemonDetailPageUseCase,
    private val getEvolutionChain: GetEvolutionChainUseCase,
    observeFavoriteIds: ObserveFavoriteIdsUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {

    private val pokemonId: Int = savedStateHandle.toRoute<DetailRoute>().pokemonId

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        observeFavorites(observeFavoriteIds)
        load()
    }

    private fun observeFavorites(observeFavoriteIds: ObserveFavoriteIdsUseCase) {
        viewModelScope.launch {
            observeFavoriteIds().collect { ids ->
                _uiState.update { it.copy(isFavorite = pokemonId in ids) }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getPokemonDetailPage(pokemonId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            page = result.data,
                            error = null,
                        )
                    }
                    loadEvolution(result.data.species.evolutionChainId)
                }

                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }

                Resource.Loading -> Unit
            }
        }
    }

    private fun loadEvolution(chainId: Int?) {
        if (chainId == null) {
            _uiState.update {
                it.copy(evolution = Resource.Error("Este Pokémon no tiene una cadena de evolución disponible."))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(evolution = Resource.Loading) }
            val result = getEvolutionChain(chainId)
            _uiState.update { it.copy(evolution = result) }
        }
    }

    fun selectTab(tab: DetailTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun retryEvolution() {
        loadEvolution(_uiState.value.page?.species?.evolutionChainId)
    }

    fun toggleFavorite() {
        val page = _uiState.value.page ?: return
        viewModelScope.launch {
            toggleFavorite(
                FavoritePokemon(
                    id = page.pokemon.id,
                    name = page.pokemon.name,
                    imageUrl = page.pokemon.imageUrl,
                    primaryType = page.pokemon.primaryTypeName,
                ),
            )
        }
    }
}
