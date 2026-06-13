package com.pokemonarena.presentation.screens.myteam

sealed interface MyTeamUiEvent {
    data class ToggleCard(val cardId: String) : MyTeamUiEvent
    data class SellCard(val cardId: String)   : MyTeamUiEvent
    data class EquipItem(val cardId: String, val itemId: String) : MyTeamUiEvent
    data class UnequipItem(val cardId: String)                   : MyTeamUiEvent
    data class CureFatigue(val cardId: String)                   : MyTeamUiEvent
}
