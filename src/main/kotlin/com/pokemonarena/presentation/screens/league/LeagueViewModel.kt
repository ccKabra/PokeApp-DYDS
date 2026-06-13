package com.pokemonarena.presentation.screens.league

import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.Gym
import com.pokemonarena.domain.entity.League
import com.pokemonarena.domain.entity.Progression
import com.pokemonarena.domain.entity.Region
import com.pokemonarena.domain.entity.WeatherCondition
import com.pokemonarena.domain.usecase.AwardBadgeIfFirstWinUseCase
import com.pokemonarena.domain.usecase.CureFatigueUseCase
import com.pokemonarena.domain.usecase.DropHeldItemUseCase
import com.pokemonarena.domain.usecase.EquipItemUseCase
import com.pokemonarena.domain.usecase.GetGymsUseCase
import com.pokemonarena.domain.usecase.GetItemCatalogUseCase
import com.pokemonarena.domain.usecase.GetItemInventoryUseCase
import com.pokemonarena.domain.usecase.GetLeaguesUseCase
import com.pokemonarena.domain.usecase.GetOwnedCardsUseCase
import com.pokemonarena.domain.usecase.GetRegionProgressUseCase
import com.pokemonarena.domain.usecase.GetTeamUseCase
import com.pokemonarena.domain.usecase.SaveBattleResultUseCase
import com.pokemonarena.domain.usecase.SimulateBattleUseCase
import com.pokemonarena.domain.usecase.UnequipItemUseCase
import com.pokemonarena.domain.usecase.UpdateTeamUseCase
import com.pokemonarena.presentation.BaseViewModel
import com.pokemonarena.presentation.utils.resolveDropFor
import com.pokemonarena.presentation.utils.swappedAdjacent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class LeagueViewModel(
    private val getLeagues:   GetLeaguesUseCase,
    private val getGyms:      GetGymsUseCase,
    private val getProgress:  GetRegionProgressUseCase,
    private val getOwned:     GetOwnedCardsUseCase,
    private val getTeam:      GetTeamUseCase,
    private val updateTeam:   UpdateTeamUseCase,
    private val getCatalog:   GetItemCatalogUseCase,
    private val getInventory: GetItemInventoryUseCase,
    private val equipItem:    EquipItemUseCase,
    private val unequipItem:  UnequipItemUseCase,
    private val cureFatigue:  CureFatigueUseCase,
    private val simulate:     SimulateBattleUseCase,
    private val saveResult:   SaveBattleResultUseCase,
    private val dropHeldItem: DropHeldItemUseCase,
    private val awardBadge:   AwardBadgeIfFirstWinUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<LeagueUiState>(LeagueUiState.Loading)
    val uiState: StateFlow<LeagueUiState> = _uiState.asStateFlow()

    private var prepJob: Job? = null
    private var victories = 0

    fun onEvent(event: LeagueUiEvent) {
        when (event) {
            is LeagueUiEvent.Load        -> scope.launch { load(event.region) }
            is LeagueUiEvent.Start       -> scope.launch { startChallenge() }
            is LeagueUiEvent.ToggleCard  -> scope.launch { toggleCard(event.cardId) }
            is LeagueUiEvent.EquipItem   -> scope.launch { equip(event.cardId, event.itemId) }
            is LeagueUiEvent.UnequipItem -> scope.launch { unequip(event.cardId) }
            is LeagueUiEvent.CureFatigue -> scope.launch { cure(event.cardId) }
            is LeagueUiEvent.Fight        -> scope.launch { enterStrategy() }
            is LeagueUiEvent.MoveCard     -> moveCard(event.index, event.up)
            is LeagueUiEvent.ConfirmFight -> scope.launch { fight() }
            is LeagueUiEvent.Continue     -> scope.launch { advance() }
        }
    }

    private suspend fun load(region: Region) {
        prepJob?.cancel()
        victories = 0
        _uiState.value = LeagueUiState.Loading
        runCatching {
            val league   = getLeagues.execute().first { it.region == region }
            val gyms     = getGyms.execute()
            val progress = getProgress.execute()
            when {
                region !in progress.unlockedRegions ->
                    LeagueUiState.Locked(region,
                        "Completá la región de ${region.previous?.displayName} para llegar a ${region.displayName}.")
                !Progression.hasAllGymBadges(region, gyms, progress.earnedBadges) ->
                    LeagueUiState.Locked(region,
                        "Necesitás las ${Progression.gymsOf(region, gyms).size} medallas de ${region.displayName} para desafiar a la Liga.")
                else -> LeagueUiState.Lobby(league, alreadyChampion = league.name in progress.earnedBadges)
            }
        }.onSuccess { _uiState.value = it }
         .onFailure { _uiState.value = LeagueUiState.Error(it.message ?: "Error") }
    }

    private suspend fun startChallenge() {
        val lobby = _uiState.value as? LeagueUiState.Lobby ?: return
        victories = 0
        val team = runCatching { getTeam.execute().first() }.getOrDefault(emptyList())
        if (team.size == Gym.BOT_TEAM_SIZE) enterStrategy(lobby.league, 0, team)
        else enterPrep(lobby.league, 0)
    }

    private fun enterPrep(league: League, opponentIndex: Int) {
        prepJob?.cancel()
        _uiState.value = LeagueUiState.Prep(league, opponentIndex)
        prepJob = scope.launch {
            val catalog = runCatching { getCatalog.execute() }.getOrDefault(emptyList())
            combine(getOwned.execute(), getTeam.execute(), getInventory.execute()) { owned, team, inv ->
                Triple(owned, team, inv)
            }.collect { (owned, team, inv) ->
                val current = _uiState.value
                if (current is LeagueUiState.Prep && current.opponentIndex == opponentIndex) {
                    _uiState.value = current.copy(owned = owned, team = team,
                                                  catalog = catalog, inventory = inv)
                }
            }
        }
    }

    private suspend fun toggleCard(cardId: String) {
        val s = _uiState.value as? LeagueUiState.Prep ?: return
        val inTeam = s.team.any { it.id == cardId }
        updateTeam.execute(cardId, !inTeam, s.team)
    }

    private suspend fun equip(cardId: String, itemId: String) {
        val s = _uiState.value as? LeagueUiState.Prep ?: return
        val card = s.owned.firstOrNull { it.id == cardId } ?: return
        val item = s.catalog.firstOrNull { it.id == itemId } ?: return
        equipItem.execute(card, item)
    }

    private suspend fun unequip(cardId: String) {
        val s = _uiState.value as? LeagueUiState.Prep ?: return
        val card = s.owned.firstOrNull { it.id == cardId } ?: return
        unequipItem.execute(card)
    }

    private suspend fun cure(cardId: String) {
        val s = _uiState.value as? LeagueUiState.Prep ?: return
        val card = s.owned.firstOrNull { it.id == cardId } ?: return
        cureFatigue.execute(card)
    }

    private fun enterStrategy() {
        val s = _uiState.value as? LeagueUiState.Prep ?: return
        if (!s.canFight) return
        prepJob?.cancel()
        enterStrategy(s.league, s.opponentIndex, s.team)
    }

    private fun enterStrategy(league: League, opponentIndex: Int, team: List<Card>) {
        val opponent = league.opponents[opponentIndex]
        val arena = Gym(
            name = "${league.name} — ${opponent.name}", city = "Meseta Añil",
            latitude = 0.0, longitude = 0.0, typeSpecialty = opponent.specialty,
            cardPool = opponent.cardPool, difficulty = league.difficulty,
            region = league.region
        )
        _uiState.value = LeagueUiState.Strategy(league, opponentIndex, arena,
                                                team = team, botTeam = arena.drawBotTeam())
    }

    private fun moveCard(index: Int, up: Boolean) {
        val s = _uiState.value as? LeagueUiState.Strategy ?: return
        val reordered = s.team.swappedAdjacent(index, up) ?: return
        _uiState.value = s.copy(team = reordered)
    }

    private suspend fun fight() {
        val s = _uiState.value as? LeagueUiState.Strategy ?: return
        val (team, notice) = dropHeldItem.resolveDropFor(s.team)
        val result = simulate.execute(team, s.botTeam, WeatherCondition.CLEAR, s.arena,
                                      date = LocalDateTime.now().toString())
        runCatching { saveResult.execute(result) }
        if (result.playerWon) victories++
        _uiState.value = LeagueUiState.Combat(s.league, s.opponentIndex, result, notice)
    }

    private suspend fun advance() {
        val s = _uiState.value as? LeagueUiState.Combat ?: return
        when {
            !s.result.playerWon ->
                _uiState.value = LeagueUiState.Finished(s.league, won = false,
                                                        defeated = victories, reward = null)
            s.isLastOpponent -> {
                val reward = runCatching {
                    awardBadge.execute(s.result.copy(gymName = s.league.name))
                }.getOrNull()
                _uiState.value = LeagueUiState.Finished(s.league, won = true,
                                                        defeated = victories, reward = reward)
            }
            else -> enterPrep(s.league, s.opponentIndex + 1)
        }
    }

}
