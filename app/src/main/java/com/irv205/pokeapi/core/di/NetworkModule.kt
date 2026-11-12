package com.irv205.pokeapi.core.di

import com.irv205.pokeapi.BuildConfig
import com.irv205.pokeapi.data.mock.MockInterceptor
import com.irv205.pokeapi.data.networklog.NetworkCallRecorder
import com.irv205.pokeapi.data.networklog.NetworkLoggingInterceptor
import com.irv205.pokeapi.data.remote.PokeApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://pokeapi.co/api/v2/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideNetworkCallRecorder(): NetworkCallRecorder = NetworkCallRecorder()

    @Provides
    @Singleton
    fun provideNetworkLoggingInterceptor(
        recorder: NetworkCallRecorder,
    ): NetworkLoggingInterceptor = NetworkLoggingInterceptor(recorder)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        mockInterceptor: MockInterceptor,
        networkLoggingInterceptor: NetworkLoggingInterceptor,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            // El registro va primero para capturar la respuesta final, ya sea real o mockeada.
            .apply {
                if (BuildConfig.DEBUG) addInterceptor(networkLoggingInterceptor)
            }
            .addInterceptor(logging)
            .addInterceptor(mockInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun providePokeApiService(retrofit: Retrofit): PokeApiService =
        retrofit.create(PokeApiService::class.java)
}
