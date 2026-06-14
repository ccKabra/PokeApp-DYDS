package com.pokemonarena.domain.usecase

import com.pokemonarena.FixedRandom
import com.pokemonarena.TestFixtures
import com.pokemonarena.domain.entity.ItemCatalog
import com.pokemonarena.domain.entity.UserStatistics
import com.pokemonarena.domain.entity.WeatherCondition
import com.pokemonarena.domain.repository.BattleRepository
import com.pokemonarena.domain.repository.CardRepository
import com.pokemonarena.domain.repository.ItemRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ItemCatalogTest {

    @Test
    fun `boosts_increaseTheTargetedStats`() {
        val band    = ItemCatalog.byId("choice-band")!!
        val boosted = band.boosts.applyTo(TestFixtures.balancedStats)
        assertEquals(120, boosted.attack)
        assertEquals(80,  boosted.defense)
        assertEquals(80,  boosted.speed)
    }

    @Test
    fun `effectiveStats_reflectTheHeldItem`() {
        val card  = TestFixtures.fireCard
        val armed = card.copy(heldItem = ItemCatalog.byId("life-orb"))
        assertTrue(armed.effectiveStats.total > card.effectiveStats.total)
        assertEquals(card.stats, card.effectiveStats)
    }

    @Test
    fun `scoreOf_anItemHolderBeatsItsTwinWithoutItem`() {
        val simulate = SimulateBattleUseCase(FixedRandom(0.99f))
        val plain = TestFixtures.fireCard
        val armed = plain.copy(id = "armed", heldItem = ItemCatalog.byId("choice-band"))
        assertTrue(simulate.scoreOf(armed, WeatherCondition.CLEAR) >
                   simulate.scoreOf(plain, WeatherCondition.CLEAR))
    }

    @Test
    fun `catalog_everyEquippableImprovesStatsOrAccuracy`() {
        ItemCatalog.ALL.forEach { item ->
            val b = item.boosts
            val boostsSomething = listOf(b.attack, b.defense, b.specialAttack, b.specialDefense, b.speed)
                .any { it > 1f } || item.missReduction > 0f
            assertTrue(boostsSomething, "${item.name} debe mejorar al menos una stat o la precisión")
        }
    }
}

class PurchaseItemUseCaseTest {

    private val itemRepo   = mockk<ItemRepository>(relaxed = true)
    private val battleRepo = mockk<BattleRepository>(relaxed = true)
    private val useCase    = PurchaseItemUseCase(itemRepo, battleRepo)
    private val band       = ItemCatalog.byId("choice-band")!!

    @Test
    fun `execute_withEnoughCoins_addsToInventoryAndDeductsPrice`() = runTest {
        every { battleRepo.getUserStatistics() } returns flowOf(UserStatistics(coins = 500))

        val result = useCase.execute(band)

        assertIs<PurchaseItemUseCase.Result.Success>(result)
        assertEquals(50, result.remainingCoins)
        coVerify { itemRepo.addToInventory("choice-band") }
    }

    @Test
    fun `execute_withoutCoins_failsAndDoesNotAddItem`() = runTest {
        every { battleRepo.getUserStatistics() } returns flowOf(UserStatistics(coins = 100))

        val result = useCase.execute(band)

        assertIs<PurchaseItemUseCase.Result.InsufficientCoins>(result)
        assertEquals(450, result.price)
        coVerify(exactly = 0) { itemRepo.addToInventory(any()) }
    }
}

class EquipItemUseCasesTest {

    private val cardRepo = mockk<CardRepository>(relaxed = true)
    private val itemRepo = mockk<ItemRepository>(relaxed = true)
    private val equip    = EquipItemUseCase(cardRepo, itemRepo)
    private val unequip  = UnequipItemUseCase(cardRepo, itemRepo)
    private val band     = ItemCatalog.byId("choice-band")!!
    private val orb      = ItemCatalog.byId("life-orb")!!

    @Test
    fun `equip_consumesInventoryAndSetsTheItemOnTheCard`() = runTest {
        every { itemRepo.getInventory() } returns flowOf(mapOf("choice-band" to 1))

        equip.execute(TestFixtures.fireCard, band)

        coVerify { itemRepo.removeFromInventory("choice-band") }
        coVerify { cardRepo.setHeldItem(TestFixtures.fireCard.id, "choice-band") }
    }

    @Test
    fun `equip_withoutStock_doesNothing`() = runTest {
        every { itemRepo.getInventory() } returns flowOf(emptyMap())

        equip.execute(TestFixtures.fireCard, band)

        coVerify(exactly = 0) { cardRepo.setHeldItem(any(), any()) }
    }

    @Test
    fun `equip_overAnotherItem_returnsThePreviousOneToInventory`() = runTest {
        every { itemRepo.getInventory() } returns flowOf(mapOf("life-orb" to 1))
        val armed = TestFixtures.fireCard.copy(heldItem = band)

        equip.execute(armed, orb)

        coVerify { itemRepo.addToInventory("choice-band") }
        coVerify { itemRepo.removeFromInventory("life-orb") }
        coVerify { cardRepo.setHeldItem(armed.id, "life-orb") }
    }

    @Test
    fun `unequip_returnsTheItemToInventoryAndClearsTheCard`() = runTest {
        val armed = TestFixtures.fireCard.copy(heldItem = band)

        unequip.execute(armed)

        coVerify { itemRepo.addToInventory("choice-band") }
        coVerify { cardRepo.setHeldItem(armed.id, null) }
    }

    @Test
    fun `unequip_withoutItem_doesNothing`() = runTest {
        unequip.execute(TestFixtures.fireCard)
        coVerify(exactly = 0) { cardRepo.setHeldItem(any(), any()) }
    }
}
