package com.irv205.pokeapi.data.mock

import com.irv205.pokeapi.data.mock.MockRequestMatcher.matchesMethodAndPath
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockRequestMatcherTest {

    private fun request(
        method: String = "GET",
        url: String = "https://pokeapi.co/api/v2/pokemon",
        body: String? = null,
    ): Request = Request.Builder()
        .url(url)
        .method(method, body?.toRequestBody("application/json".toMediaType()))
        .build()

    private fun service(
        method: String = "GET",
        pathPattern: String = "/api/v2/pokemon",
    ) = MockService(
        id = "service",
        name = "service",
        method = method,
        pathPattern = pathPattern,
    )

    @Test
    fun `matches exact path`() {
        assertTrue(service().matchesMethodAndPath(request()))
    }

    @Test
    fun `does not match a longer path for an exact pattern`() {
        assertFalse(service().matchesMethodAndPath(request(url = "https://pokeapi.co/api/v2/pokemon/25")))
    }

    @Test
    fun `matches path template`() {
        val service = service(pathPattern = "/api/v2/pokemon/{id}")
        assertTrue(service.matchesMethodAndPath(request(url = "https://pokeapi.co/api/v2/pokemon/25")))
    }

    @Test
    fun `matches regex path`() {
        val service = service(pathPattern = "/api/v2/pokemon/\\d+")
        assertTrue(service.matchesMethodAndPath(request(url = "https://pokeapi.co/api/v2/pokemon/25")))
        assertFalse(service.matchesMethodAndPath(request(url = "https://pokeapi.co/api/v2/pokemon/pikachu")))
    }

    @Test
    fun `does not match a different method`() {
        val service = service(method = "POST")
        assertFalse(service.matchesMethodAndPath(request(method = "GET")))
    }

    @Test
    fun `matches query constraints`() {
        val scenario = MockScenario(
            id = "quiz",
            name = "quiz",
            requestQuery = "limit=1025",
        )
        assertTrue(
            scenario.matchesRequest(
                request(url = "https://pokeapi.co/api/v2/pokemon?limit=1025&offset=0"),
            ),
        )
        assertFalse(
            scenario.matchesRequest(
                request(url = "https://pokeapi.co/api/v2/pokemon?limit=20&offset=0"),
            ),
        )
    }

    @Test
    fun `matches body content`() {
        val scenario = MockScenario(
            id = "created",
            name = "created",
            requestBody = "\"productId\":1",
        )
        assertTrue(
            scenario.matchesRequest(
                request(
                    method = "POST",
                    url = "https://pokeapi.co/api/v2/orders",
                    body = """{"productId":1,"quantity":2}""",
                ),
            ),
        )
        assertFalse(
            scenario.matchesRequest(
                request(
                    method = "POST",
                    url = "https://pokeapi.co/api/v2/orders",
                    body = """{"productId":2}""",
                ),
            ),
        )
    }

    @Test
    fun `scenario without constraints does not participate in specific matching`() {
        val scenario = MockScenario(id = "happy", name = "happy")
        assertFalse(scenario.matchesRequest(request()))
    }

    @Test
    fun `resolves the selected scenario when no constraint matches`() {
        val service = MockService(
            id = "page",
            name = "page",
            pathPattern = "/api/v2/pokemon",
            selectedScenarioId = "first",
            scenarios = listOf(
                MockScenario(id = "other", name = "other", requestQuery = "limit=1025"),
                MockScenario(id = "first", name = "first"),
            ),
        )

        val match = MockRequestMatcher.resolve(listOf(service), request())

        assertNotNull(match)
        assertTrue(match?.scenario?.id == "first")
    }

    @Test
    fun `constrained scenario wins over the selected one`() {
        val service = MockService(
            id = "page",
            name = "page",
            pathPattern = "/api/v2/pokemon",
            selectedScenarioId = "first",
            scenarios = listOf(
                MockScenario(id = "quiz", name = "quiz", requestQuery = "limit=1025"),
                MockScenario(id = "first", name = "first"),
            ),
        )

        val match = MockRequestMatcher.resolve(
            listOf(service),
            request(url = "https://pokeapi.co/api/v2/pokemon?limit=1025"),
        )

        assertNotNull(match)
        assertTrue(match?.scenario?.id == "quiz")
    }

    @Test
    fun `returns null when no service matches`() {
        assertNull(
            MockRequestMatcher.resolve(
                listOf(service()),
                request(url = "https://pokeapi.co/api/v2/berry"),
            ),
        )
    }

    @Test
    fun `disabled services are ignored`() {
        val disabled = service().copy(enabled = false)
        assertNull(MockRequestMatcher.resolve(listOf(disabled), request()))
    }
}
