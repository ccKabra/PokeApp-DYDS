package com.pokemonarena.domain.usecase

import com.pokemonarena.TestFixtures
import com.pokemonarena.domain.entity.*
import com.pokemonarena.domain.repository.BattleRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateStatisticsAfterBattleUseCaseTest {

    private val repo    = mockk<BattleRepository>(relaxed = true)
    private val useCase = UpdateStatisticsAfterBattleUseCase(repo)

    @Test
    fun `invoke_whenPlayerWins_incrementsTotalBattlesAndWins`() = runTest {
        every { repo.getUserStatistics() } returns flowOf(TestFixtures.emptyStats)
        every { repo.getBattleHistory()  } returns flowOf(listOf(TestFixtures.playerWin))

        useCase.execute(TestFixtures.playerWin)

        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertEquals(1, slot.captured.totalBattles)
        assertEquals(1, slot.captured.totalWins)
        assertEquals(0, slot.captured.totalLosses)
    }

    @Test
    fun `invoke_whenBotWins_incrementsTotalLosses`() = runTest {
        every { repo.getUserStatistics() } returns flowOf(TestFixtures.emptyStats)
        every { repo.getBattleHistory()  } returns flowOf(listOf(TestFixtures.botWin))

        useCase.execute(TestFixtures.botWin)

        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertEquals(0, slot.captured.totalWins)
        assertEquals(1, slot.captured.totalLosses)
    }

    @Test
    fun `invoke_whenDraw_doesNotIncrementWinsOrLosses`() = runTest {
        every { repo.getUserStatistics() } returns flowOf(TestFixtures.emptyStats)
        every { repo.getBattleHistory()  } returns flowOf(listOf(TestFixtures.draw))

        useCase.execute(TestFixtures.draw)

        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertEquals(1, slot.captured.totalBattles)
        assertEquals(0, slot.captured.totalWins)
        assertEquals(0, slot.captured.totalLosses)
    }

    @Test
    fun `invoke_withThreeConsecutivePlayerWins_setsStreakToThree`() = runTest {
        val history = listOf(
            TestFixtures.playerWin.copy(date = "2024-01-03"),
            TestFixtures.playerWin.copy(date = "2024-01-02"),
            TestFixtures.playerWin.copy(date = "2024-01-01")
        )
        every { repo.getUserStatistics() } returns flowOf(TestFixtures.emptyStats)
        every { repo.getBattleHistory()  } returns flowOf(history)

        useCase.execute(TestFixtures.playerWin)

        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertEquals(3, slot.captured.currentStreak)
    }

    @Test
    fun `invoke_whenLossBreaksStreak_resetsCurrentStreakToZero`() = runTest {
        val history = listOf(
            TestFixtures.botWin.copy(date    = "2024-01-03"),
            TestFixtures.playerWin.copy(date = "2024-01-02"),
            TestFixtures.playerWin.copy(date = "2024-01-01")
        )
        every { repo.getUserStatistics() } returns flowOf(TestFixtures.emptyStats)
        every { repo.getBattleHistory()  } returns flowOf(history)

        useCase.execute(TestFixtures.botWin)

        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertEquals(0, slot.captured.currentStreak)
    }

    @Test
    fun `invoke_whenNewStreakExceedsBest_updatesBestStreak`() = runTest {
        val existing = TestFixtures.emptyStats.copy(bestStreak = 2)
        val history  = (1..5).map { TestFixtures.playerWin.copy(date = "2024-01-0$it") }
        every { repo.getUserStatistics() } returns flowOf(existing)
        every { repo.getBattleHistory()  } returns flowOf(history)

        useCase.execute(TestFixtures.playerWin)

        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertEquals(5, slot.captured.bestStreak)
    }

    @Test
    fun `invoke_withEmptyHistory_setsFavoritePokemonToNull`() = runTest {
        every { repo.getUserStatistics() } returns flowOf(TestFixtures.emptyStats)
        every { repo.getBattleHistory()  } returns flowOf(emptyList())

        useCase.execute(TestFixtures.playerWin)

        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertNull(slot.captured.favoritePokemon)
    }

    @Test
    fun `invoke_setsCorrectFavoritePokemon`() = runTest {
        val history = listOf(
            TestFixtures.battleResult(playerCard = TestFixtures.fireCard),
            TestFixtures.battleResult(playerCard = TestFixtures.fireCard),
            TestFixtures.battleResult(playerCard = TestFixtures.waterCard)
        )
        every { repo.getUserStatistics() } returns flowOf(TestFixtures.emptyStats)
        every { repo.getBattleHistory()  } returns flowOf(history)

        useCase.execute(TestFixtures.playerWin)

        val slot = slot<UserStatistics>()
        coVerify { repo.saveUserStatistics(capture(slot)) }
        assertEquals("charmander", slot.captured.favoritePokemon)
    }
}
