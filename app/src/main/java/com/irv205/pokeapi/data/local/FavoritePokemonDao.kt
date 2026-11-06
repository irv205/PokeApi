package com.irv205.pokeapi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritePokemonDao {
    @Query("SELECT * FROM favorite_pokemon ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoritePokemonEntity>>

    @Query("SELECT id FROM favorite_pokemon")
    fun observeIds(): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_pokemon WHERE id = :id)")
    suspend fun exists(id: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pokemon: FavoritePokemonEntity)

    @Query("DELETE FROM favorite_pokemon WHERE id = :id")
    suspend fun deleteById(id: Int)
}
