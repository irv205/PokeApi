package com.irv205.pokeapi.presentation.mock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.irv205.pokeapi.data.mock.InMemoryMockConfigProvider
import com.irv205.pokeapi.data.mock.PokeMockFixtures
import com.irv205.pokeapi.ui.theme.PokeApiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MockServiceDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var config: InMemoryMockConfigProvider

    private fun showScreen(serviceId: String = "pokemon_page") {
        config = InMemoryMockConfigProvider(services = PokeMockFixtures.services())
        val viewModel = MockServiceDetailViewModel(
            configProvider = config,
            savedStateHandle = SavedStateHandle(mapOf("serviceId" to serviceId)),
        )

        composeRule.setContent {
            PokeApiTheme {
                MockServiceDetailScreen(
                    onBack = {},
                    viewModel = viewModel,
                )
            }
        }
    }

    @Test
    fun serviceDetails_showServiceAndResponses() {
        showScreen()

        composeRule.onAllNodesWithText("Lista de Pokemon").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Respuestas").assertIsDisplayed()
        composeRule.onNodeWithTag("service_detail_list")
            .performScrollToNode(hasTestTag("scenario_active_first_page"))
        composeRule.onNodeWithTag("scenario_active_first_page", useUnmergedTree = true)
            .assertIsSelected()
    }

    @Test
    fun serviceCanBeEditedAndSaved() {
        showScreen()

        composeRule.onNodeWithTag("service_name").performTextReplacement("Lista editada")
        composeRule.onNodeWithTag("service_path").performTextReplacement("/api/v2/pokemon/custom")
        composeRule.onNodeWithTag("save_service").performClick()

        val service = config.findService("pokemon_page")!!
        assertEquals("Lista editada", service.name)
        assertEquals("/api/v2/pokemon/custom", service.pathPattern)
    }

    @Test
    fun responseCanBeAddedWithCustomStatusAndMessage() {
        showScreen()

        composeRule.onNodeWithTag("add_response").performClick()

        composeRule.onAllNodesWithText("Nueva respuesta").onFirst().assertIsDisplayed()
        composeRule.onNodeWithTag("scenario_name").performTextReplacement("Error 400 custom")
        composeRule.onNodeWithTag("scenario_status").performTextReplacement("400")
        composeRule.onNodeWithTag("scenario_query_enabled", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("scenario_request_query")
            .performTextReplacement("limit=0")
        composeRule.onNodeWithTag("scenario_response_body")
            .performTextReplacement("""{"detail":"limit invalido"}""")

        composeRule.onNodeWithTag("scenario_response_body").performScrollTo()
        composeRule.onNodeWithTag("save_scenario").performClick()

        val added = config.findService("pokemon_page")!!
            .scenarios
            .first { it.name == "Error 400 custom" }
        assertEquals(400, added.statusCode)
        assertEquals("limit=0", added.requestQuery)
        assertEquals("""{"detail":"limit invalido"}""", added.responseBody)
    }

    @Test
    fun deletingAResponseAsksForConfirmationAndRemovesIt() {
        showScreen()

        composeRule.onNodeWithTag("service_detail_list")
            .performScrollToNode(hasTestTag("delete_scenario_server_error"))
        composeRule.onNodeWithTag("delete_scenario_server_error").performClick()

        composeRule.onNodeWithText("Eliminar respuesta").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_delete_scenario").performClick()

        assertTrue(
            config.findService("pokemon_page")!!
                .scenarios
                .none { it.id == "server_error" },
        )
    }

    @Test
    fun selectingAResponseUpdatesTheActiveScenario() {
        showScreen()

        composeRule.onNodeWithTag("service_detail_list")
            .performScrollToNode(hasTestTag("scenario_active_server_error"))
        composeRule.onNodeWithTag("scenario_active_server_error", useUnmergedTree = true)
            .performClick()

        assertEquals("server_error", config.findService("pokemon_page")?.selectedScenarioId)
    }

    @Test
    fun deletingTheServiceInvokesBackAfterConfirmation() {
        var backCalled = false
        config = InMemoryMockConfigProvider(services = PokeMockFixtures.services())
        val viewModel = MockServiceDetailViewModel(
            configProvider = config,
            savedStateHandle = SavedStateHandle(mapOf("serviceId" to "pokemon_page")),
        )

        composeRule.setContent {
            PokeApiTheme {
                MockServiceDetailScreen(
                    onBack = { backCalled = true },
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithTag("service_detail_list")
            .performScrollToNode(hasTestTag("delete_service"))
        composeRule.onNodeWithTag("delete_service").performClick()
        composeRule.onNodeWithTag("confirm_delete_service").performClick()

        composeRule.waitForIdle()
        assertTrue(backCalled)
        assertEquals(null, config.findService("pokemon_page"))
    }
}
