package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.entity.AimGame
import com.pokemonarena.domain.entity.UserStatistics
import com.pokemonarena.domain.repository.BattleRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegisterAimShotUseCaseTest {

    private val repo    = mockk<BattleRepository>(relaxed = true)
    private val useCase = RegisterAimShotUseCase(repo)

    @Test
    fun `registerHit_smallBalloonPaysMoreThanBigOne`() = runTest {
        every { repo.getUserStatistics() } returns flowOf(UserStatistics(coins = 100))

        val small = useCase.registerHit(sizeFraction = 0f)
        val big   = useCase.registerHit(sizeFraction = 1f)

        assertTrue(small > big)
        assertEquals(AimGame.MAX_HIT_REWARD, small)
        assertEquals(AimGame.MIN_HIT_REWARD, big)
    }

    @Test
    fun `registerHit_creditsTheRewardToTheUser`() = runTest {
        every { repo.getUserStatistics() } returns flowOf(UserStatistics(coins = 100))

        val reward = useCase.registerHit(sizeFraction = 0.5f)

        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertEquals(100 + reward, slot.captured.coins)
    }

    @Test
    fun `registerMiss_subtractsThePenalty`() = runTest {
        every { repo.getUserStatistics() } returns flowOf(UserStatistics(coins = 100))

        val delta = useCase.registerMiss()

        assertEquals(-AimGame.MISS_PENALTY, delta)
        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertEquals(100 - AimGame.MISS_PENALTY, slot.captured.coins)
    }

    @Test
    fun `registerMiss_neverLeavesCoinsBelowZero`() = runTest {
        every { repo.getUserStatistics() } returns flowOf(UserStatistics(coins = 1))

        useCase.registerMiss()

        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertEquals(0, slot.captured.coins)
    }
}
