package com.irv205.pokeapi.data.repository

import com.irv205.pokeapi.core.common.Resource
import com.irv205.pokeapi.core.di.IoDispatcher
import com.irv205.pokeapi.core.network.safeApiCall
import com.irv205.pokeapi.data.mapper.toDomain
import com.irv205.pokeapi.data.remote.PokeApiService
import com.irv205.pokeapi.domain.model.EvolutionChain
import com.irv205.pokeapi.domain.model.PokemonDetail
import com.irv205.pokeapi.domain.model.PokemonPage
import com.irv205.pokeapi.domain.model.PokemonSpecies
import com.irv205.pokeapi.domain.repository.PokemonRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

class PokemonRepositoryImpl @Inject constructor(
    private val api: PokeApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PokemonRepository {

    override suspend fun getPokemonPage(limit: Int, offset: Int): Resource<PokemonPage> =
        safeApiCall(ioDispatcher) {
            api.getPokemonPage(limit = limit, offset = offset).toDomain()
        }

    override suspend fun getPokemonDetail(id: Int): Resource<PokemonDetail> =
        safeApiCall(ioDispatcher) {
            api.getPokemonDetail(id).toDomain()
        }

    override suspend fun getPokemonSpecies(id: Int): Resource<PokemonSpecies> =
        safeApiCall(ioDispatcher) {
            api.getPokemonSpecies(id).toDomain()
        }

    override suspend fun getEvolutionChain(id: Int): Resource<EvolutionChain> =
        safeApiCall(ioDispatcher) {
            api.getEvolutionChain(id).toDomain()
        }
}
