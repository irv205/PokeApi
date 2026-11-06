package com.irv205.pokeapi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FavoritePokemonEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PokeDatabase : RoomDatabase() {
    abstract fun favoritePokemonDao(): FavoritePokemonDao
}
