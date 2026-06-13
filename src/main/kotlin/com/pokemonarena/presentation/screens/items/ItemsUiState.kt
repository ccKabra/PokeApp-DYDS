package com.pokemonarena.presentation.screens.items

import com.pokemonarena.domain.entity.Economy
import com.pokemonarena.domain.entity.Item
import com.pokemonarena.presentation.UiMessage

data class ItemsUiState(
    val catalog:   List<Item>       = emptyList(),
    val inventory: Map<String, Int> = emptyMap(),
    val coins:     Int              = Economy.STARTING_COINS,
    val isLoading: Boolean          = true,
    val message:   UiMessage?       = null
)
