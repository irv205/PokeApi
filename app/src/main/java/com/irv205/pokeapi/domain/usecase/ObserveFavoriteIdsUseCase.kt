package com.irv205.pokeapi.domain.usecase

import com.irv205.pokeapi.domain.repository.FavoritesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveFavoriteIdsUseCase @Inject constructor(
    private val repository: FavoritesRepository,
) {
    operator fun invoke(): Flow<Set<Int>> = repository.observeFavoriteIds()
}
