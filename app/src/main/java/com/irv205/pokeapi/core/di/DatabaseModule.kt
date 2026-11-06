package com.irv205.pokeapi.core.di

import android.content.Context
import androidx.room.Room
import com.irv205.pokeapi.data.local.FavoritePokemonDao
import com.irv205.pokeapi.data.local.PokeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PokeDatabase =
        Room.databaseBuilder(context, PokeDatabase::class.java, "pokecompose.db").build()

    @Provides
    fun provideFavoritePokemonDao(database: PokeDatabase): FavoritePokemonDao =
        database.favoritePokemonDao()
}
