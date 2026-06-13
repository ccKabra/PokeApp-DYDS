package com.pokemonarena.presentation.screens.gyms

import com.pokemonarena.domain.entity.Gym
import com.pokemonarena.domain.entity.Region
import com.pokemonarena.domain.entity.WeatherCondition

data class GymWithWeather(
    val gym:     Gym,
    val weather: WeatherCondition? = null,
    val loading: Boolean           = true
)

data class GymsUiState(
    val region:         Region               = Region.KANTO,
    val regionUnlocked: Boolean              = true,
    val gyms:           List<GymWithWeather> = emptyList(),
    val teamSize:       Int                  = 0,
    val earnedBadges:   Set<String>          = emptySet(),
    val isLoading:      Boolean              = false,
    val error:          String?              = null
)
