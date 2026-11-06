package com.irv205.pokeapi.data.repository

import com.irv205.pokeapi.core.di.IoDispatcher
import com.irv205.pokeapi.data.local.FavoritePokemonDao
import com.irv205.pokeapi.data.local.FavoritePokemonEntity
import com.irv205.pokeapi.domain.model.FavoritePokemon
import com.irv205.pokeapi.domain.repository.FavoritesRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FavoritesRepositoryImpl @Inject constructor(
    private val dao: FavoritePokemonDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FavoritesRepository {

    override fun observeFavorites(): Flow<List<FavoritePokemon>> =
        dao.observeAll()
            .map { entities -> entities.map(FavoritePokemonEntity::toDomain) }
            .flowOn(ioDispatcher)

    override fun observeFavoriteIds(): Flow<Set<Int>> =
        dao.observeIds()
            .map { it.toSet() }
            .flowOn(ioDispatcher)

    override suspend fun add(favorite: FavoritePokemon) = withContext(ioDispatcher) {
        dao.insert(favorite.toEntity())
    }

    override suspend fun remove(id: Int) = withContext(ioDispatcher) {
        dao.deleteById(id)
    }

    override suspend fun toggle(favorite: FavoritePokemon) = withContext(ioDispatcher) {
        if (dao.exists(favorite.id)) {
            dao.deleteById(favorite.id)
        } else {
            dao.insert(favorite.toEntity())
        }
    }
}

private fun FavoritePokemonEntity.toDomain(): FavoritePokemon = FavoritePokemon(
    id = id,
    name = name,
    imageUrl = imageUrl,
    primaryType = primaryType,
)

private fun FavoritePokemon.toEntity(): FavoritePokemonEntity = FavoritePokemonEntity(
    id = id,
    name = name,
    imageUrl = imageUrl,
    primaryType = primaryType,
)
