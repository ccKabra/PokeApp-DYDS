package com.pokemonarena.data.external.source_b

import com.pokemonarena.core.Constants
import com.pokemonarena.data.external.broker.ExternalCardInfo
import com.pokemonarena.data.external.broker.PokemonCardsExternalSource
import com.pokemonarena.data.external.broker.PokemonDetailExternalSource
import com.pokemonarena.domain.entity.Pokemon
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class TcgExternalSource(private val client: HttpClient) :
    PokemonDetailExternalSource, PokemonCardsExternalSource {

    override suspend fun getPokemonByName(name: String): Pokemon? =
        runCatching {
            fetchPage(name, page = 1, pageSize = 1).data.firstOrNull()?.toDomainPokemon(name)
        }.getOrNull()

    override suspend fun getCardsByPokemonName(name: String): List<ExternalCardInfo> =
        runCatching { fetchAllCards(name).map { it.toCardInfo() } }.getOrDefault(emptyList())

    private suspend fun fetchAllCards(name: String): List<CardDto> {
        val cards = mutableListOf<CardDto>()
        var page = 1
        while (true) {
            val response = fetchPage(name, page, Constants.TCG_PAGE_SIZE)
            cards += response.data
            if (response.data.isEmpty() || cards.size >= response.totalCount) break
            page++
        }
        return cards
    }

    private suspend fun fetchPage(name: String, page: Int, pageSize: Int): CardListResponse =
        client.get("${Constants.TCG_BASE_URL}/cards") {
            parameter("q",        "name:${name.trim()}")
            parameter("page",     page)
            parameter("pageSize", pageSize)
        }.body<CardListResponse>()

    private fun CardDto.toCardInfo(): ExternalCardInfo =
        ExternalCardInfo(id, name, images.small, images.large, rarity, set.name)

    private fun CardDto.toDomainPokemon(originalName: String): Pokemon =
        Pokemon(id = 0, name = originalName, imageUrl = images.small, types = emptyList())
}
