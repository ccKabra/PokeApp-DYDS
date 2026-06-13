package com.pokemonarena.presentation.screens.collection

import com.pokemonarena.domain.entity.Pokemon
import com.pokemonarena.domain.entity.Region

data class CollectionUiState(
    val allPokemons:     List<Pokemon> = emptyList(),
    val filteredPokemons:List<Pokemon> = emptyList(),
    val query:           String        = "",
    val unlockedRegions: Set<Region>   = setOf(Region.KANTO),
    val nextRegionHint:  String?       = null,
    val isLoading:       Boolean       = false,
    val error:           String?       = null
)
