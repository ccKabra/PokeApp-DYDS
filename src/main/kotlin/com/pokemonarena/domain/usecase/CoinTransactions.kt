package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.repository.BattleRepository
import kotlinx.coroutines.flow.first

internal suspend fun BattleRepository.adjustCoins(delta: Int): Int {
    val stats   = getUserStatistics().first()
    val balance = (stats.coins + delta).coerceAtLeast(0)
    saveUserStatistics(stats.copy(coins = balance))
    return balance
}
