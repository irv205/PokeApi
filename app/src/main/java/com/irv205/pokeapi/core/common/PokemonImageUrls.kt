package com.irv205.pokeapi.core.common

/**
 * PokeAPI does not include artwork in the paginated list response, so we build
 * the official artwork URL from the Pokemon id. The sprites repository is the
 * same one used by PokeAPI.
 */
object PokemonImageUrls {
    private const val SPRITES_BASE =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon"

    fun officialArtwork(id: Int): String = "$SPRITES_BASE/other/official-artwork/$id.png"

    fun home(id: Int): String = "$SPRITES_BASE/other/home/$id.png"

    fun pixelSprite(id: Int): String = "$SPRITES_BASE/$id.png"
}

/** Converts API names such as `mr-mime` or `special-attack` into `Mr Mime` / `Special Attack`. */
fun String.toDisplayName(): String = split('-', '_')
    .filter { it.isNotBlank() }
    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
