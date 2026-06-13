package com.pokemonarena.presentation.screens.myteam

import com.pokemonarena.domain.usecase.CureFatigueUseCase
import com.pokemonarena.domain.usecase.EquipItemUseCase
import com.pokemonarena.domain.usecase.GetItemCatalogUseCase
import com.pokemonarena.domain.usecase.GetItemInventoryUseCase
import com.pokemonarena.domain.usecase.GetOwnedCardsUseCase
import com.pokemonarena.domain.usecase.GetTeamUseCase
import com.pokemonarena.domain.usecase.GetUserStatisticsUseCase
import com.pokemonarena.domain.usecase.SellCardUseCase
import com.pokemonarena.domain.usecase.UnequipItemUseCase
import com.pokemonarena.domain.usecase.UpdateTeamUseCase
import com.pokemonarena.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyTeamViewModel(
    private val getOwned:     GetOwnedCardsUseCase,
    private val getTeam:      GetTeamUseCase,
    private val updateTeam:   UpdateTeamUseCase,
    private val getStats:     GetUserStatisticsUseCase,
    private val sellCard:     SellCardUseCase,
    private val getCatalog:   GetItemCatalogUseCase,
    private val getInventory: GetItemInventoryUseCase,
    private val equipItem:    EquipItemUseCase,
    private val unequipItem:  UnequipItemUseCase,
    private val cureFatigue:  CureFatigueUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(MyTeamState())
    val uiState: StateFlow<MyTeamState> = _uiState.asStateFlow()

    init {
        scope.launch {
            combine(getOwned.execute(), getTeam.execute(), getStats.execute(),
                    getInventory.execute()) { owned, team, stats, inventory ->
                MyTeamState(owned, team, stats, _uiState.value.catalog, inventory, isLoading = false)
            }.collect { state -> _uiState.update { state.copy(catalog = it.catalog) } }
        }
        scope.launch {
            val catalog = runCatching { getCatalog.execute() }.getOrDefault(emptyList())
            _uiState.update { it.copy(catalog = catalog) }
        }
    }

    fun onEvent(event: MyTeamUiEvent) {
        when (event) {
            is MyTeamUiEvent.ToggleCard  -> scope.launch { toggleCard(event.cardId) }
            is MyTeamUiEvent.SellCard    -> scope.launch { sell(event.cardId) }
            is MyTeamUiEvent.EquipItem   -> scope.launch { equip(event.cardId, event.itemId) }
            is MyTeamUiEvent.UnequipItem -> scope.launch { unequip(event.cardId) }
            is MyTeamUiEvent.CureFatigue -> scope.launch { cure(event.cardId) }
        }
    }

    private suspend fun cure(cardId: String) {
        val card = _uiState.value.ownedCards.firstOrNull { it.id == cardId } ?: return
        cureFatigue.execute(card)
    }

    suspend fun toggleCard(cardId: String) {
        val team     = _uiState.value.teamCards
        val isInTeam = team.any { it.id == cardId }
        updateTeam.execute(cardId, !isInTeam, team)
    }

    private suspend fun sell(cardId: String) {
        val card = _uiState.value.ownedCards.firstOrNull { it.id == cardId } ?: return
        sellCard.execute(card)
    }

    private suspend fun equip(cardId: String, itemId: String) {
        val card = _uiState.value.ownedCards.firstOrNull { it.id == cardId } ?: return
        val item = _uiState.value.catalog.firstOrNull { it.id == itemId } ?: return
        equipItem.execute(card, item)
    }

    private suspend fun unequip(cardId: String) {
        val card = _uiState.value.ownedCards.firstOrNull { it.id == cardId } ?: return
        unequipItem.execute(card)
    }
}
