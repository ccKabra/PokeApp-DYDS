package com.pokemonarena.data.repository

import com.pokemonarena.data.external.broker.PokemonFullDetailExternalSource
import com.pokemonarena.data.external.broker.PokemonsExternalSource
import com.pokemonarena.domain.entity.Pokemon
import com.pokemonarena.domain.entity.PokemonDetail
import com.pokemonarena.domain.repository.PokemonRepository

class PokemonRepositoryImpl(
    private val listSource:   PokemonsExternalSource,
    private val detailSource: PokemonFullDetailExternalSource
) : PokemonRepository {

    override suspend fun getPokemonList(limit: Int, offset: Int): List<Pokemon> =
        listSource.getPokemons(limit, offset)

    override suspend fun getPokemonDetail(name: String): PokemonDetail =
        detailSource.getPokemonDetailFull(name)
}
