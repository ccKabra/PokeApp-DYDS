package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.entity.CoinMine
import com.pokemonarena.domain.entity.MiningReward
import com.pokemonarena.domain.repository.BattleRepository
import kotlin.random.Random

class MineCoinsUseCase(
    private val repo:   BattleRepository,
    private val random: Random = Random.Default
) {
    suspend fun execute(): MiningReward {
        val reward = CoinMine.rewardFor(random.nextFloat())
        if (reward.coins > 0) repo.adjustCoins(reward.coins)
        return reward
    }
}
