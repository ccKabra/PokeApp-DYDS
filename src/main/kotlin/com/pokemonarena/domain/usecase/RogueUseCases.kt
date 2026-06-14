package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.entity.RogueLives
import com.pokemonarena.domain.entity.RogueMetaState
import com.pokemonarena.domain.entity.RogueSpecies
import com.pokemonarena.domain.entity.RogueUpgrade
import com.pokemonarena.domain.repository.BattleRepository
import com.pokemonarena.domain.repository.RogueMetaRepository
import com.pokemonarena.domain.repository.RoguePoolRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class GetRoguePoolUseCase(private val repo: RoguePoolRepository) {
    suspend fun execute(): List<RogueSpecies> = repo.getPool()
}

class CashOutRogueRunUseCase(private val repo: BattleRepository) {
    suspend fun execute(payout: Int): Int {
        if (payout > 0) repo.adjustCoins(payout)
        return payout
    }
}

class GetRogueMetaUseCase(private val repo: RogueMetaRepository) {
    fun execute(): Flow<RogueMetaState> = repo.getUpgrades()
}

class GetRogueLivesUseCase(private val repo: RogueMetaRepository) {
    suspend fun execute(now: Long): RogueLives = repo.getLives().regenerated(now)
}

class ConsumeRogueLifeUseCase(private val repo: RogueMetaRepository) {
    suspend fun execute(now: Long): RogueLives? {
        val next = repo.getLives().consume(now) ?: return null
        repo.saveLives(next)
        return next
    }
}

class PurchaseRogueUpgradeUseCase(
    private val metaRepo:   RogueMetaRepository,
    private val battleRepo: BattleRepository
) {
    sealed interface Result {
        data class Success(val remainingCoins: Int, val newLevel: Int) : Result
        data class InsufficientCoins(val currentCoins: Int, val cost: Int) : Result
        object MaxedOut : Result
    }

    suspend fun execute(upgrade: RogueUpgrade): Result {
        val level = metaRepo.getUpgrades().first().levelOf(upgrade)
        if (level >= upgrade.maxLevel) return Result.MaxedOut
        val cost  = upgrade.costAt(level)
        val coins = battleRepo.getUserStatistics().first().coins
        if (coins < cost) return Result.InsufficientCoins(coins, cost)
        val remaining = battleRepo.adjustCoins(-cost)
        metaRepo.setLevel(upgrade.id, level + 1)
        return Result.Success(remaining, level + 1)
    }
}
