package com.pokemonarena.presentation.screens.items

import com.pokemonarena.domain.entity.Item

sealed interface ItemsUiEvent {
    data class Purchase(val item: Item) : ItemsUiEvent
}
