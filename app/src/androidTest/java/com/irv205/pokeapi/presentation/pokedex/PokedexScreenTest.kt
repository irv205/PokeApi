package com.irv205.pokeapi.presentation.pokedex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.irv205.pokeapi.core.common.PokemonImageUrls
import com.irv205.pokeapi.domain.model.PokemonSummary
import com.irv205.pokeapi.ui.theme.PokeApiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PokedexScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val pikachu = PokemonSummary(
        id = 25,
        name = "pikachu",
        imageUrl = PokemonImageUrls.officialArtwork(25),
    )

    @Test
    fun pokemonCard_showsNameAndNavigatesOnClick() {
        var clickedPokemonId: Int? = null

        composeRule.setContent {
            PokeApiTheme {
                PokedexContent(
                    state = PokedexUiState(
                        pokemon = listOf(pikachu),
                        endReached = true,
                    ),
                    onQueryChange = {},
                    onClearQuery = {},
                    onRetry = {},
                    onLoadMore = {},
                    onPokemonClick = { clickedPokemonId = it },
                    onFavoriteClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Pikachu").assertIsDisplayed()
        composeRule.onNodeWithText("#025").assertIsDisplayed()

        composeRule.onNodeWithText("Pikachu").performClick()

        assertEquals(25, clickedPokemonId)
    }

    @Test
    fun searchField_updatesQueryAndClearActionResetsIt() {
        composeRule.setContent {
            var query by remember { mutableStateOf("") }

            PokeApiTheme {
                PokedexContent(
                    state = PokedexUiState(
                        pokemon = listOf(pikachu),
                        query = query,
                        endReached = true,
                    ),
                    onQueryChange = { query = it },
                    onClearQuery = { query = "" },
                    onRetry = {},
                    onLoadMore = {},
                    onPokemonClick = {},
                    onFavoriteClick = {},
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("pika")

        composeRule.onNodeWithText("pika").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Limpiar búsqueda").performClick()

        composeRule.onNodeWithText("pika").assertDoesNotExist()
    }
}
