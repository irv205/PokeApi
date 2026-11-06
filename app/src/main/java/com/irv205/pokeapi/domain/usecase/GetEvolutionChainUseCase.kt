package com.irv205.pokeapi.domain.usecase

import com.irv205.pokeapi.core.common.Resource
import com.irv205.pokeapi.domain.model.EvolutionChain
import com.irv205.pokeapi.domain.repository.PokemonRepository
import javax.inject.Inject

class GetEvolutionChainUseCase @Inject constructor(
    private val repository: PokemonRepository,
) {
    suspend operator fun invoke(id: Int): Resource<EvolutionChain> =
        repository.getEvolutionChain(id)
}
