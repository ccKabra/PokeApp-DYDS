package com.pokemonarena.data.external.source_a

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PokemonListResponse(
    val count: Int, val next: String?, val previous: String?,
    val results: List<PokemonItemDto>
)

@Serializable
data class PokemonItemDto(val name: String, val url: String) {
    fun extractId(): Int = url.trimEnd('/').substringAfterLast('/').toIntOrNull() ?: 0
}

@Serializable
data class PokemonDetailResponse(
    val id: Int, val name: String, val height: Int, val weight: Int,
    val sprites: SpritesDto, val stats: List<StatDto>,
    val types: List<TypeSlotDto>, val species: SpeciesRefDto
)

@Serializable
data class SpritesDto(
    @SerialName("front_default") val frontDefault: String?,
    val other: OtherSpritesDto? = null
)

@Serializable
data class OtherSpritesDto(
    @SerialName("official-artwork") val officialArtwork: OfficialArtworkDto? = null
)

@Serializable
data class OfficialArtworkDto(@SerialName("front_default") val frontDefault: String?)

@Serializable
data class StatDto(@SerialName("base_stat") val baseStat: Int, val stat: StatNameDto)

@Serializable
data class StatNameDto(val name: String)

@Serializable
data class TypeSlotDto(val slot: Int, val type: TypeNameDto)

@Serializable
data class TypeNameDto(val name: String, val url: String = "")

@Serializable
data class SpeciesRefDto(val name: String, val url: String)

@Serializable
data class PokemonSpeciesDto(
    @SerialName("evolution_chain") val evolutionChain: EvolutionChainUrlDto
)

@Serializable
data class EvolutionChainUrlDto(val url: String) {
    fun extractChainId(): Int = url.trimEnd('/').substringAfterLast('/').toIntOrNull() ?: 1
}

@Serializable
data class EvolutionChainResponse(val id: Int, val chain: ChainLinkDto)

@Serializable
data class ChainLinkDto(
    val species: SpeciesNameDto,
    @SerialName("evolves_to") val evolvesTo: List<ChainLinkDto> = emptyList()
)

@Serializable
data class SpeciesNameDto(val name: String, val url: String = "")

fun ChainLinkDto.toNameList(): List<String> = buildList {
    fun traverse(link: ChainLinkDto) { add(link.species.name); link.evolvesTo.forEach { traverse(it) } }
    traverse(this@toNameList)
}
