package com.pokemonarena.data.external.source_c

import com.pokemonarena.core.Constants
import com.pokemonarena.data.external.broker.ExternalCardInfo
import com.pokemonarena.data.external.broker.PokemonCardsExternalSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class TcgDexExternalSource(private val client: HttpClient) : PokemonCardsExternalSource {

    override suspend fun getCardsByPokemonName(name: String): List<ExternalCardInfo> =
        runCatching {
            fetchCards(name.trim().replace("\"", ""))
                .filter { it.image != null }
                .map { it.toCardInfo() }
        }.getOrDefault(emptyList())

    private suspend fun fetchCards(name: String): List<TcgDexCardDto> {
        val query = """{ cards(filters: {name: "$name"}) { id name rarity image set { name } } }"""
        return client.post(Constants.TCGDEX_GRAPHQL_URL) {
            contentType(ContentType.Application.Json)
            setBody(TcgDexGraphQLRequest(query))
        }.body<TcgDexGraphQLResponse>().data?.cards.orEmpty()
    }

    private fun TcgDexCardDto.toCardInfo() = ExternalCardInfo(
        id         = id,
        name       = name,
        imageSmall = "$image/low.webp",
        imageLarge = "$image/high.webp",
        rarity     = rarity?.takeUnless { it.equals("None", ignoreCase = true) },
        setName    = set?.name ?: "TCGdex"
    )
}
