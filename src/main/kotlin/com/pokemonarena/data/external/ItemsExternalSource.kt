package com.pokemonarena.data.external

import com.pokemonarena.core.Constants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
data class ItemResponse(val sprites: ItemSpritesDto = ItemSpritesDto())

@Serializable
data class ItemSpritesDto(val default: String? = null)

class ItemsExternalSource(private val client: HttpClient) {

    suspend fun getItemSpriteUrl(itemId: String): String? =
        runCatching {
            client.get("${Constants.POKEAPI_BASE_URL}/item/$itemId")
                .body<ItemResponse>().sprites.default
        }.getOrNull()
}
