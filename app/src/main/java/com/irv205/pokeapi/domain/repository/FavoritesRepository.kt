package com.irv205.pokeapi.domain.repository

import com.irv205.pokeapi.domain.model.FavoritePokemon
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeFavorites(): Flow<List<FavoritePokemon>>

    fun observeFavoriteIds(): Flow<Set<Int>>

    suspend fun add(favorite: FavoritePokemon)

    suspend fun remove(id: Int)

    suspend fun toggle(favorite: FavoritePokemon)
}
