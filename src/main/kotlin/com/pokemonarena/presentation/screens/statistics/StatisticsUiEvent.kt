package com.pokemonarena.presentation.screens.statistics

sealed interface StatisticsUiEvent {
    object Refresh : StatisticsUiEvent
}
