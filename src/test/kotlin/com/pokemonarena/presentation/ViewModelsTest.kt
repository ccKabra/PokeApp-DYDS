package com.pokemonarena.presentation

import com.pokemonarena.TestFixtures
import com.pokemonarena.domain.usecase.GetBattleHistoryUseCase
import com.pokemonarena.domain.usecase.GetEarnedBadgesUseCase
import com.pokemonarena.domain.usecase.GetGymsUseCase
import com.pokemonarena.domain.usecase.GetLeaguesUseCase
import com.pokemonarena.domain.usecase.GetTeamUseCase
import com.pokemonarena.domain.usecase.GetUserStatisticsUseCase
import com.pokemonarena.presentation.screens.home.HomeUiEvent
import com.pokemonarena.presentation.screens.home.HomeViewModel
import io.mockk.coEvery
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var getStats:   GetUserStatisticsUseCase
    private lateinit var getTeam:    GetTeamUseCase
    private lateinit var getHistory: GetBattleHistoryUseCase
    private lateinit var getGyms:    GetGymsUseCase
    private lateinit var getLeagues: GetLeaguesUseCase
    private lateinit var getBadges:  GetEarnedBadgesUseCase
    private lateinit var viewModel:  HomeViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getStats   = mockk()
        getTeam    = mockk()
        getHistory = mockk()
        getGyms    = mockk()
        getLeagues = mockk()
        getBadges  = mockk()
        every   { getStats.execute() } returns flowOf(TestFixtures.emptyStats)
        every   { getTeam.execute() } returns flowOf(emptyList())
        every   { getHistory.execute() } returns flowOf(emptyList())
        coEvery { getGyms.execute() } returns listOf(TestFixtures.gym())
        coEvery { getLeagues.execute() } returns emptyList()
        every   { getBadges.execute() } returns flowOf(emptySet())
        viewModel = vm()
    }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = HomeViewModel(getStats, getTeam, getHistory, getGyms, getLeagues, getBadges)

    @Test
    fun `init_loadsDataIntoUiState`() = runTest {
        every { getStats.execute() } returns flowOf(TestFixtures.statsWithData)
        every { getTeam.execute() } returns flowOf(listOf(TestFixtures.fireCard))
        every { getHistory.execute() } returns flowOf(listOf(TestFixtures.playerWin))
        viewModel = vm()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(TestFixtures.statsWithData, viewModel.uiState.value.stats)
    }

    @Test
    fun `init_withEmptyData_showsDefaultState`() = runTest {
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.teamCards.isEmpty())
        assertTrue(viewModel.uiState.value.recentBattles.isEmpty())
        assertEquals(0, viewModel.uiState.value.earnedCount)
    }

    @Test
    fun `init_limitsRecentBattlesToThree`() = runTest {
        every { getHistory.execute() } returns flowOf((1..10).map { TestFixtures.playerWin })
        viewModel = vm()

        assertTrue(viewModel.uiState.value.recentBattles.size <= 3)
    }

    @Test
    fun `init_loadsEarnedBadges`() = runTest {
        every { getBadges.execute() } returns flowOf(setOf(TestFixtures.gym().name))
        viewModel = vm()

        assertEquals(1, viewModel.uiState.value.earnedCount)
    }

    @Test
    fun `earnedCount_ignoresBadgesOfUnknownGyms`() = runTest {
        every { getBadges.execute() } returns flowOf(setOf("Gimnasio Fantasma"))
        viewModel = vm()

        assertEquals(0, viewModel.uiState.value.earnedCount)
    }

    @Test
    fun `init_errorState_setsErrorField`() = runTest {
        every { getStats.execute() } throws RuntimeException("Network error")
        viewModel = vm()

        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `onEvent_refresh_reloadsDashboard`() = runTest {
        every { getStats.execute() } returns flowOf(TestFixtures.statsWithData)
        viewModel.onEvent(HomeUiEvent.Refresh)

        assertEquals(TestFixtures.statsWithData, viewModel.uiState.value.stats)
    }
}
