package com.pokemonarena.data.external.broker

import com.pokemonarena.domain.entity.Pokemon

interface PokemonDetailExternalSource {
    suspend fun getPokemonByName(name: String): Pokemon?
}
