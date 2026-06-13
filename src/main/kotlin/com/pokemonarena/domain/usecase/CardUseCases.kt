package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.CardPricing
import com.pokemonarena.domain.entity.CollectionRules
import com.pokemonarena.domain.repository.BattleRepository
import com.pokemonarena.domain.repository.CardRepository
import com.pokemonarena.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class GetCardsForPokemonUseCase(private val repo: CardRepository) {
    suspend fun execute(pokemonName: String) = repo.getCardsForPokemon(pokemonName)
}

class GetOwnedCardsUseCase(private val repo: CardRepository) {
    fun execute(): Flow<List<Card>> = repo.getOwnedCards()
}

class PurchaseCardUseCase(
    private val cardRepo:   CardRepository,
    private val battleRepo: BattleRepository
) {
    sealed interface Result {
        data class Success(val remainingCoins: Int, val pricePaid: Int) : Result
        data class InsufficientCoins(val currentCoins: Int, val price: Int) : Result
        data class CollectionFull(val maxCards: Int) : Result
    }

    suspend fun execute(card: Card): Result {
        if (cardRepo.getOwnedCards().first().size >= CollectionRules.MAX_OWNED_CARDS)
            return Result.CollectionFull(CollectionRules.MAX_OWNED_CARDS)
        val price = CardPricing.priceOf(card)
        val coins = battleRepo.getUserStatistics().first().coins
        if (coins < price) return Result.InsufficientCoins(coins, price)
        cardRepo.purchaseCard(card)
        return Result.Success(battleRepo.adjustCoins(-price), price)
    }
}

class SellCardUseCase(
    private val cardRepo:   CardRepository,
    private val battleRepo: BattleRepository,
    private val itemRepo:   ItemRepository
) {
    suspend fun execute(card: Card): Int {
        card.heldItem?.let { itemRepo.addToInventory(it.id) }
        val value = CardPricing.sellValueOf(card)
        cardRepo.removeCard(card.id)
        battleRepo.adjustCoins(value)
        return value
    }
}
