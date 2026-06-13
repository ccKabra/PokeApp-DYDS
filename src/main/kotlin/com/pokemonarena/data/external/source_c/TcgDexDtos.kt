package com.pokemonarena.data.external.source_c

import kotlinx.serialization.Serializable

@Serializable
data class TcgDexGraphQLRequest(val query: String)

@Serializable
data class TcgDexGraphQLResponse(val data: TcgDexCardsData? = null)

@Serializable
data class TcgDexCardsData(val cards: List<TcgDexCardDto> = emptyList())

@Serializable
data class TcgDexCardDto(
    val id: String,
    val name: String,
    val rarity: String? = null,
    val image: String? = null,
    val set: TcgDexSetDto? = null
)

@Serializable
data class TcgDexSetDto(val name: String? = null)
