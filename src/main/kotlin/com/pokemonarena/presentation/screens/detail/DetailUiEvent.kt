package com.pokemonarena.presentation.screens.detail

import com.pokemonarena.domain.entity.Card

sealed interface DetailUiEvent {
    data class Load(val pokemonName: String) : DetailUiEvent
    data class CardSelected(val card: Card)  : DetailUiEvent
    object PurchaseRequested                 : DetailUiEvent
}
