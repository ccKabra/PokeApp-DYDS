package com.pokemonarena.domain.fakes

import com.pokemonarena.domain.entity.Pokemon
import com.pokemonarena.domain.entity.PokemonDetail
import com.pokemonarena.domain.entity.Stats
import com.pokemonarena.domain.repository.PokemonRepository

class FakePokemonRepository : PokemonRepository {
    var shouldFail = false
    var pokemons: List<Pokemon> = listOf(
        Pokemon(1, "bulbasaur", "url1", listOf("grass")),
        Pokemon(4, "charmander","url4", listOf("fire"))
    )

    override suspend fun getPokemonList(limit: Int, offset: Int): List<Pokemon> {
        if (shouldFail) throw Exception("Repository error")
        return pokemons
    }

    override suspend fun getPokemonDetail(name: String): PokemonDetail {
        if (shouldFail) throw Exception("Repository error")
        val p = pokemons.firstOrNull { it.name == name }
            ?: throw Exception("Pokemon not found: $name")
        return PokemonDetail(p.id, p.name, p.imageUrl, p.types, 0, 0, Stats(0,0,0,0,0,0), listOf(p.name))
    }
}
