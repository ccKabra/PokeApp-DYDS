package com.pokemonarena.presentation.screens.statistics

import com.pokemonarena.domain.usecase.GetBattleHistoryUseCase
import com.pokemonarena.domain.usecase.GetUserStatisticsUseCase
import com.pokemonarena.presentation.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class StatisticsViewModel(
    private val getStats:   GetUserStatisticsUseCase,
    private val getHistory: GetBattleHistoryUseCase
) : BaseViewModel() {

    private var observeJob: Job? = null

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init { loadStatistics() }

    fun onEvent(event: StatisticsUiEvent) {
        when (event) {
            is StatisticsUiEvent.Refresh -> loadStatistics()
        }
    }

    private fun loadStatistics() {
        observeJob?.cancel()
        observeJob = scope.launch {
            combine(getStats.execute(), getHistory.execute()) { stats, history ->
                StatisticsUiState(stats = stats, history = history.take(10), isLoading = false)
            }.collect { _uiState.value = it }
        }
    }
}
