package com.irv205.pokeapi.domain.model

data class PokemonSummary(
    val id: Int,
    val name: String,
    val imageUrl: String,
)

data class PokemonPage(
    val items: List<PokemonSummary>,
    val hasMore: Boolean,
)

data class PokemonType(
    val name: String,
    val slot: Int,
)

data class PokemonStat(
    val name: String,
    val baseStat: Int,
)

data class PokemonAbility(
    val name: String,
    val isHidden: Boolean,
)

data class PokemonDetail(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val types: List<PokemonType>,
    val stats: List<PokemonStat>,
    val abilities: List<PokemonAbility>,
    val imageUrl: String,
) {
    val primaryTypeName: String
        get() = types.minByOrNull { it.slot }?.name ?: "normal"
}

data class PokemonSpecies(
    val id: Int,
    val name: String,
    val flavorText: String,
    val genus: String,
    val habitat: String?,
    val colorName: String?,
    val isLegendary: Boolean,
    val isMythical: Boolean,
    val evolutionChainId: Int?,
)

data class PokemonDetailPage(
    val pokemon: PokemonDetail,
    val species: PokemonSpecies,
)

data class EvolutionChain(
    val id: Int,
    val root: EvolutionNode,
) {
    /** Evolution levels, e.g. [Bulbasaur], [Ivysaur], [Venusaur]. Branching chains stay grouped. */
    fun stages(): List<List<EvolutionNode>> {
        val result = mutableListOf<List<EvolutionNode>>()
        var current = listOf(root)
        while (current.isNotEmpty()) {
            result += current
            current = current.flatMap { it.children }
        }
        return result
    }
}

data class EvolutionNode(
    val speciesId: Int,
    val name: String,
    val imageUrl: String,
    val minLevel: Int?,
    val trigger: String?,
    val children: List<EvolutionNode> = emptyList(),
) {
    val evolutionRequirement: String?
        get() = when {
            minLevel != null -> "Nv. $minLevel"
            trigger == "trade" -> "Intercambio"
            trigger == "use-item" -> "Usar objeto"
            !trigger.isNullOrBlank() -> trigger.replaceFirstChar { it.uppercase() }
            else -> null
        }
}

data class FavoritePokemon(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val primaryType: String? = null,
)
