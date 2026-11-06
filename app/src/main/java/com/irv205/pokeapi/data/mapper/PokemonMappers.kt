package com.irv205.pokeapi.data.mapper

import com.irv205.pokeapi.core.common.PokemonImageUrls
import com.irv205.pokeapi.data.remote.dto.ChainLinkDto
import com.irv205.pokeapi.data.remote.dto.EvolutionChainDto
import com.irv205.pokeapi.data.remote.dto.NamedResourceDto
import com.irv205.pokeapi.data.remote.dto.PokemonDetailDto
import com.irv205.pokeapi.data.remote.dto.PokemonPageDto
import com.irv205.pokeapi.data.remote.dto.PokemonSpeciesDto
import com.irv205.pokeapi.domain.model.EvolutionChain
import com.irv205.pokeapi.domain.model.EvolutionNode
import com.irv205.pokeapi.domain.model.PokemonAbility
import com.irv205.pokeapi.domain.model.PokemonDetail
import com.irv205.pokeapi.domain.model.PokemonPage
import com.irv205.pokeapi.domain.model.PokemonSpecies
import com.irv205.pokeapi.domain.model.PokemonStat
import com.irv205.pokeapi.domain.model.PokemonSummary
import com.irv205.pokeapi.domain.model.PokemonType

fun PokemonPageDto.toDomain(): PokemonPage = PokemonPage(
    items = results.mapNotNull { it.toSummary() },
    hasMore = next != null,
)

fun NamedResourceDto.toSummary(): PokemonSummary? {
    val resourceId = idFromUrl() ?: return null
    return PokemonSummary(
        id = resourceId,
        name = name,
        imageUrl = PokemonImageUrls.officialArtwork(resourceId),
    )
}

fun NamedResourceDto.idFromUrl(): Int? = url.trimEnd('/').substringAfterLast('/').toIntOrNull()

fun PokemonDetailDto.toDomain(): PokemonDetail = PokemonDetail(
    id = id,
    name = name,
    height = height,
    weight = weight,
    types = types.map { PokemonType(name = it.type.name, slot = it.slot) }.sortedBy { it.slot },
    stats = stats.map { PokemonStat(name = it.stat.name, baseStat = it.baseStat) },
    abilities = abilities
        .sortedBy { it.slot }
        .map { PokemonAbility(name = it.ability.name, isHidden = it.isHidden) },
    imageUrl = sprites.other?.officialArtwork?.frontDefault
        ?: sprites.other?.home?.frontDefault
        ?: sprites.frontDefault
        ?: PokemonImageUrls.officialArtwork(id),
)

fun PokemonSpeciesDto.toDomain(): PokemonSpecies {
    val description = flavorTextEntries
        .firstOrNull { it.language.name == "es" }
        ?: flavorTextEntries.firstOrNull { it.language.name == "en" }
        ?: flavorTextEntries.firstOrNull()

    val genus = genera
        .firstOrNull { it.language.name == "es" }
        ?: genera.firstOrNull { it.language.name == "en" }
        ?: genera.firstOrNull()

    return PokemonSpecies(
        id = id,
        name = name,
        flavorText = description?.flavorText?.cleanFlavorText().orEmpty(),
        genus = genus?.genus.orEmpty(),
        habitat = habitat?.name,
        colorName = color?.name,
        isLegendary = isLegendary,
        isMythical = isMythical,
        evolutionChainId = evolutionChain?.url?.trimEnd('/')?.substringAfterLast('/')?.toIntOrNull(),
    )
}

fun EvolutionChainDto.toDomain(): EvolutionChain = EvolutionChain(
    id = id,
    root = chain.toDomain(),
)

fun ChainLinkDto.toDomain(): EvolutionNode {
    val resourceId = species.idFromUrl() ?: 0
    val firstDetail = evolutionDetails.firstOrNull()
    return EvolutionNode(
        speciesId = resourceId,
        name = species.name,
        imageUrl = PokemonImageUrls.officialArtwork(resourceId),
        minLevel = firstDetail?.minLevel,
        trigger = firstDetail?.item?.name ?: firstDetail?.trigger?.name,
        children = evolvesTo.map { it.toDomain() },
    )
}

private fun String.cleanFlavorText(): String = replace("\n", " ")
    .replace("\u000C", " ")
    .replace(Regex("\\s+"), " ")
    .trim()
