package com.irv205.pokeapi.data.remote

import com.irv205.pokeapi.data.remote.dto.EvolutionChainDto
import com.irv205.pokeapi.data.remote.dto.PokemonDetailDto
import com.irv205.pokeapi.data.remote.dto.PokemonPageDto
import com.irv205.pokeapi.data.remote.dto.PokemonSpeciesDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PokeApiService {
    @GET("pokemon")
    suspend fun getPokemonPage(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): PokemonPageDto

    @GET("pokemon/{id}")
    suspend fun getPokemonDetail(@Path("id") id: Int): PokemonDetailDto

    @GET("pokemon-species/{id}")
    suspend fun getPokemonSpecies(@Path("id") id: Int): PokemonSpeciesDto

    @GET("evolution-chain/{id}")
    suspend fun getEvolutionChain(@Path("id") id: Int): EvolutionChainDto
}
