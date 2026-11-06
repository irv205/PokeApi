package com.irv205.pokeapi.domain.usecase

import com.irv205.pokeapi.core.common.Resource
import com.irv205.pokeapi.domain.model.PokemonDetailPage
import com.irv205.pokeapi.domain.repository.PokemonRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Combines `/pokemon/{id}` and `/pokemon-species/{id}` in parallel because the
 * detail screen needs data from both endpoints.
 */
class GetPokemonDetailPageUseCase @Inject constructor(
    private val repository: PokemonRepository,
) {
    suspend operator fun invoke(id: Int): Resource<PokemonDetailPage> = coroutineScope {
        val detail = async { repository.getPokemonDetail(id) }
        val species = async { repository.getPokemonSpecies(id) }

        when (val detailResult = detail.await()) {
            is Resource.Error -> detailResult
            Resource.Loading -> Resource.Loading
            is Resource.Success -> when (val speciesResult = species.await()) {
                is Resource.Error -> speciesResult
                Resource.Loading -> Resource.Loading
                is Resource.Success -> Resource.Success(
                    PokemonDetailPage(
                        pokemon = detailResult.data,
                        species = speciesResult.data,
                    ),
                )
            }
        }
    }
}
