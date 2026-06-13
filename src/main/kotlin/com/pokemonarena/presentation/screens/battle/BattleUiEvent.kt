package com.pokemonarena.presentation.screens.battle

sealed interface BattleUiEvent {
    data class Load(val gymName: String) : BattleUiEvent
    data class MoveCard(val index: Int, val up: Boolean) : BattleUiEvent
    object Fight : BattleUiEvent
    object ContinueToResult : BattleUiEvent
}
