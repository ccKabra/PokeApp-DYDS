package com.pokemonarena.presentation.screens.detail

import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.Economy
import com.pokemonarena.domain.entity.PokemonDetail
import com.pokemonarena.presentation.UiMessage

sealed interface DetailUiState {
    object Loading : DetailUiState
    data class Success(
        val detail:       PokemonDetail,
        val cards:        List<Card>,
        val selectedCard: Card? = null,
        val ownedCardIds: Set<String> = emptySet(),
        val coins:        Int = Economy.STARTING_COINS,
        val message:      UiMessage? = null
    ) : DetailUiState
    data class Error(val message: String) : DetailUiState
}
