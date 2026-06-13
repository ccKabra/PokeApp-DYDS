package com.pokemonarena.presentation.screens.battle

import com.pokemonarena.domain.entity.BattleResult
import com.pokemonarena.domain.entity.Region
import com.pokemonarena.domain.entity.WeatherCondition
import com.pokemonarena.domain.usecase.AwardBadgeIfFirstWinUseCase
import com.pokemonarena.domain.usecase.DropHeldItemUseCase
import com.pokemonarena.domain.usecase.FirstWinReward
import com.pokemonarena.domain.usecase.GetGymByNameUseCase
import com.pokemonarena.domain.usecase.GetTeamUseCase
import com.pokemonarena.domain.usecase.GetWeatherConditionUseCase
import com.pokemonarena.domain.usecase.SaveBattleResultUseCase
import com.pokemonarena.domain.usecase.SimulateBattleUseCase
import com.pokemonarena.presentation.BaseViewModel
import com.pokemonarena.presentation.utils.resolveDropFor
import com.pokemonarena.presentation.utils.swappedAdjacent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class BattleViewModel(
    private val getGymByName: GetGymByNameUseCase,
    private val getTeam:      GetTeamUseCase,
    private val getWeather:   GetWeatherConditionUseCase,
    private val simulate:     SimulateBattleUseCase,
    private val saveResult:   SaveBattleResultUseCase,
    private val dropHeldItem: DropHeldItemUseCase,
    private val awardBadge:   AwardBadgeIfFirstWinUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<BattleUiState>(BattleUiState.Loading)
    val uiState: StateFlow<BattleUiState> = _uiState.asStateFlow()

    private val _lastResult = MutableStateFlow<BattleResult?>(null)
    val lastResult: StateFlow<BattleResult?> = _lastResult.asStateFlow()

    private val _firstWinReward = MutableStateFlow<FirstWinReward?>(null)
    val firstWinReward: StateFlow<FirstWinReward?> = _firstWinReward.asStateFlow()

    private val _lastRegion = MutableStateFlow(Region.KANTO)
    val lastRegion: StateFlow<Region> = _lastRegion.asStateFlow()

    private val _navigateToResult = MutableSharedFlow<Unit>()
    val navigateToResult: SharedFlow<Unit> = _navigateToResult.asSharedFlow()

    fun onEvent(event: BattleUiEvent) {
        when (event) {
            is BattleUiEvent.Load             -> scope.launch { load(event.gymName) }
            is BattleUiEvent.MoveCard         -> moveCard(event.index, event.up)
            is BattleUiEvent.Fight            -> scope.launch { battle() }
            is BattleUiEvent.ContinueToResult -> scope.launch { _navigateToResult.emit(Unit) }
        }
    }

    private suspend fun load(gymName: String) {
        _uiState.value = BattleUiState.Loading
        val gym = runCatching { getGymByName.execute(gymName) }.getOrNull() ?: run {
            _uiState.value = BattleUiState.Error("No se encontró el gimnasio \"$gymName\""); return
        }
        val team = runCatching { getTeam.execute().first() }.getOrDefault(emptyList())
        if (team.size < 3) {
            _uiState.value = BattleUiState.Error("Necesitás 3 cartas en tu equipo.\nConfiguralo en la sección Mi Equipo.")
            return
        }
        val weather = runCatching { getWeather.execute(gym.latitude, gym.longitude) }
            .getOrDefault(WeatherCondition.CLEAR)
        _lastRegion.value = gym.region
        _uiState.value = BattleUiState.Ready(gym, team, gym.drawBotTeam(), weather)
    }

    private fun moveCard(index: Int, up: Boolean) {
        val s = _uiState.value as? BattleUiState.Ready ?: return
        val reordered = s.teamCards.swappedAdjacent(index, up) ?: return
        _uiState.value = s.copy(teamCards = reordered)
    }

    private suspend fun battle() {
        val s = _uiState.value as? BattleUiState.Ready ?: return
        val (team, notice) = dropHeldItem.resolveDropFor(s.teamCards)
        val result = simulate.execute(team, s.botCards, s.weather, s.gym,
                                      date = LocalDateTime.now().toString())
        _lastResult.value = result
        runCatching { saveResult.execute(result) }
        _firstWinReward.value = runCatching { awardBadge.execute(result) }.getOrNull()
        _uiState.value = BattleUiState.Combat(s.gym, s.weather, result, notice)
    }
}
