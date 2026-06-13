package com.pokemonarena.presentation.screens.home

import com.pokemonarena.domain.entity.BattleResult
import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.Gym
import com.pokemonarena.domain.entity.League
import com.pokemonarena.domain.entity.Region
import com.pokemonarena.domain.entity.UserStatistics

data class HomeUiState(
    val stats:           UserStatistics     = UserStatistics(),
    val teamCards:       List<Card>         = emptyList(),
    val recentBattles:   List<BattleResult> = emptyList(),
    val gyms:            List<Gym>          = emptyList(),
    val leagues:         List<League>       = emptyList(),
    val earnedBadges:    Set<String>        = emptySet(),
    val unlockedRegions: Set<Region>        = setOf(Region.KANTO),
    val isLoading:       Boolean            = false,
    val error:           String?            = null
) {
    val totalBadges: Int get() = gyms.size + leagues.size
    val earnedCount: Int get() = gyms.count { it.name in earnedBadges } +
                                 leagues.count { it.name in earnedBadges }
}
