package com.irv205.pokeapi.presentation.mock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.irv205.pokeapi.data.mock.ApiEnvironment
import com.irv205.pokeapi.data.mock.InMemoryMockConfigProvider
import com.irv205.pokeapi.data.mock.MutableEnvironmentProvider
import com.irv205.pokeapi.data.mock.PokeMockFixtures
import com.irv205.pokeapi.data.networklog.NetworkCallRecord
import com.irv205.pokeapi.data.networklog.NetworkCallRecorder
import com.irv205.pokeapi.data.networklog.NetworkCallSource
import com.irv205.pokeapi.data.networklog.NetworkHeader
import com.irv205.pokeapi.ui.theme.PokeApiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MockConfigScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun createViewModel(
        environment: MutableEnvironmentProvider = MutableEnvironmentProvider(ApiEnvironment.PROD),
        recorder: NetworkCallRecorder = NetworkCallRecorder(),
    ) = MockConfigViewModel(
        environmentProvider = environment,
        configProvider = InMemoryMockConfigProvider(services = PokeMockFixtures.services()),
        networkCallRecorder = recorder,
    )

    @Test
    fun mockEnvironmentSwitch_togglesBetweenProdAndMock() {
        val environment = MutableEnvironmentProvider(ApiEnvironment.PROD)
        val viewModel = createViewModel(environment)

        composeRule.setContent {
            PokeApiTheme {
                MockConfigScreen(
                    onBack = {},
                    onApplied = {},
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithText("Mocks de red").assertIsDisplayed()
        composeRule.onNodeWithTag("mock_environment_switch").assertIsOff()

        composeRule.onNodeWithTag("mock_environment_switch").performClick()

        composeRule.onNodeWithTag("mock_environment_switch").assertIsOn()
        composeRule.onNodeWithText("MOCK activo").assertIsDisplayed()
        assertEquals(ApiEnvironment.MOCK, environment.current)
    }

    @Test
    fun applyButton_invokesCallback() {
        var applied = false

        composeRule.setContent {
            PokeApiTheme {
                MockConfigScreen(
                    onBack = {},
                    onApplied = { applied = true },
                    viewModel = createViewModel(),
                )
            }
        }

        composeRule.onNodeWithTag("mock_apply").performClick()

        assertEquals(true, applied)
    }

    @Test
    fun callsTab_showsSimplifiedCardAndExpandsFullDetails() {
        val recorder = NetworkCallRecorder().apply { add(sampleCall()) }
        val viewModel = createViewModel(recorder = recorder)

        composeRule.setContent {
            PokeApiTheme {
                MockConfigScreen(
                    onBack = {},
                    onApplied = {},
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithTag("mock_tab_calls").performClick()

        composeRule.onNodeWithText("/api/v2/pokemon?limit=20").assertIsDisplayed()
        composeRule.onNodeWithTag("call_url").assertDoesNotExist()

        composeRule.onNodeWithTag("network_call_1").performClick()

        composeRule.onNodeWithTag("call_url", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(
            text = "https://pokeapi.co/api/v2/pokemon?limit=20",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag("call_response_body", useUnmergedTree = true).assertExists()
    }

    private fun sampleCall() = NetworkCallRecord(
        id = 1,
        startedAtEpochMs = 1_700_000_000_000,
        durationMs = 12,
        method = "GET",
        url = "https://pokeapi.co/api/v2/pokemon?limit=20",
        path = "/api/v2/pokemon",
        query = "limit=20",
        requestHeaders = listOf(NetworkHeader("Accept", "application/json")),
        requestBody = null,
        statusCode = 200,
        statusMessage = "OK",
        responseHeaders = listOf(NetworkHeader("Content-Type", "application/json")),
        responseBody = """{"count":1}""",
        responseBodyTruncated = false,
        source = NetworkCallSource.MOCK,
        mockService = "Lista de Pokemon",
        mockScenario = "Primera pagina",
        error = null,
    )
}
