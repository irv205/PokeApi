package com.irv205.pokeapi.domain.usecase

import com.irv205.pokeapi.domain.repository.FavoritesRepository
import javax.inject.Inject

class RemoveFavoriteUseCase @Inject constructor(
    private val repository: FavoritesRepository,
) {
    suspend operator fun invoke(id: Int) = repository.remove(id)
}
