package com.pokemonarena.data.external.broker

class CardsSourceWithFallback(
    private val primary:   PokemonCardsExternalSource,
    private val secondary: PokemonCardsExternalSource
) : PokemonCardsExternalSource {

    override suspend fun getCardsByPokemonName(name: String): List<ExternalCardInfo> =
        primary.getCardsByPokemonName(name).ifEmpty { secondary.getCardsByPokemonName(name) }
}
