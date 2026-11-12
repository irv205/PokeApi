package com.irv205.pokeapi.data.mock

import com.irv205.pokeapi.data.remote.dto.EvolutionChainDto
import com.irv205.pokeapi.data.remote.dto.PokemonDetailDto
import com.irv205.pokeapi.data.remote.dto.PokemonPageDto
import com.irv205.pokeapi.data.remote.dto.PokemonSpeciesDto
import java.io.IOException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var environment: MutableEnvironmentProvider
    private lateinit var config: InMemoryMockConfigProvider

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        environment = MutableEnvironmentProvider(ApiEnvironment.MOCK)
        config = InMemoryMockConfigProvider(
            services = PokeMockFixtures.services(),
            fallbackToNetwork = false,
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(MockInterceptor(environment, config))
        .build()

    private fun execute(path: String) = client()
        .newCall(Request.Builder().url(server.url(path)).build())
        .execute()

    @Test
    fun `serves the first page without hitting the network`() {
        val response = execute("/api/v2/pokemon?limit=20&offset=0")

        assertEquals(200, response.code)
        assertEquals("true", response.header("x-mocked"))
        assertEquals("Lista de Pokemon", response.header("x-mock-service"))
        assertEquals("Primera pagina (200)", response.header("x-mock-scenario"))
        assertEquals(0, server.requestCount)

        val page = json.decodeFromString<PokemonPageDto>(response.body!!.string())
        assertEquals(24, page.count)
        assertEquals(20, page.results.size)
        assertTrue(page.next != null)
        assertEquals("bulbasaur", page.results.first().name)
    }

    @Test
    fun `serves the second page with the offset constraint`() {
        val response = execute("/api/v2/pokemon?limit=20&offset=20")

        assertEquals(200, response.code)
        assertEquals("Segunda pagina (200)", response.header("x-mock-scenario"))

        val page = json.decodeFromString<PokemonPageDto>(response.body!!.string())
        assertEquals(4, page.results.size)
        assertNull(page.next)
        assertEquals("spearow", page.results.first().name)
    }

    @Test
    fun `serves the full quiz pool for limit 1025`() {
        val response = execute("/api/v2/pokemon?limit=1025&offset=0")

        assertEquals(200, response.code)
        assertEquals("Pool del quiz (200)", response.header("x-mock-scenario"))

        val page = json.decodeFromString<PokemonPageDto>(response.body!!.string())
        assertEquals(24, page.results.size)
        assertNull(page.next)
    }

    @Test
    fun `serves a pokemon detail`() {
        val response = execute("/api/v2/pokemon/25")

        assertEquals(200, response.code)
        val detail = json.decodeFromString<PokemonDetailDto>(response.body!!.string())
        assertEquals(25, detail.id)
        assertEquals("pikachu", detail.name)
        assertEquals("electric", detail.types.first().type.name)
        assertEquals(6, detail.stats.size)
    }

    @Test
    fun `serves species and evolution chain`() {
        val speciesResponse = execute("/api/v2/pokemon-species/25")
        val species = json.decodeFromString<PokemonSpeciesDto>(speciesResponse.body!!.string())
        assertEquals(25, species.id)
        assertEquals(25, species.evolutionChain?.url?.trimEnd('/')?.substringAfterLast('/')?.toInt())

        val evolutionResponse = execute("/api/v2/evolution-chain/25")
        val evolution = json.decodeFromString<EvolutionChainDto>(evolutionResponse.body!!.string())
        assertEquals(25, evolution.id)
        assertEquals("pikachu", evolution.chain.species.name)
    }

    @Test
    fun `returns a configured error scenario`() {
        config.services = config.services.map { service ->
            if (service.id == "pokemon_page") {
                service.copy(selectedScenarioId = "server_error")
            } else {
                service
            }
        }

        val response = execute("/api/v2/pokemon?limit=20&offset=0")

        assertEquals(500, response.code)
        assertEquals("Error de servidor (500)", response.header("x-mock-scenario"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `passes through in PROD`() {
        environment.current = ApiEnvironment.PROD
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"real":true}"""),
        )

        val response = execute("/api/v2/pokemon?limit=20&offset=0")

        assertEquals(200, response.code)
        assertEquals("""{"real":true}""", response.body!!.string())
        assertNull(response.header("x-mocked"))
        assertEquals(1, server.requestCount)
    }

    @Test(expected = IOException::class)
    fun `strict mode fails when no mock matches`() {
        execute("/api/v2/berry")
    }

    @Test
    fun `falls back to network when configured`() {
        config.fallbackToNetwork = true
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"fallback":true}"""),
        )

        val response = execute("/api/v2/berry")

        assertEquals(200, response.code)
        assertEquals("""{"fallback":true}""", response.body!!.string())
        assertNull(response.header("x-mocked"))
        assertEquals(1, server.requestCount)
    }
}
