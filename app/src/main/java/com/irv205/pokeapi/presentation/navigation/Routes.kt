package com.irv205.pokeapi.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object PokedexRoute

@Serializable
data object FavoritesRoute

@Serializable
data object QuizRoute

@Serializable
data object MockConfigRoute

@Serializable
data class MockServiceDetailRoute(val serviceId: String)

@Serializable
data class DetailRoute(val pokemonId: Int)

@Serializable
data class EvolutionRoute(
    val pokemonId: Int,
    val chainId: Int,
)
