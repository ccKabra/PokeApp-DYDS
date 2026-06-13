package com.pokemonarena.data.external.broker

import com.pokemonarena.domain.entity.PokemonDetail

interface PokemonFullDetailExternalSource {
    suspend fun getPokemonDetailFull(name: String): PokemonDetail
}
