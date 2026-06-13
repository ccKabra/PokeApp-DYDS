package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.entity.AimGame
import com.pokemonarena.domain.repository.BattleRepository

class RegisterAimShotUseCase(private val repo: BattleRepository) {

    suspend fun registerHit(sizeFraction: Float): Int {
        val reward = AimGame.hitRewardFor(sizeFraction)
        repo.adjustCoins(reward)
        return reward
    }

    suspend fun registerMiss(): Int {
        repo.adjustCoins(-AimGame.MISS_PENALTY)
        return -AimGame.MISS_PENALTY
    }
}
