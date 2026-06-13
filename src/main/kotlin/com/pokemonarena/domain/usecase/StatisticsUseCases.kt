package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.entity.UserStatistics
import com.pokemonarena.domain.repository.BattleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetBattleHistoryUseCase(private val repo: BattleRepository) {
    fun execute() = repo.getBattleHistory()
}

class GetUserStatisticsUseCase(private val repo: BattleRepository) {
    fun execute(): Flow<UserStatistics> = repo.getUserStatistics()
}

class GetUserCoinsUseCase(private val repo: BattleRepository) {
    fun execute() = repo.getUserStatistics().map { it.coins }
}
