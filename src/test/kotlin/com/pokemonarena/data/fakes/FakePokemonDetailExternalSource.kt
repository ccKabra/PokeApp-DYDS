package com.pokemonarena.data.fakes

import com.pokemonarena.data.external.broker.PokemonDetailExternalSource
import com.pokemonarena.domain.entity.Pokemon

class FakePokemonDetailExternalSource : PokemonDetailExternalSource {
    var shouldFail   = false
    var pokemonToReturn: Pokemon? = Pokemon(1, "bulbasaur", "https://img/bulbasaur.png", listOf("grass"))

    override suspend fun getPokemonByName(name: String): Pokemon? {
        if (shouldFail) throw Exception("Source error")
        return pokemonToReturn
    }
}
