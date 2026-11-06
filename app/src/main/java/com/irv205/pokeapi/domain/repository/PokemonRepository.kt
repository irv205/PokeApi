package com.irv205.pokeapi.domain.repository

import com.irv205.pokeapi.core.common.Resource
import com.irv205.pokeapi.domain.model.EvolutionChain
import com.irv205.pokeapi.domain.model.PokemonDetail
import com.irv205.pokeapi.domain.model.PokemonPage
import com.irv205.pokeapi.domain.model.PokemonSpecies

interface PokemonRepository {
    suspend fun getPokemonPage(limit: Int, offset: Int): Resource<PokemonPage>

    suspend fun getPokemonDetail(id: Int): Resource<PokemonDetail>

    suspend fun getPokemonSpecies(id: Int): Resource<PokemonSpecies>

    suspend fun getEvolutionChain(id: Int): Resource<EvolutionChain>
}
