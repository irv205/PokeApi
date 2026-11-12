package com.irv205.pokeapi.core.di

import com.irv205.pokeapi.BuildConfig
import com.irv205.pokeapi.data.mock.ApiEnvironment
import com.irv205.pokeapi.data.mock.EnvironmentProvider
import com.irv205.pokeapi.data.mock.InMemoryMockConfigProvider
import com.irv205.pokeapi.data.mock.MockConfigProvider
import com.irv205.pokeapi.data.mock.MockInterceptor
import com.irv205.pokeapi.data.mock.MutableEnvironmentProvider
import com.irv205.pokeapi.data.mock.PokeMockFixtures
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MockModule {

    @Provides
    @Singleton
    fun provideMutableEnvironmentProvider(): MutableEnvironmentProvider =
        MutableEnvironmentProvider(defaultEnvironment())

    @Provides
    @Singleton
    fun provideEnvironmentProvider(
        impl: MutableEnvironmentProvider,
    ): EnvironmentProvider = impl

    @Provides
    @Singleton
    fun provideInMemoryMockConfigProvider(): InMemoryMockConfigProvider =
        InMemoryMockConfigProvider(
            services = PokeMockFixtures.services(),
            fallbackToNetwork = false,
        )

    @Provides
    @Singleton
    fun provideMockConfigProvider(
        impl: InMemoryMockConfigProvider,
    ): MockConfigProvider = impl

    @Provides
    @Singleton
    fun provideMockInterceptor(
        environment: EnvironmentProvider,
        config: MockConfigProvider,
    ): MockInterceptor = MockInterceptor(environment, config)

    private fun defaultEnvironment(): ApiEnvironment =
        ApiEnvironment.entries.firstOrNull {
            it.name.equals(BuildConfig.DEFAULT_API_ENVIRONMENT, ignoreCase = true)
        } ?: ApiEnvironment.PROD
}
