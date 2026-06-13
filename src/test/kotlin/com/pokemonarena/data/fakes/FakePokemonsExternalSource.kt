package com.pokemonarena.data.fakes

import com.pokemonarena.data.external.broker.PokemonsExternalSource
import com.pokemonarena.domain.entity.Pokemon

class FakePokemonsExternalSource : PokemonsExternalSource {
    var shouldFail = false
    var pokemons: List<Pokemon> = listOf(
        Pokemon(1, "bulbasaur", "https://img/1.png", listOf("grass")),
        Pokemon(4, "charmander", "https://img/4.png", listOf("fire"))
    )

    override suspend fun getPokemons(limit: Int, offset: Int): List<Pokemon> {
        if (shouldFail) throw Exception("API error")
        return pokemons
    }
}
