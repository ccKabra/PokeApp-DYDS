package com.pokemonarena.presentation.screens.statistics

import com.pokemonarena.domain.entity.BattleResult
import com.pokemonarena.domain.entity.UserStatistics

data class StatisticsUiState(
    val stats:     UserStatistics     = UserStatistics(),
    val history:   List<BattleResult> = emptyList(),
    val isLoading: Boolean            = true,
    val error:     String?            = null
)
