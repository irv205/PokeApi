package com.irv205.pokeapi.domain.usecase

import com.irv205.pokeapi.domain.model.FavoritePokemon
import com.irv205.pokeapi.domain.repository.FavoritesRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: FavoritesRepository,
) {
    suspend operator fun invoke(favorite: FavoritePokemon) = repository.toggle(favorite)
}
