package com.irv205.pokeapi.domain.usecase

import com.irv205.pokeapi.core.common.Resource
import com.irv205.pokeapi.domain.model.PokemonPage
import com.irv205.pokeapi.domain.repository.PokemonRepository
import javax.inject.Inject

class GetPokemonPageUseCase @Inject constructor(
    private val repository: PokemonRepository,
) {
    suspend operator fun invoke(limit: Int, offset: Int): Resource<PokemonPage> =
        repository.getPokemonPage(limit = limit, offset = offset)
}
