package com.pokemonarena.data.external.source_b

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CardListResponse(
    val data: List<CardDto>,
    val totalCount: Int = 0
)

@Serializable
data class CardDto(
    val id: String, val name: String,
    val images: CardImagesDto,
    val rarity: String? = null,
    val set: CardSetDto
)

@Serializable
data class CardImagesDto(val small: String, val large: String)

@Serializable
data class CardSetDto(val name: String)
