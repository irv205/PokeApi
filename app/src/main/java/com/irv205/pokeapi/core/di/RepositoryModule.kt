package com.irv205.pokeapi.core.di

import com.irv205.pokeapi.data.repository.FavoritesRepositoryImpl
import com.irv205.pokeapi.data.repository.PokemonRepositoryImpl
import com.irv205.pokeapi.domain.repository.FavoritesRepository
import com.irv205.pokeapi.domain.repository.PokemonRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPokemonRepository(impl: PokemonRepositoryImpl): PokemonRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository
}
