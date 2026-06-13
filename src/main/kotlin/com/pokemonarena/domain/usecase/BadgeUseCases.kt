package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.entity.BattleResult
import com.pokemonarena.domain.entity.BattleRewards
import com.pokemonarena.domain.entity.Item
import com.pokemonarena.domain.entity.ItemCatalog
import com.pokemonarena.domain.repository.BadgeRepository
import com.pokemonarena.domain.repository.BattleRepository
import com.pokemonarena.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

class GetEarnedBadgesUseCase(private val repo: BadgeRepository) {
    fun execute(): Flow<Set<String>> = repo.getEarnedBadges()
}

data class FirstWinReward(val coins: Int, val item: Item)

class AwardBadgeIfFirstWinUseCase(
    private val badgeRepo:  BadgeRepository,
    private val battleRepo: BattleRepository,
    private val itemRepo:   ItemRepository,
    private val random:     Random = Random.Default
) {
    suspend fun execute(result: BattleResult): FirstWinReward? {
        if (!result.playerWon || result.gymName.isBlank()) return null
        if (badgeRepo.hasBadge(result.gymName)) return null
        badgeRepo.awardBadge(result.gymName)
        battleRepo.adjustCoins(BattleRewards.FIRST_WIN_BONUS)
        val item = ItemCatalog.EXCLUSIVES.random(random)
        itemRepo.addToInventory(item.id)
        return FirstWinReward(BattleRewards.FIRST_WIN_BONUS, item)
    }
}
