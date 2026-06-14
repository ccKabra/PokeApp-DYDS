package com.pokemonarena.presentation.screens.league

import com.pokemonarena.domain.entity.BattleResult
import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.Gym
import com.pokemonarena.domain.entity.Item
import com.pokemonarena.domain.entity.ItemCatalog
import com.pokemonarena.domain.entity.League
import com.pokemonarena.domain.entity.LeagueOpponent
import com.pokemonarena.domain.entity.Region
import com.pokemonarena.domain.entity.TeamRules
import com.pokemonarena.domain.usecase.FirstWinReward

sealed interface LeagueUiState {
    object Loading : LeagueUiState

    data class Locked(val region: Region, val message: String) : LeagueUiState

    data class Lobby(val league: League, val alreadyChampion: Boolean) : LeagueUiState

    data class Prep(
        val league:        League,
        val opponentIndex: Int,
        val owned:         List<Card>       = emptyList(),
        val team:          List<Card>       = emptyList(),
        val catalog:       List<Item>       = emptyList(),
        val inventory:     Map<String, Int> = emptyMap()
    ) : LeagueUiState {
        val opponent: LeagueOpponent get() = league.opponents[opponentIndex]
        val canFight: Boolean        get() = team.size == TeamRules.SIZE
        val availableItems: List<Pair<Item, Int>>
            get() = catalog.filterNot { ItemCatalog.isConsumable(it.id) }
                .mapNotNull { item -> inventory[item.id]?.takeIf { it > 0 }?.let { item to it } }
        val fatigueCures: Int get() = inventory[ItemCatalog.ENERGY_ROOT_ID] ?: 0
    }

    data class Strategy(
        val league:        League,
        val opponentIndex: Int,
        val arena:         Gym,
        val team:          List<Card>,
        val botTeam:       List<Card>
    ) : LeagueUiState {
        val opponent: LeagueOpponent get() = league.opponents[opponentIndex]
    }

    data class Combat(
        val league:         League,
        val opponentIndex:  Int,
        val result:         BattleResult,
        val itemDropNotice: String? = null
    ) : LeagueUiState {
        val opponent: LeagueOpponent get() = league.opponents[opponentIndex]
        val isLastOpponent: Boolean  get() = opponentIndex == league.opponents.lastIndex
    }

    data class Finished(
        val league:   League,
        val won:      Boolean,
        val defeated: Int,
        val reward:   FirstWinReward?
    ) : LeagueUiState

    data class Error(val message: String) : LeagueUiState
}

sealed interface LeagueUiEvent {
    data class Load(val region: Region) : LeagueUiEvent
    object Start : LeagueUiEvent
    data class ToggleCard(val cardId: String)                    : LeagueUiEvent
    data class EquipItem(val cardId: String, val itemId: String) : LeagueUiEvent
    data class UnequipItem(val cardId: String)                   : LeagueUiEvent
    data class CureFatigue(val cardId: String)                   : LeagueUiEvent
    object Fight : LeagueUiEvent
    data class MoveCard(val index: Int, val up: Boolean)         : LeagueUiEvent
    object ConfirmFight : LeagueUiEvent
    object Continue : LeagueUiEvent
}
