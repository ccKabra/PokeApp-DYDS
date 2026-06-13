package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.entity.PokemonDetail
import com.pokemonarena.domain.repository.PokemonRepository

interface GetPokemonDetailUseCase {
    suspend fun execute(name: String): PokemonDetail
}

class GetPokemonDetailUseCaseImpl(private val repository: PokemonRepository) : GetPokemonDetailUseCase {
    override suspend fun execute(name: String): PokemonDetail =
        repository.getPokemonDetail(name)
}
