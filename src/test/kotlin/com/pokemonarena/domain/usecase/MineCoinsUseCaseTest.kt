package com.pokemonarena.domain.usecase

import com.pokemonarena.FixedRandom
import com.pokemonarena.domain.entity.CoinMine
import com.pokemonarena.domain.entity.MiningTier
import com.pokemonarena.domain.entity.UserStatistics
import com.pokemonarena.domain.repository.BattleRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoinMineTest {

    @Test
    fun `rewardFor_mapsRollsToTiers`() {
        assertEquals(MiningTier.JACKPOT, CoinMine.rewardFor(0.004f).tier)
        assertEquals(MiningTier.EPIC,    CoinMine.rewardFor(0.02f).tier)
        assertEquals(MiningTier.GREAT,   CoinMine.rewardFor(0.08f).tier)
        assertEquals(MiningTier.NICE,    CoinMine.rewardFor(0.20f).tier)
        assertEquals(MiningTier.COMMON,  CoinMine.rewardFor(0.40f).tier)
        assertEquals(MiningTier.NOTHING, CoinMine.rewardFor(0.80f).tier)
    }

    @Test
    fun `rewardFor_betterTiersPayMore`() {
        assertTrue(CoinMine.rewardFor(0.004f).coins > CoinMine.rewardFor(0.02f).coins)
        assertTrue(CoinMine.rewardFor(0.02f).coins  > CoinMine.rewardFor(0.08f).coins)
        assertTrue(CoinMine.rewardFor(0.08f).coins  > CoinMine.rewardFor(0.20f).coins)
        assertTrue(CoinMine.rewardFor(0.20f).coins  > CoinMine.rewardFor(0.40f).coins)
        assertTrue(CoinMine.rewardFor(0.40f).coins  > CoinMine.rewardFor(0.80f).coins)
    }

    @Test
    fun `rewardFor_fiftyFivePercentOfRollsPayNothing`() {
        assertEquals(0, CoinMine.rewardFor(0.45f).coins)
        assertEquals(0, CoinMine.rewardFor(0.80f).coins)
        assertEquals(0, CoinMine.rewardFor(0.999f).coins)
    }

    @Test
    fun `rewardFor_jackpotIsBelowOnePercent`() {
        assertEquals(MiningTier.JACKPOT, CoinMine.rewardFor(0.0049f).tier)
        assertTrue(CoinMine.rewardFor(0.006f).tier != MiningTier.JACKPOT)
    }
}

class MineCoinsUseCaseTest {

    private val repo = mockk<BattleRepository>(relaxed = true)

    @Test
    fun `execute_creditsTheRewardToTheUserCoins`() = runTest {
        every { repo.getUserStatistics() } returns flowOf(UserStatistics(coins = 10))
        val useCase = MineCoinsUseCase(repo, FixedRandom(0.40f))

        val reward = useCase.execute()

        assertEquals(1, reward.coins)
        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertEquals(11, slot.captured.coins)
    }

    @Test
    fun `execute_withLuckyRoll_paysTheJackpot`() = runTest {
        every { repo.getUserStatistics() } returns flowOf(UserStatistics(coins = 0))
        val useCase = MineCoinsUseCase(repo, FixedRandom(0.0f))

        val reward = useCase.execute()

        assertEquals(MiningTier.JACKPOT, reward.tier)
        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertEquals(100, slot.captured.coins)
    }

    @Test
    fun `execute_withEmptyStrike_doesNotTouchTheCoins`() = runTest {
        val useCase = MineCoinsUseCase(repo, FixedRandom(0.80f))

        val reward = useCase.execute()

        assertEquals(0, reward.coins)
        assertEquals(MiningTier.NOTHING, reward.tier)
        coVerify(exactly = 0) { repo.saveUserStatistics(any()) }
    }

    @Test
    fun `execute_rescuesABankruptPlayer`() = runTest {
        every { repo.getUserStatistics() } returns flowOf(UserStatistics(coins = 0))
        val useCase = MineCoinsUseCase(repo, FixedRandom(0.40f))

        useCase.execute()

        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertTrue(slot.captured.coins > 0)
    }
}
