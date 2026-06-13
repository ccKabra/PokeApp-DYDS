package com.pokemonarena.data.external.broker

import com.pokemonarena.domain.entity.Pokemon

class PokemonDetailBroker(
    private val pokeApiSource: PokemonDetailExternalSource,
    private val tcgSource:     PokemonDetailExternalSource
) : PokemonDetailExternalSource {

    override suspend fun getPokemonByName(name: String): Pokemon? {
        val pokeData = runCatching { pokeApiSource.getPokemonByName(name) }.getOrNull()
        val tcgData  = runCatching { tcgSource.getPokemonByName(name) }.getOrNull()

        return when {
            pokeData != null && tcgData != null -> mergePokemon(pokeData, tcgData)
            pokeData != null                    -> pokeData
            tcgData  != null                    -> tcgData
            else                                -> null
        }
    }

    private fun mergePokemon(pokeApi: Pokemon, tcg: Pokemon): Pokemon =
        pokeApi.copy(
            imageUrl = tcg.imageUrl.ifEmpty { pokeApi.imageUrl }
        )
}
