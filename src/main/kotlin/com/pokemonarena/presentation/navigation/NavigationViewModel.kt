package com.pokemonarena.presentation.navigation

import com.pokemonarena.domain.entity.Economy
import com.pokemonarena.domain.entity.Region
import com.pokemonarena.domain.usecase.GetEarnedBadgesUseCase
import com.pokemonarena.domain.usecase.GetRegionProgressUseCase
import com.pokemonarena.domain.usecase.GetUserCoinsUseCase
import com.pokemonarena.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NavigationViewModel(
    getUserCoins: GetUserCoinsUseCase,
    getBadges:    GetEarnedBadgesUseCase,
    getProgress:  GetRegionProgressUseCase
) : BaseViewModel() {

    private val _coins = MutableStateFlow(Economy.STARTING_COINS)
    val coins: StateFlow<Int> = _coins.asStateFlow()

    private val _unlockedRegions = MutableStateFlow(setOf(Region.KANTO))
    val unlockedRegions: StateFlow<Set<Region>> = _unlockedRegions.asStateFlow()

    init {
        scope.launch { getUserCoins.execute().collect { _coins.value = it } }
        scope.launch {
            getBadges.execute().collect {
                _unlockedRegions.value = runCatching { getProgress.execute().unlockedRegions }
                    .getOrDefault(setOf(Region.KANTO))
            }
        }
    }
}
