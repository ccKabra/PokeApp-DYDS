package com.pokemonarena.data.external.source_a

import com.pokemonarena.core.Constants
import com.pokemonarena.data.external.broker.PokemonDetailExternalSource
import com.pokemonarena.data.external.broker.PokemonFullDetailExternalSource
import com.pokemonarena.data.external.broker.PokemonsExternalSource
import com.pokemonarena.domain.entity.Pokemon
import com.pokemonarena.domain.entity.PokemonDetail
import com.pokemonarena.domain.entity.Stats
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class PokeApiExternalSource(private val client: HttpClient) :
    PokemonsExternalSource, PokemonDetailExternalSource, PokemonFullDetailExternalSource {

    override suspend fun getPokemons(limit: Int, offset: Int): List<Pokemon> =
        client.get("${Constants.POKEAPI_BASE_URL}/pokemon") {
            parameter("limit",  limit)
            parameter("offset", offset)
        }.body<PokemonListResponse>().results.map { dto ->
            val id = dto.extractId()
            Pokemon(id, dto.name, artworkUrl(id), emptyList())
        }

    override suspend fun getPokemonByName(name: String): Pokemon? =
        runCatching { getPokemonDetailFull(name).let { d -> Pokemon(d.id, d.name, d.imageUrl, d.types) } }
            .getOrNull()

    override suspend fun getPokemonDetailFull(name: String): PokemonDetail {
        val detail    = client.get("${Constants.POKEAPI_BASE_URL}/pokemon/$name").body<PokemonDetailResponse>()
        val speciesId = detail.species.url.trimEnd('/').substringAfterLast('/').toIntOrNull() ?: 1
        val chainId   = runCatching {
            client.get("${Constants.POKEAPI_BASE_URL}/pokemon-species/$speciesId/")
                .body<PokemonSpeciesDto>().evolutionChain.extractChainId()
        }.getOrDefault(speciesId)
        val chain = runCatching {
            client.get("${Constants.POKEAPI_BASE_URL}/evolution-chain/$chainId/")
                .body<EvolutionChainResponse>().chain.toNameList()
        }.getOrElse { listOf(detail.name) }
        return PokemonDetail(
            id             = detail.id,
            name           = detail.name,
            imageUrl       = detail.sprites.other?.officialArtwork?.frontDefault
                             ?: detail.sprites.frontDefault ?: artworkUrl(detail.id),
            types          = detail.types.sortedBy { it.slot }.map { it.type.name },
            height         = detail.height,
            weight         = detail.weight,
            stats          = detail.stats.toStats(),
            evolutionChain = chain
        )
    }

    private fun List<StatDto>.toStats() = Stats(
        hp             = firstOrNull { it.stat.name == "hp"              }?.baseStat ?: 0,
        attack         = firstOrNull { it.stat.name == "attack"          }?.baseStat ?: 0,
        defense        = firstOrNull { it.stat.name == "defense"         }?.baseStat ?: 0,
        specialAttack  = firstOrNull { it.stat.name == "special-attack"  }?.baseStat ?: 0,
        specialDefense = firstOrNull { it.stat.name == "special-defense" }?.baseStat ?: 0,
        speed          = firstOrNull { it.stat.name == "speed"           }?.baseStat ?: 0
    )
}

internal fun artworkUrl(id: Int) =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
