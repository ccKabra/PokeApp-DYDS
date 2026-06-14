package com.pokemonarena.domain.usecase

import com.pokemonarena.TestFixtures
import com.pokemonarena.domain.entity.CardPricing
import com.pokemonarena.domain.entity.CollectionRules
import com.pokemonarena.domain.entity.ItemCatalog
import com.pokemonarena.domain.entity.UserStatistics
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

class PurchaseCardUseCaseTest {

    private val cardRepo   = mockk<CardRepository>(relaxed = true) {
        every { getOwnedCards() } returns flowOf(emptyList())
    }
    private val battleRepo = mockk<BattleRepository>(relaxed = true)
    private val useCase    = PurchaseCardUseCase(cardRepo, battleRepo)

    private val firePrice = CardPricing.priceOf(TestFixtures.fireCard)

    @Test
    fun `execute_withFullCollection_returnsCollectionFullAndDoesNotPurchase`() = runTest {
        every { cardRepo.getOwnedCards() } returns
            flowOf((1..CollectionRules.MAX_OWNED_CARDS).map { TestFixtures.fireCard.copy(id = "c$it") })
        every { battleRepo.getUserStatistics() } returns flowOf(UserStatistics(coins = 99_999))

        val result = useCase.execute(TestFixtures.fireCard)

        assertIs<PurchaseCardUseCase.Result.CollectionFull>(result)
        assertEquals(CollectionRules.MAX_OWNED_CARDS, result.maxCards)
        coVerify(exactly = 0) { cardRepo.purchaseCard(any()) }
    }

    @Test
    fun `execute_withSufficientCoins_purchasesCardAndDeductsDynamicPrice`() = runTest {
        every { battleRepo.getUserStatistics() } returns flowOf(UserStatistics(coins = firePrice + 100))

        val result = useCase.execute(TestFixtures.fireCard)

        assertIs<PurchaseCardUseCase.Result.Success>(result)
        assertEquals(100, result.remainingCoins)
        assertEquals(firePrice, result.pricePaid)
        coVerify { cardRepo.purchaseCard(TestFixtures.fireCard) }
        coVerify { battleRepo.saveUserStatistics(any()) }
    }

    @Test
    fun `execute_withExactCoins_succeedsAndLeavesZero`() = runTest {
        every { battleRepo.getUserStatistics() } returns flowOf(UserStatistics(coins = firePrice))

        val result = useCase.execute(TestFixtures.fireCard)

        assertIs<PurchaseCardUseCase.Result.Success>(result)
        assertEquals(0, result.remainingCoins)
    }

    @Test
    fun `execute_withInsufficientCoins_returnsPriceAndCurrentCoins`() = runTest {
        every { battleRepo.getUserStatistics() } returns flowOf(UserStatistics(coins = firePrice - 1))

        val result = useCase.execute(TestFixtures.fireCard)

        assertIs<PurchaseCardUseCase.Result.InsufficientCoins>(result)
        assertEquals(firePrice - 1, result.currentCoins)
        assertEquals(firePrice, result.price)
        coVerify(exactly = 0) { cardRepo.purchaseCard(any()) }
    }

    @Test
    fun `execute_deductsExactlyTheDynamicPrice`() = runTest {
        val initialCoins = firePrice + 235
        every { battleRepo.getUserStatistics() } returns flowOf(UserStatistics(coins = initialCoins))

        useCase.execute(TestFixtures.fireCard)

        val slot = slot<UserStatistics>()
        coVerify { battleRepo.saveUserStatistics(capture(slot)) }
        assertEquals(initialCoins - firePrice, slot.captured.coins)
    }
}

class SellCardUseCaseTest {

    private val cardRepo   = mockk<CardRepository>(relaxed = true)
    private val battleRepo = mockk<BattleRepository>(relaxed = true)
    private val itemRepo   = mockk<ItemRepository>(relaxed = true)
    private val useCase    = SellCardUseCase(cardRepo, battleRepo, itemRepo)

    @Test
    fun `execute_removesCardAndCreditsSellValue`() = runTest {
        every { battleRepo.getUserStatistics() } returns flowOf(UserStatistics(coins = 100))
        val expectedValue = CardPricing.sellValueOf(TestFixtures.fireCard)

        val value = useCase.execute(TestFixtures.fireCard)

        assertEquals(expectedValue, value)
        coVerify { cardRepo.removeCard(TestFixtures.fireCard.id) }
        val slot = slot<UserStatistics>()
        coVerify { battleRepo.saveUserStatistics(capture(slot)) }
        assertEquals(100 + expectedValue, slot.captured.coins)
    }

    @Test
    fun `execute_sellValueIsLessThanPurchasePrice`() = runTest {
        every { battleRepo.getUserStatistics() } returns flowOf(UserStatistics(coins = 0))
        val value = useCase.execute(TestFixtures.strongCard)
        assertTrue(value < CardPricing.priceOf(TestFixtures.strongCard))
    }

    @Test
    fun `execute_sellingACardWithItem_returnsTheItemToInventory`() = runTest {
        every { battleRepo.getUserStatistics() } returns flowOf(UserStatistics(coins = 0))
        val armed = TestFixtures.fireCard.copy(heldItem = ItemCatalog.byId("choice-band"))

        useCase.execute(armed)

        coVerify { itemRepo.addToInventory("choice-band") }
    }
}

class UpdateTeamUseCaseTest {

    private val repo    = mockk<CardRepository>(relaxed = true)
    private val useCase = UpdateTeamUseCase(repo)

    @Test
    fun `execute_withEmptyTeam_addsCardSuccessfully`() = runTest {
        val result = useCase.execute(TestFixtures.fireCard.id, inTeam = true, currentTeam = emptyList())
        assertTrue(result)
        coVerify { repo.updateTeamMembership(TestFixtures.fireCard.id, true) }
    }

    @Test
    fun `execute_withTwoCardsInTeam_addsThirdSuccessfully`() = runTest {
        val team = listOf(TestFixtures.fireCard, TestFixtures.waterCard)
        val result = useCase.execute(TestFixtures.grassCard.id, inTeam = true, currentTeam = team)
        assertTrue(result)
    }

    @Test
    fun `execute_withFullTeam_doesNotAddFourthCard`() = runTest {
        val fullTeam = listOf(TestFixtures.fireCard, TestFixtures.waterCard, TestFixtures.grassCard)
        val result = useCase.execute("card4", inTeam = true, currentTeam = fullTeam)
        assertTrue(!result)
        coVerify(exactly = 0) { repo.updateTeamMembership(any(), any()) }
    }

    @Test
    fun `execute_removingCard_alwaysSucceeds`() = runTest {
        val fullTeam = listOf(TestFixtures.fireCard, TestFixtures.waterCard, TestFixtures.grassCard)

        val result = useCase.execute(TestFixtures.fireCard.id, inTeam = false, currentTeam = fullTeam)
        assertTrue(result)
        coVerify { repo.updateTeamMembership(TestFixtures.fireCard.id, false) }
    }
}

class CoinRewardsTest {

    private val repo    = mockk<BattleRepository>(relaxed = true)
    private val useCase = UpdateStatisticsAfterBattleUseCase(repo)

    @Test
    fun `execute_whenPlayerWins_addsTheResultCoinsDelta`() = runTest {
        every { repo.getUserStatistics() } returns flowOf(UserStatistics(coins = 100))
        every { repo.getBattleHistory()  } returns flowOf(listOf(TestFixtures.playerWin))

        useCase.execute(TestFixtures.playerWin)

        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertEquals(145, slot.captured.coins)
    }

    @Test
    fun `execute_whenBotWins_subtractsTheResultCoinsDelta`() = runTest {
        every { repo.getUserStatistics() } returns flowOf(UserStatistics(coins = 100))
        every { repo.getBattleHistory()  } returns flowOf(listOf(TestFixtures.botWin))

        useCase.execute(TestFixtures.botWin)

        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertEquals(90, slot.captured.coins)
    }

    @Test
    fun `execute_whenDrawWithZeroDelta_doesNotChangeCoins`() = runTest {
        every { repo.getUserStatistics() } returns flowOf(UserStatistics(coins = 200))
        every { repo.getBattleHistory()  } returns flowOf(listOf(TestFixtures.draw))

        useCase.execute(TestFixtures.draw)

        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertEquals(200, slot.captured.coins)
    }

    @Test
    fun `execute_whenCoinsWouldGoBelowZero_clampsToZero`() = runTest {
        every { repo.getUserStatistics() } returns flowOf(UserStatistics(coins = 5))
        every { repo.getBattleHistory()  } returns flowOf(listOf(TestFixtures.botWin))

        useCase.execute(TestFixtures.botWin)

        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertEquals(0, slot.captured.coins)
    }
}
