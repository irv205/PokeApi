package com.irv205.pokeapi.data.mock

import com.irv205.pokeapi.data.remote.dto.AbilitySlotDto
import com.irv205.pokeapi.data.remote.dto.ChainLinkDto
import com.irv205.pokeapi.data.remote.dto.EvolutionChainDto
import com.irv205.pokeapi.data.remote.dto.EvolutionChainRefDto
import com.irv205.pokeapi.data.remote.dto.EvolutionDetailDto
import com.irv205.pokeapi.data.remote.dto.FlavorTextEntryDto
import com.irv205.pokeapi.data.remote.dto.GenusDto
import com.irv205.pokeapi.data.remote.dto.HomeSpritesDto
import com.irv205.pokeapi.data.remote.dto.NamedResourceDto
import com.irv205.pokeapi.data.remote.dto.OfficialArtworkDto
import com.irv205.pokeapi.data.remote.dto.OtherSpritesDto
import com.irv205.pokeapi.data.remote.dto.PokemonDetailDto
import com.irv205.pokeapi.data.remote.dto.PokemonPageDto
import com.irv205.pokeapi.data.remote.dto.PokemonSpeciesDto
import com.irv205.pokeapi.data.remote.dto.SpritesDto
import com.irv205.pokeapi.data.remote.dto.StatSlotDto
import com.irv205.pokeapi.data.remote.dto.TypeSlotDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Fixtures locales de PokeAPI para el motor de mocks.
 *
 * Cubren las cuatro llamadas de la app: listado paginado, detalle, especie y
 * cadena de evolución. La primera página devuelve 20 Pokémon (el `PAGE_SIZE`
 * de la Pokédex), la segunda los 4 restantes y el pool del quiz los 24.
 */
object PokeMockFixtures {

    private const val JSON = "application/json"
    private const val API = "https://pokeapi.co/api/v2"
    private const val SPRITES =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon"

    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
    }

    private data class FixturePokemon(
        val id: Int,
        val name: String,
        val types: List<String>,
        val chainRootId: Int,
    )

    private val pokemon = listOf(
        FixturePokemon(1, "bulbasaur", listOf("grass", "poison"), 1),
        FixturePokemon(2, "ivysaur", listOf("grass", "poison"), 1),
        FixturePokemon(3, "venusaur", listOf("grass", "poison"), 1),
        FixturePokemon(4, "charmander", listOf("fire"), 4),
        FixturePokemon(5, "charmeleon", listOf("fire"), 4),
        FixturePokemon(6, "charizard", listOf("fire", "flying"), 4),
        FixturePokemon(7, "squirtle", listOf("water"), 7),
        FixturePokemon(8, "wartortle", listOf("water"), 7),
        FixturePokemon(9, "blastoise", listOf("water"), 7),
        FixturePokemon(10, "caterpie", listOf("bug"), 10),
        FixturePokemon(11, "metapod", listOf("bug"), 10),
        FixturePokemon(12, "butterfree", listOf("bug", "flying"), 10),
        FixturePokemon(13, "weedle", listOf("bug", "poison"), 13),
        FixturePokemon(14, "kakuna", listOf("bug", "poison"), 13),
        FixturePokemon(15, "beedrill", listOf("bug", "poison"), 13),
        FixturePokemon(16, "pidgey", listOf("normal", "flying"), 16),
        FixturePokemon(17, "pidgeotto", listOf("normal", "flying"), 16),
        FixturePokemon(18, "pidgeot", listOf("normal", "flying"), 16),
        FixturePokemon(19, "rattata", listOf("normal"), 19),
        FixturePokemon(20, "raticate", listOf("normal"), 19),
        FixturePokemon(21, "spearow", listOf("normal", "flying"), 21),
        FixturePokemon(25, "pikachu", listOf("electric"), 25),
        FixturePokemon(133, "eevee", listOf("normal"), 133),
        FixturePokemon(143, "snorlax", listOf("normal"), 143),
    )

    private val families: Map<Int, List<List<Int>>> = mapOf(
        1 to listOf(listOf(1), listOf(2), listOf(3)),
        4 to listOf(listOf(4), listOf(5), listOf(6)),
        7 to listOf(listOf(7), listOf(8), listOf(9)),
        10 to listOf(listOf(10), listOf(11), listOf(12)),
        13 to listOf(listOf(13), listOf(14), listOf(15)),
        16 to listOf(listOf(16), listOf(17), listOf(18)),
        19 to listOf(listOf(19), listOf(20)),
        21 to listOf(listOf(21)),
        25 to listOf(listOf(25)),
        133 to listOf(listOf(133)),
        143 to listOf(listOf(143)),
    )

    fun services(): List<MockService> = buildList {
        add(pokemonPageService())
        pokemon.forEach { entry ->
            add(pokemonDetailService(entry))
            add(pokemonSpeciesService(entry))
        }
        families.forEach { (chainId, stages) ->
            add(evolutionChainService(chainId, stages))
        }
    }

    private fun pokemonPageService() = MockService(
        id = "pokemon_page",
        name = "Lista de Pokemon",
        method = "GET",
        pathPattern = "/api/v2/pokemon",
        selectedScenarioId = "first_page",
        scenarios = listOf(
            MockScenario(
                id = "quiz_pool",
                name = "Pool del quiz (200)",
                contentType = JSON,
                requestQuery = "limit=1025",
                responseBody = pageJson(startIndex = 0, count = pokemon.size, next = null),
            ),
            MockScenario(
                id = "second_page",
                name = "Segunda pagina (200)",
                contentType = JSON,
                requestQuery = "limit=20&offset=20",
                responseBody = pageJson(
                    startIndex = 20,
                    count = pokemon.size - 20,
                    next = null,
                ),
            ),
            MockScenario(
                id = "first_page",
                name = "Primera pagina (200)",
                contentType = JSON,
                responseBody = pageJson(
                    startIndex = 0,
                    count = 20,
                    next = "$API/pokemon?offset=20&limit=20",
                ),
            ),
            MockScenario(
                id = "empty",
                name = "Lista vacia (200)",
                contentType = JSON,
                requestQuery = "offset=9999",
                responseBody = pageJson(startIndex = 0, count = 0, next = null),
            ),
            MockScenario(
                id = "server_error",
                name = "Error de servidor (500)",
                statusCode = 500,
                contentType = JSON,
                responseBody = """{"detail":"Internal Server Error"}""",
            ),
        ),
    )

    private fun pokemonDetailService(entry: FixturePokemon) = MockService(
        id = "pokemon_detail_${entry.id}",
        name = "Detalle de ${entry.name}",
        method = "GET",
        pathPattern = "/api/v2/pokemon/${entry.id}",
        selectedScenarioId = "happy",
        scenarios = listOf(
            MockScenario(
                id = "happy",
                name = "${entry.name} (200)",
                contentType = JSON,
                responseBody = detailJson(entry),
            ),
            MockScenario(
                id = "not_found",
                name = "${entry.name} no encontrado (404)",
                statusCode = 404,
                contentType = JSON,
                responseBody = """{"detail":"Not found"}""",
            ),
        ),
    )

    private fun pokemonSpeciesService(entry: FixturePokemon) = MockService(
        id = "pokemon_species_${entry.id}",
        name = "Especie de ${entry.name}",
        method = "GET",
        pathPattern = "/api/v2/pokemon-species/${entry.id}",
        selectedScenarioId = "happy",
        scenarios = listOf(
            MockScenario(
                id = "happy",
                name = "${entry.name} especie (200)",
                contentType = JSON,
                responseBody = speciesJson(entry),
            ),
        ),
    )

    private fun evolutionChainService(
        chainId: Int,
        stages: List<List<Int>>,
    ) = MockService(
        id = "evolution_chain_$chainId",
        name = "Evolucion $chainId",
        method = "GET",
        pathPattern = "/api/v2/evolution-chain/$chainId",
        selectedScenarioId = "happy",
        scenarios = listOf(
            MockScenario(
                id = "happy",
                name = "Cadena $chainId (200)",
                contentType = JSON,
                responseBody = evolutionJson(chainId, stages),
            ),
        ),
    )

    private fun pageJson(
        startIndex: Int,
        count: Int,
        next: String?,
    ): String {
        val page = pokemon.drop(startIndex).take(count)
        return json.encodeToString(
            PokemonPageDto(
                count = pokemon.size,
                next = next,
                previous = if (startIndex == 0) {
                    null
                } else {
                    "$API/pokemon?offset=0&limit=20"
                },
                results = page.map {
                    NamedResourceDto(
                        name = it.name,
                        url = "$API/pokemon/${it.id}/",
                    )
                },
            ),
        )
    }

    private fun detailJson(entry: FixturePokemon): String {
        val statNames = listOf(
            "hp",
            "attack",
            "defense",
            "special-attack",
            "special-defense",
            "speed",
        )
        val baseStat = 45 + (entry.id * 7) % 70
        return json.encodeToString(
            PokemonDetailDto(
                id = entry.id,
                name = entry.name,
                height = 4 + entry.id % 16,
                weight = 30 + (entry.id * 3) % 200,
                types = entry.types.mapIndexed { index, type ->
                    TypeSlotDto(
                        slot = index + 1,
                        type = NamedResourceDto(type, "$API/type/$type/"),
                    )
                },
                stats = statNames.mapIndexed { index, name ->
                    StatSlotDto(
                        baseStat = baseStat + index * 4,
                        effort = if (index == 0) 1 else 0,
                        stat = NamedResourceDto(name, "$API/stat/$name/"),
                    )
                },
                abilities = listOf(
                    AbilitySlotDto(
                        isHidden = false,
                        slot = 1,
                        ability = NamedResourceDto("overgrow", "$API/ability/overgrow/"),
                    ),
                    AbilitySlotDto(
                        isHidden = true,
                        slot = 3,
                        ability = NamedResourceDto("chlorophyll", "$API/ability/chlorophyll/"),
                    ),
                ),
                sprites = SpritesDto(
                    frontDefault = "$SPRITES/${entry.id}.png",
                    other = OtherSpritesDto(
                        officialArtwork = OfficialArtworkDto(
                            "$SPRITES/other/official-artwork/${entry.id}.png",
                        ),
                        home = HomeSpritesDto("$SPRITES/other/home/${entry.id}.png"),
                    ),
                ),
            ),
        )
    }

    private fun speciesJson(entry: FixturePokemon): String {
        val displayName = entry.name.replaceFirstChar { it.uppercase() }
        val primaryType = entry.types.first()
        val flavor = "$displayName es un Pokemon de tipo $primaryType usado como fixture " +
            "local de PokeCompose. Permite probar la app completa sin depender de la red."
        return json.encodeToString(
            PokemonSpeciesDto(
                id = entry.id,
                name = entry.name,
                habitat = NamedResourceDto("grassland", "$API/pokemon-habitat/grassland/"),
                color = NamedResourceDto(
                    colorFor(primaryType),
                    "$API/pokemon-color/${colorFor(primaryType)}/",
                ),
                genera = listOf(
                    GenusDto("Pokemon de prueba", NamedResourceDto("es", "$API/language/es/")),
                    GenusDto("Test Pokemon", NamedResourceDto("en", "$API/language/en/")),
                ),
                flavorTextEntries = listOf(
                    FlavorTextEntryDto(flavor, NamedResourceDto("es", "$API/language/es/")),
                    FlavorTextEntryDto(
                        "$displayName is a mock Pokemon used by PokeCompose fixtures.",
                        NamedResourceDto("en", "$API/language/en/"),
                    ),
                ),
                evolutionChain = EvolutionChainRefDto(
                    "$API/evolution-chain/${entry.chainRootId}/",
                ),
                isLegendary = false,
                isMythical = false,
            ),
        )
    }

    private fun evolutionJson(
        chainId: Int,
        stages: List<List<Int>>,
    ): String {
        val byId = pokemon.associateBy { it.id }

        fun nodeAt(id: Int, stageIndex: Int): ChainLinkDto {
            val entry = byId.getValue(id)
            val details = if (stageIndex == 0) {
                emptyList()
            } else {
                listOf(
                    EvolutionDetailDto(
                        minLevel = 14 + stageIndex * 2,
                        trigger = NamedResourceDto(
                            "level-up",
                            "$API/evolution-trigger/level-up/",
                        ),
                    ),
                )
            }
            return ChainLinkDto(
                species = NamedResourceDto(
                    entry.name,
                    "$API/pokemon-species/${entry.id}/",
                ),
                evolutionDetails = details,
                evolvesTo = stages.getOrNull(stageIndex + 1)
                    ?.map { nodeAt(it, stageIndex + 1) }
                    .orEmpty(),
            )
        }

        return json.encodeToString(
            EvolutionChainDto(
                id = chainId,
                chain = nodeAt(stages.first().first(), 0),
            ),
        )
    }

    private fun colorFor(type: String): String = when (type) {
        "grass", "bug" -> "green"
        "fire" -> "red"
        "water" -> "blue"
        "electric" -> "yellow"
        "poison" -> "purple"
        "flying" -> "blue"
        else -> "gray"
    }
}
