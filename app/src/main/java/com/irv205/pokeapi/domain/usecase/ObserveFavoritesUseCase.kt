package com.irv205.pokeapi.domain.usecase

import com.irv205.pokeapi.domain.model.FavoritePokemon
import com.irv205.pokeapi.domain.repository.FavoritesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveFavoritesUseCase @Inject constructor(
    private val repository: FavoritesRepository,
) {
    operator fun invoke(): Flow<List<FavoritePokemon>> = repository.observeFavorites()
}
