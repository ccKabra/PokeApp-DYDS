package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.Item
import com.pokemonarena.domain.entity.ItemCatalog
import com.pokemonarena.domain.repository.BattleRepository
import com.pokemonarena.domain.repository.CardRepository
import com.pokemonarena.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.random.Random

class GetItemCatalogUseCase(private val repo: ItemRepository) {
    suspend fun execute(): List<Item> = repo.getCatalog()
}

class GetItemInventoryUseCase(private val repo: ItemRepository) {
    fun execute(): Flow<Map<String, Int>> = repo.getInventory()
}

class PurchaseItemUseCase(
    private val itemRepo:   ItemRepository,
    private val battleRepo: BattleRepository
) {
    sealed interface Result {
        data class Success(val remainingCoins: Int) : Result
        data class InsufficientCoins(val currentCoins: Int, val price: Int) : Result
    }

    suspend fun execute(item: Item): Result {
        val coins = battleRepo.getUserStatistics().first().coins
        if (coins < item.price) return Result.InsufficientCoins(coins, item.price)
        itemRepo.addToInventory(item.id)
        return Result.Success(battleRepo.adjustCoins(-item.price))
    }
}

class CureFatigueUseCase(
    private val cardRepo: CardRepository,
    private val itemRepo: ItemRepository
) {
    suspend fun execute(card: Card): Boolean {
        val available = itemRepo.getInventory().first()[ItemCatalog.ENERGY_ROOT_ID] ?: 0
        if (available <= 0 || card.timesUsed == 0) return false
        itemRepo.removeFromInventory(ItemCatalog.ENERGY_ROOT_ID)
        cardRepo.resetBattleUsage(card.id)
        return true
    }
}

class DropHeldItemUseCase(
    private val cardRepo: CardRepository,
    private val random:   Random = Random.Default
) {
    suspend fun execute(team: List<Card>): Card? {
        val holders = team.filter { it.heldItem != null }
        if (holders.isEmpty() || random.nextFloat() >= DROP_CHANCE) return null
        val victim = holders.random(random)
        cardRepo.setHeldItem(victim.id, null)
        return victim
    }

    companion object {
        const val DROP_CHANCE = 0.45f
    }
}

class EquipItemUseCase(
    private val cardRepo: CardRepository,
    private val itemRepo: ItemRepository
) {
    suspend fun execute(card: Card, item: Item) {
        val available = itemRepo.getInventory().first()[item.id] ?: 0
        if (available <= 0) return
        card.heldItem?.let { itemRepo.addToInventory(it.id) }
        itemRepo.removeFromInventory(item.id)
        cardRepo.setHeldItem(card.id, item.id)
    }
}

class UnequipItemUseCase(
    private val cardRepo: CardRepository,
    private val itemRepo: ItemRepository
) {
    suspend fun execute(card: Card) {
        val held = card.heldItem ?: return
        itemRepo.addToInventory(held.id)
        cardRepo.setHeldItem(card.id, null)
    }
}
