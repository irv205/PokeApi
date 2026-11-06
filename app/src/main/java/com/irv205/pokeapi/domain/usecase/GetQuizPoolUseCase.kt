package com.irv205.pokeapi.domain.usecase

import com.irv205.pokeapi.core.common.Resource
import com.irv205.pokeapi.domain.model.PokemonPage
import com.irv205.pokeapi.domain.repository.PokemonRepository
import javax.inject.Inject

/**
 * The quiz picks its candidates from the full national dex (Gen 1-9) with a
 * single request instead of firing four detail requests per round.
 */
class GetQuizPoolUseCase @Inject constructor(
    private val repository: PokemonRepository,
) {
    suspend operator fun invoke(): Resource<PokemonPage> =
        repository.getPokemonPage(limit = QUIZ_POOL_SIZE, offset = 0)

    companion object {
        const val QUIZ_POOL_SIZE = 1025
    }
}
