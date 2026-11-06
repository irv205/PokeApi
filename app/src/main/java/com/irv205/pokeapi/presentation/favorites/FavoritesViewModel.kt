package com.irv205.pokeapi.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irv205.pokeapi.domain.model.FavoritePokemon
import com.irv205.pokeapi.domain.usecase.ObserveFavoritesUseCase
import com.irv205.pokeapi.domain.usecase.RemoveFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val isLoading: Boolean = true,
    val favorites: List<FavoritePokemon> = emptyList(),
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    observeFavorites: ObserveFavoritesUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeFavorites()
                .catch { _uiState.update { it.copy(isLoading = false) } }
                .collect { favorites ->
                    _uiState.update {
                        it.copy(isLoading = false, favorites = favorites)
                    }
                }
        }
    }

    fun remove(id: Int) {
        viewModelScope.launch {
            runCatching { removeFavorite(id) }
        }
    }
}
