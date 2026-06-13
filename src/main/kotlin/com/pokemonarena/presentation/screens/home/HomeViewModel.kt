package com.pokemonarena.presentation.screens.home

import com.pokemonarena.domain.entity.Progression
import com.pokemonarena.domain.usecase.GetBattleHistoryUseCase
import com.pokemonarena.domain.usecase.GetEarnedBadgesUseCase
import com.pokemonarena.domain.usecase.GetGymsUseCase
import com.pokemonarena.domain.usecase.GetLeaguesUseCase
import com.pokemonarena.domain.usecase.GetTeamUseCase
import com.pokemonarena.domain.usecase.GetUserStatisticsUseCase
import com.pokemonarena.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getStats:   GetUserStatisticsUseCase,
    private val getTeam:    GetTeamUseCase,
    private val getHistory: GetBattleHistoryUseCase,
    private val getGyms:    GetGymsUseCase,
    private val getLeagues: GetLeaguesUseCase,
    private val getBadges:  GetEarnedBadgesUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadDashboard() }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.Refresh -> loadDashboard()
        }
    }

    fun loadDashboard() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val gyms    = runCatching { getGyms.execute() }.getOrDefault(emptyList())
                val leagues = runCatching { getLeagues.execute() }.getOrDefault(emptyList())
                combine(getStats.execute(), getTeam.execute(), getHistory.execute(),
                        getBadges.execute()) { stats, team, history, badges ->
                    HomeUiState(
                        stats           = stats,
                        teamCards       = team,
                        recentBattles   = history.take(3),
                        gyms            = gyms,
                        leagues         = leagues,
                        earnedBadges    = badges,
                        unlockedRegions = Progression.unlockedRegions(gyms, leagues, badges),
                        isLoading       = false
                    )
                }.collect { state -> _uiState.update { state } }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
