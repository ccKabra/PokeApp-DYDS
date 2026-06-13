package com.pokemonarena.data.external.broker

data class ExternalCardInfo(
    val id:         String,
    val name:       String,
    val imageSmall: String,
    val imageLarge: String,
    val rarity:     String?,
    val setName:    String
)

interface PokemonCardsExternalSource {
    suspend fun getCardsByPokemonName(name: String): List<ExternalCardInfo>
}
