package com.pokemonarena.presentation.screens.rogue

import com.pokemonarena.domain.entity.RogueBlessing
import com.pokemonarena.domain.entity.RogueEvent
import com.pokemonarena.domain.entity.RogueItem
import com.pokemonarena.domain.entity.RogueLives
import com.pokemonarena.domain.entity.RogueMap
import com.pokemonarena.domain.entity.RogueMetaState
import com.pokemonarena.domain.entity.RogueNodeType
import com.pokemonarena.domain.entity.RoguePokemon
import com.pokemonarena.domain.usecase.RogueStrike

data class RogueRunSnapshot(
    val act:            Int,
    val map:            RogueMap,
    val team:           List<RoguePokemon>,
    val inventory:      List<RogueItem>     = emptyList(),
    val currentNodeId:  Int?               = null,
    val clearedNodeIds: Set<Int>           = emptySet(),
    val blessings:      Set<RogueBlessing>  = emptySet(),
    val loot:           Int                = 0,
    val bossesDefeated: Int                = 0
) {
    val reachableNodeIds: List<Int> get() =
        if (currentNodeId == null) map.entryNodeIds else map.node(currentNodeId).next

    val aliveCount: Int get() = team.count { it.isAlive }

    fun node(id: Int) = map.node(id)
}

enum class RogueBattleOutcome { ONGOING, WON, LOST }

data class RogueBattleState(
    val node:              RogueNodeType,
    val enemies:           List<RoguePokemon>,
    val enemyIndex:        Int                = 0,
    val playerActiveIndex: Int                = 0,
    val log:               List<RogueStrike>  = emptyList(),
    val lastDuel:          List<RogueStrike>  = emptyList(),
    val outcome:           RogueBattleOutcome = RogueBattleOutcome.ONGOING,
    val turnId:            Int                = 0
) {
    val enemy:        RoguePokemon get() = enemies[enemyIndex]
    val isBoss:       Boolean      get() = node == RogueNodeType.BOSS
    val enemiesTotal: Int          get() = enemies.size
}

sealed interface RogueRewardOption {
    data class Recruit(val pokemon: RoguePokemon)    : RogueRewardOption
    object Heal                                      : RogueRewardOption
    data class Blessing(val blessing: RogueBlessing) : RogueRewardOption
    data class Loot(val coins: Int)                  : RogueRewardOption
    data class Gear(val item: RogueItem)             : RogueRewardOption
}

sealed interface RogueUiState {
    object Loading : RogueUiState

    data class Lobby(
        val coins:          Int,
        val meta:           RogueMetaState,
        val lives:          RogueLives,
        val purchaseNotice: String? = null
    ) : RogueUiState

    data class Draft(val starters: List<RoguePokemon>) : RogueUiState

    data class Map(val run: RogueRunSnapshot, val notice: String? = null) : RogueUiState

    data class Battle(val run: RogueRunSnapshot, val battle: RogueBattleState) : RogueUiState

    data class Capture(val run: RogueRunSnapshot, val candidates: List<RoguePokemon>) : RogueUiState

    data class Event(val run: RogueRunSnapshot, val event: RogueEvent) : RogueUiState

    data class Reward(
        val run:     RogueRunSnapshot,
        val options: List<RogueRewardOption>,
        val notes:   List<String> = emptyList()
    ) : RogueUiState

    data class Manage(val run: RogueRunSnapshot) : RogueUiState

    data class Finished(
        val run:     RogueRunSnapshot,
        val victory: Boolean,
        val payout:  Int
    ) : RogueUiState

    val locksNavigation: Boolean
        get() = when (this) {
            is Loading, is Lobby, is Finished -> false
            else                              -> true
        }
}

sealed interface RogueUiEvent {
    object OpenLobby                              : RogueUiEvent
    data class BuyUpgrade(val upgradeId: String)  : RogueUiEvent
    object Start                                  : RogueUiEvent
    data class PickStarter(val index: Int)        : RogueUiEvent
    data class PickNode(val nodeId: Int)          : RogueUiEvent
    object AdvanceBattle                          : RogueUiEvent
    object ConcludeBattle                         : RogueUiEvent
    data class PickCapture(val index: Int)        : RogueUiEvent
    object SkipCapture                            : RogueUiEvent
    data class PickEventOption(val index: Int)    : RogueUiEvent
    data class PickReward(val index: Int)         : RogueUiEvent
    object OpenManage                             : RogueUiEvent
    object CloseManage                            : RogueUiEvent
    data class EquipFromBag(val itemIndex: Int, val memberIndex: Int) : RogueUiEvent
    data class UnequipToBag(val memberIndex: Int) : RogueUiEvent
    data class ReorderTeam(val from: Int, val to: Int) : RogueUiEvent
    object Abandon                                : RogueUiEvent
}
