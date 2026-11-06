package com.irv205.pokeapi.presentation.pokedex

import app.cash.turbine.test
import com.irv205.pokeapi.MainDispatcherRule
import com.irv205.pokeapi.core.common.PokemonImageUrls
import com.irv205.pokeapi.core.common.Resource
import com.irv205.pokeapi.domain.model.PokemonPage
import com.irv205.pokeapi.domain.model.PokemonSummary
import com.irv205.pokeapi.domain.usecase.GetPokemonPageUseCase
import com.irv205.pokeapi.domain.usecase.ObserveFavoriteIdsUseCase
import com.irv205.pokeapi.domain.usecase.ToggleFavoriteUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PokedexViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getPokemonPage = mockk<GetPokemonPageUseCase>()
    private val observeFavoriteIds = mockk<ObserveFavoriteIdsUseCase>()
    private val toggleFavorite = mockk<ToggleFavoriteUseCase>(relaxed = true)

    private fun createViewModel(): PokedexViewModel {
        every { observeFavoriteIds() } returns flowOf(emptySet())
        return PokedexViewModel(
            getPokemonPage = getPokemonPage,
            observeFavoriteIds = observeFavoriteIds,
            toggleFavorite = toggleFavorite,
        )
    }

    @Test
    fun `first page success exposes pokemon and clears loading`() = runTest {
        coEvery { getPokemonPage(20, 0) } returns Resource.Success(
            PokemonPage(
                items = listOf(
                    summary(1, "bulbasaur"),
                    summary(4, "charmander"),
                    summary(7, "squirtle"),
                ),
                hasMore = false,
            ),
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitLoadedState()

            assertEquals(3, state.pokemon.size)
            assertEquals("bulbasaur", state.pokemon.first().name)
            assertNull(state.error)
            assertFalse(state.isLoading)
            assertTrue(state.endReached)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search filters loaded pokemon by name and national dex id`() = runTest {
        coEvery { getPokemonPage(20, 0) } returns Resource.Success(
            PokemonPage(
                items = listOf(
                    summary(1, "bulbasaur"),
                    summary(4, "charmander"),
                    summary(25, "pikachu"),
                ),
                hasMore = false,
            ),
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitLoadedState()

            viewModel.onQueryChange("char")
            var state = awaitItem()
            while (state.query != "char") state = awaitItem()

            assertEquals(listOf("charmander"), state.visiblePokemon.map { it.name })

            viewModel.onQueryChange("25")
            state = awaitItem()
            while (state.query != "25") state = awaitItem()

            assertEquals(listOf("pikachu"), state.visiblePokemon.map { it.name })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `first page error exposes message and retry loads pokemon again`() = runTest {
        coEvery { getPokemonPage(20, 0) } returns Resource.Error("Sin conexión.") andThen
            Resource.Success(
                PokemonPage(
                    items = listOf(summary(25, "pikachu")),
                    hasMore = false,
                ),
            )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.error == null) state = awaitItem()

            assertNotNull(state.error)
            assertTrue(state.pokemon.isEmpty())
            assertFalse(state.isLoading)

            viewModel.retry()
            state = awaitLoadedState()

            assertEquals(listOf("pikachu"), state.pokemon.map { it.name })
            assertNull(state.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<PokedexUiState>.awaitLoadedState(): PokedexUiState {
        var state = awaitItem()
        while (state.pokemon.isEmpty() && state.error == null) {
            state = awaitItem()
        }
        return state
    }

    private fun summary(id: Int, name: String) = PokemonSummary(
        id = id,
        name = name,
        imageUrl = PokemonImageUrls.officialArtwork(id),
    )
}
