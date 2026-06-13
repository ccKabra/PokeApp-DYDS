package com.pokemonarena.presentation.screens.rogue

import com.pokemonarena.domain.entity.RogueBlessing
import com.pokemonarena.domain.entity.RogueItem
import com.pokemonarena.domain.entity.RogueNodeType
import com.pokemonarena.domain.entity.RoguePokemon
import com.pokemonarena.domain.entity.RogueRules
import com.pokemonarena.domain.usecase.RogueStrike

data class RogueRunSnapshot(
    val floor:       Int                = 1,
    val team:        List<RoguePokemon> = emptyList(),
    val activeIndex: Int                = 0,
    val blessings:   Set<RogueBlessing> = emptySet(),
    val loot:        Int                = 0,
    val hopeTokens:  Int                = RogueRules.HOPE_TOKENS_START
)

sealed interface RogueRewardOption {
    data class Recruit(val pokemon: RoguePokemon)      : RogueRewardOption
    object Heal                                        : RogueRewardOption
    data class Blessing(val blessing: RogueBlessing)   : RogueRewardOption
    data class Loot(val coins: Int)                    : RogueRewardOption
    data class Gear(val item: RogueItem)               : RogueRewardOption
}

sealed interface RogueUiState {
    object Idle : RogueUiState

    data class Draft(val starters: List<RoguePokemon>) : RogueUiState

    data class PathChoice(
        val run:     RogueRunSnapshot,
        val options: List<RogueNodeType>,
        val notice:  String? = null
    ) : RogueUiState

    data class Battle(
        val run:           RogueRunSnapshot,
        val node:          RogueNodeType,
        val enemy:         RoguePokemon,
        val log:           List<RogueStrike> = emptyList(),
        val awaitingSwap:  Boolean           = false,
        val turnId:        Int               = 0,
        val lastTurn:      List<RogueStrike> = emptyList(),
        val turnsSurvived: Int               = 0,
        val enrageCount:   Int               = 0,
        val taunt:         String?           = null
    ) : RogueUiState {
        val active: RoguePokemon get() = run.team[run.activeIndex]
        val isBoss: Boolean        get() = node == RogueNodeType.BOSS
        val canFlee: Boolean       get() = !isBoss && turnsSurvived >= RogueRules.TURNS_TO_ESCAPE
        val canSpendHope: Boolean  get() = !isBoss && run.hopeTokens > 0
        val turnsToEscape: Int     get() = (RogueRules.TURNS_TO_ESCAPE - turnsSurvived).coerceAtLeast(0)
    }

    data class Reward(
        val run:     RogueRunSnapshot,
        val options: List<RogueRewardOption>,
        val notes:   List<String> = emptyList()
    ) : RogueUiState

    /** Elegís a qué Pokémon equiparle el item recién obtenido. */
    data class EquipGear(
        val run:  RogueRunSnapshot,
        val item: RogueItem
    ) : RogueUiState

    data class Finished(
        val run:     RogueRunSnapshot,
        val victory: Boolean,
        val payout:  Int
    ) : RogueUiState
}

sealed interface RogueUiEvent {
    object Start                            : RogueUiEvent
    data class PickStarter(val index: Int)  : RogueUiEvent
    data class PickNode(val index: Int)     : RogueUiEvent
    data class Attack(val moveIndex: Int)   : RogueUiEvent
    data class SetActive(val index: Int)    : RogueUiEvent
    data class PickReward(val index: Int)   : RogueUiEvent
    data class EquipOn(val index: Int)      : RogueUiEvent
    object Flee                             : RogueUiEvent
    object SpendHope                        : RogueUiEvent
    object Abandon                          : RogueUiEvent
    object BackToIdle                       : RogueUiEvent
}
