package com.pokemonarena.presentation

import com.pokemonarena.TestFixtures
import com.pokemonarena.domain.usecase.GetBattleHistoryUseCase
import com.pokemonarena.domain.usecase.GetUserStatisticsUseCase
import com.pokemonarena.presentation.screens.statistics.StatisticsViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getStats   = mockk<GetUserStatisticsUseCase>()
    private val getHistory = mockk<GetBattleHistoryUseCase>()

    @BeforeTest
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `init_loadsStatsAndHistoryIntoUiState`() = runTest {
        every { getStats.execute()   } returns flowOf(TestFixtures.statsWithData)
        every { getHistory.execute() } returns flowOf(listOf(TestFixtures.playerWin))

        val viewModel = StatisticsViewModel(getStats, getHistory)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(TestFixtures.statsWithData, viewModel.uiState.value.stats)
        assertEquals(1, viewModel.uiState.value.history.size)
    }

    @Test
    fun `init_limitsHistoryToTen`() = runTest {
        every { getStats.execute()   } returns flowOf(TestFixtures.emptyStats)
        every { getHistory.execute() } returns flowOf((1..25).map { TestFixtures.playerWin })

        val viewModel = StatisticsViewModel(getStats, getHistory)

        assertTrue(viewModel.uiState.value.history.size <= 10)
    }
}
