package com.pokemonarena.data.external.broker

import com.pokemonarena.domain.entity.Pokemon

interface PokemonsExternalSource {
    suspend fun getPokemons(limit: Int, offset: Int): List<Pokemon>
}
