package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.entity.Pokemon
import com.pokemonarena.domain.repository.PokemonRepository

interface GetPokemonsUseCase {
    suspend fun execute(limit: Int = TOTAL_POKEMON_COUNT, offset: Int = 0): List<Pokemon>

    companion object {
        const val TOTAL_POKEMON_COUNT = 1302
    }
}

class GetPokemonsUseCaseImpl(private val repository: PokemonRepository) : GetPokemonsUseCase {
    override suspend fun execute(limit: Int, offset: Int): List<Pokemon> =
        repository.getPokemonList(limit, offset)
}
