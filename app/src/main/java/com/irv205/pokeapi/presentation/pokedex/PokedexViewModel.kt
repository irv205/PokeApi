package com.irv205.pokeapi.presentation.pokedex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irv205.pokeapi.core.common.Resource
import com.irv205.pokeapi.domain.model.FavoritePokemon
import com.irv205.pokeapi.domain.model.PokemonSummary
import com.irv205.pokeapi.domain.usecase.GetPokemonPageUseCase
import com.irv205.pokeapi.domain.usecase.ObserveFavoriteIdsUseCase
import com.irv205.pokeapi.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class PokedexUiState(
    val pokemon: List<PokemonSummary> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val isAppending: Boolean = false,
    val error: String? = null,
    val endReached: Boolean = false,
    val favoriteIds: Set<Int> = emptySet(),
) {
    val isSearching: Boolean get() = query.isNotBlank()

    val visiblePokemon: List<PokemonSummary>
        get() {
            val term = query.trim()
            if (term.isEmpty()) return pokemon
            val idTerm = term.removePrefix("#")
            return pokemon.filter { pokemon ->
                pokemon.name.contains(term, ignoreCase = true) ||
                    (idTerm.toIntOrNull() != null && pokemon.id == idTerm.toInt())
            }
        }

    val isEmptySearchResult: Boolean
        get() = isSearching && visiblePokemon.isEmpty() && !isLoading
}

@HiltViewModel
class PokedexViewModel @Inject constructor(
    private val getPokemonPage: GetPokemonPageUseCase,
    observeFavoriteIds: ObserveFavoriteIdsUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PokedexUiState())
    val uiState: StateFlow<PokedexUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        observeFavorites(observeFavoriteIds)
        loadNextPage()
    }

    private fun observeFavorites(observeFavoriteIds: ObserveFavoriteIdsUseCase) {
        viewModelScope.launch {
            observeFavoriteIds()
                .catch { _uiState.update { state -> state.copy(favoriteIds = emptySet()) } }
                .collect { ids ->
                    _uiState.update { state -> state.copy(favoriteIds = ids) }
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onClearQuery() {
        _uiState.update { it.copy(query = "") }
    }

    fun loadNextPage() {
        val current = _uiState.value
        if (current.isLoading || current.isAppending || current.endReached || current.error != null) {
            return
        }

        val isFirstPage = current.pokemon.isEmpty()
        _uiState.update {
            it.copy(
                isLoading = isFirstPage,
                isAppending = !isFirstPage,
                error = null,
            )
        }

        loadJob = viewModelScope.launch {
            when (val result = getPokemonPage(PAGE_SIZE, current.pokemon.size)) {
                is Resource.Success -> {
                    val knownIds = _uiState.value.pokemon.mapTo(mutableSetOf()) { it.id }
                    val newItems = result.data.items.filter { knownIds.add(it.id) }
                    _uiState.update { state ->
                        state.copy(
                            pokemon = if (isFirstPage) newItems else state.pokemon + newItems,
                            isLoading = false,
                            isAppending = false,
                            error = null,
                            endReached = !result.data.hasMore || result.data.items.isEmpty(),
                        )
                    }
                }

                is Resource.Error -> _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isAppending = false,
                        error = result.message,
                    )
                }

                Resource.Loading -> Unit
            }
        }
    }

    fun refresh() {
        loadJob?.cancel()
        _uiState.update { state ->
            state.copy(
                pokemon = emptyList(),
                isLoading = false,
                isAppending = false,
                error = null,
                endReached = false,
            )
        }
        loadNextPage()
    }

    fun retry() {
        if (_uiState.value.error == null) return
        _uiState.update { it.copy(error = null) }
        loadNextPage()
    }

    fun onFavoriteClick(pokemon: PokemonSummary) {
        viewModelScope.launch {
            toggleFavorite(
                FavoritePokemon(
                    id = pokemon.id,
                    name = pokemon.name,
                    imageUrl = pokemon.imageUrl,
                ),
            )
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
