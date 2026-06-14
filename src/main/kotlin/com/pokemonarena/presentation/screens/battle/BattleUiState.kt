package com.pokemonarena.presentation.screens.battle

import com.pokemonarena.domain.entity.BattleResult
import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.Gym
import com.pokemonarena.domain.entity.TeamRules
import com.pokemonarena.domain.entity.WeatherCondition

sealed interface BattleUiState {
    object Loading : BattleUiState

    data class Ready(
        val gym:       Gym,
        val teamCards: List<Card>,
        val botCards:  List<Card>,
        val weather:   WeatherCondition
    ) : BattleUiState {
        val canBattle get() = teamCards.size == TeamRules.SIZE
    }

    data class Combat(
        val gym:            Gym,
        val weather:        WeatherCondition,
        val result:         BattleResult,
        val itemDropNotice: String? = null
    ) : BattleUiState

    data class Error(val message: String) : BattleUiState
}
