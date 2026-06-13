package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.entity.RogueSpecies
import com.pokemonarena.domain.repository.BattleRepository
import com.pokemonarena.domain.repository.RoguePoolRepository

class GetRoguePoolUseCase(private val repo: RoguePoolRepository) {
    suspend fun execute(): List<RogueSpecies> = repo.getPool()
}

class CashOutRogueRunUseCase(private val repo: BattleRepository) {
    suspend fun execute(payout: Int): Int {
        if (payout > 0) repo.adjustCoins(payout)
        return payout
    }
}
