package com.pokemonarena.presentation.screens.gyms

import com.pokemonarena.domain.entity.Gym
import com.pokemonarena.domain.entity.WeatherCondition
import com.pokemonarena.domain.entity.Region
import com.pokemonarena.domain.usecase.GetGymsUseCase
import com.pokemonarena.domain.usecase.GetRegionProgressUseCase
import com.pokemonarena.domain.usecase.GetTeamUseCase
import com.pokemonarena.domain.usecase.GetWeatherConditionUseCase
import com.pokemonarena.presentation.BaseViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GymsViewModel(
    private val getGyms:     GetGymsUseCase,
    private val getWeather:  GetWeatherConditionUseCase,
    private val getTeam:     GetTeamUseCase,
    private val getProgress: GetRegionProgressUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(GymsUiState())
    val uiState: StateFlow<GymsUiState> = _uiState.asStateFlow()

    fun onEvent(event: GymsUiEvent) {
        when (event) {
            is GymsUiEvent.Load -> load(event.region)
        }
    }

    fun load(region: Region) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val gyms     = getGyms.execute().filter { it.region == region }
                val teamSize = getTeam.execute().first().size
                val progress = getProgress.execute()
                _uiState.update { GymsUiState(region = region,
                                              regionUnlocked = region in progress.unlockedRegions,
                                              gyms = gyms.map { GymWithWeather(it) },
                                              teamSize = teamSize,
                                              earnedBadges = progress.earnedBadges) }
                gyms.mapIndexed { i, gym ->
                    async {
                        val cond = runCatching { getWeather.execute(gym.latitude, gym.longitude) }.getOrNull()
                        _uiState.update { cur ->
                            cur.copy(gyms = cur.gyms.mapIndexed { j, g ->
                                if (j == i) g.copy(weather = cond, loading = false) else g
                            })
                        }
                    }
                }.awaitAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
