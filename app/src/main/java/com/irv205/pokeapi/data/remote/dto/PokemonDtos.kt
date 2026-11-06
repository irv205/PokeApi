package com.irv205.pokeapi.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PokemonPageDto(
    val count: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val results: List<NamedResourceDto> = emptyList(),
)

@Serializable
data class NamedResourceDto(
    val name: String = "",
    val url: String = "",
)

@Serializable
data class PokemonDetailDto(
    val id: Int = 0,
    val name: String = "",
    val height: Int = 0,
    val weight: Int = 0,
    val types: List<TypeSlotDto> = emptyList(),
    val stats: List<StatSlotDto> = emptyList(),
    val abilities: List<AbilitySlotDto> = emptyList(),
    val sprites: SpritesDto = SpritesDto(),
)

@Serializable
data class TypeSlotDto(
    val slot: Int = 0,
    val type: NamedResourceDto = NamedResourceDto(),
)

@Serializable
data class StatSlotDto(
    @SerialName("base_stat") val baseStat: Int = 0,
    val effort: Int = 0,
    val stat: NamedResourceDto = NamedResourceDto(),
)

@Serializable
data class AbilitySlotDto(
    @SerialName("is_hidden") val isHidden: Boolean = false,
    val slot: Int = 0,
    val ability: NamedResourceDto = NamedResourceDto(),
)

@Serializable
data class SpritesDto(
    @SerialName("front_default") val frontDefault: String? = null,
    val other: OtherSpritesDto? = null,
)

@Serializable
data class OtherSpritesDto(
    @SerialName("official-artwork") val officialArtwork: OfficialArtworkDto? = null,
    val home: HomeSpritesDto? = null,
)

@Serializable
data class OfficialArtworkDto(
    @SerialName("front_default") val frontDefault: String? = null,
)

@Serializable
data class HomeSpritesDto(
    @SerialName("front_default") val frontDefault: String? = null,
)

@Serializable
data class PokemonSpeciesDto(
    val id: Int = 0,
    val name: String = "",
    val habitat: NamedResourceDto? = null,
    val color: NamedResourceDto? = null,
    val genera: List<GenusDto> = emptyList(),
    @SerialName("flavor_text_entries") val flavorTextEntries: List<FlavorTextEntryDto> = emptyList(),
    @SerialName("evolution_chain") val evolutionChain: EvolutionChainRefDto? = null,
    @SerialName("is_legendary") val isLegendary: Boolean = false,
    @SerialName("is_mythical") val isMythical: Boolean = false,
)

@Serializable
data class GenusDto(
    val genus: String = "",
    val language: NamedResourceDto = NamedResourceDto(),
)

@Serializable
data class FlavorTextEntryDto(
    @SerialName("flavor_text") val flavorText: String = "",
    val language: NamedResourceDto = NamedResourceDto(),
)

@Serializable
data class EvolutionChainRefDto(
    val url: String = "",
)

@Serializable
data class EvolutionChainDto(
    val id: Int = 0,
    val chain: ChainLinkDto = ChainLinkDto(),
)

@Serializable
data class ChainLinkDto(
    val species: NamedResourceDto = NamedResourceDto(),
    @SerialName("evolution_details") val evolutionDetails: List<EvolutionDetailDto> = emptyList(),
    @SerialName("evolves_to") val evolvesTo: List<ChainLinkDto> = emptyList(),
)

@Serializable
data class EvolutionDetailDto(
    @SerialName("min_level") val minLevel: Int? = null,
    val trigger: NamedResourceDto? = null,
    val item: NamedResourceDto? = null,
    @SerialName("min_happiness") val minHappiness: Int? = null,
    @SerialName("time_of_day") val timeOfDay: String = "",
    val location: NamedResourceDto? = null,
)
