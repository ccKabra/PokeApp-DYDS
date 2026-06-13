package com.pokemonarena.presentation.fakes

import com.pokemonarena.domain.entity.Pokemon
import com.pokemonarena.domain.usecase.GetPokemonsUseCase

class FakeGetPokemonsUseCase : GetPokemonsUseCase {
    var shouldFail = false
    var pokemons: List<Pokemon> = listOf(
        Pokemon(1, "bulbasaur",  "url1", listOf("grass")),
        Pokemon(4, "charmander", "url4", listOf("fire"))
    )

    override suspend fun execute(limit: Int, offset: Int): List<Pokemon> {
        if (shouldFail) throw Exception("Use case error")
        return pokemons
    }
}
