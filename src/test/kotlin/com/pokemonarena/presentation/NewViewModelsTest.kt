package com.pokemonarena.presentation

import com.pokemonarena.TestFixtures
import com.pokemonarena.domain.entity.Gym
import com.pokemonarena.domain.entity.WeatherCondition
import com.pokemonarena.domain.usecase.AwardBadgeIfFirstWinUseCase
import com.pokemonarena.domain.usecase.CureFatigueUseCase
import com.pokemonarena.domain.usecase.DropHeldItemUseCase
import com.pokemonarena.domain.usecase.EquipItemUseCase
import com.pokemonarena.domain.usecase.GetGymByNameUseCase
import com.pokemonarena.domain.usecase.GetItemCatalogUseCase
import com.pokemonarena.domain.usecase.GetItemInventoryUseCase
import com.pokemonarena.domain.usecase.GetOwnedCardsUseCase
import com.pokemonarena.domain.usecase.GetTeamUseCase
import com.pokemonarena.domain.usecase.GetUserStatisticsUseCase
import com.pokemonarena.domain.usecase.GetWeatherConditionUseCase
import com.pokemonarena.domain.usecase.SaveBattleResultUseCase
import com.pokemonarena.domain.usecase.SellCardUseCase
import com.pokemonarena.domain.usecase.SimulateBattleUseCase
import com.pokemonarena.domain.usecase.UnequipItemUseCase
import com.pokemonarena.domain.usecase.UpdateTeamUseCase
import com.pokemonarena.presentation.screens.battle.BattleUiEvent
import com.pokemonarena.presentation.screens.battle.BattleUiState
import com.pokemonarena.presentation.screens.battle.BattleViewModel
import com.pokemonarena.presentation.screens.myteam.MyTeamUiEvent
import com.pokemonarena.presentation.screens.myteam.MyTeamViewModel
import io.mockk.coEvery
import io.mockk.coVerify
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MyTeamViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getOwned     = mockk<GetOwnedCardsUseCase>()
    private val getTeam      = mockk<GetTeamUseCase>()
    private val updateTeam   = mockk<UpdateTeamUseCase>(relaxed = true)
    private val getStats     = mockk<GetUserStatisticsUseCase>()
    private val sellCard     = mockk<SellCardUseCase>(relaxed = true)
    private val getCatalog   = mockk<GetItemCatalogUseCase>()
    private val getInventory = mockk<GetItemInventoryUseCase>()
    private val equipItem    = mockk<EquipItemUseCase>(relaxed = true)
    private val unequipItem  = mockk<UnequipItemUseCase>(relaxed = true)
    private val cureFatigue  = mockk<CureFatigueUseCase>(relaxed = true)

    @BeforeTest
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm(): MyTeamViewModel {
        coEvery { getCatalog.execute() } returns emptyList()
        every   { getInventory.execute() } returns flowOf(emptyMap())
        return MyTeamViewModel(getOwned, getTeam, updateTeam, getStats, sellCard,
                               getCatalog, getInventory, equipItem, unequipItem, cureFatigue)
    }

    @Test
    fun `uiState_emitsOwnedCardsTeamAndStats`() = runTest {
        every { getOwned.execute() } returns flowOf(listOf(TestFixtures.fireCard))
        every { getTeam.execute()  } returns flowOf(listOf(TestFixtures.waterCard))
        every { getStats.execute() } returns flowOf(TestFixtures.statsWithData.copy(coins = 350))

        val state = vm().uiState.value

        assertFalse(state.isLoading)
        assertEquals(1, state.ownedCards.size)
        assertEquals(1, state.teamCards.size)
        assertEquals(350, state.stats.coins)
    }

    @Test
    fun `canBattle_isFalseWithLessThan3TeamCards`() = runTest {
        every { getOwned.execute() } returns flowOf(listOf(TestFixtures.fireCard))
        every { getTeam.execute()  } returns flowOf(listOf(TestFixtures.fireCard, TestFixtures.waterCard))
        every { getStats.execute() } returns flowOf(TestFixtures.emptyStats)

        assertFalse(vm().uiState.value.canBattle)
    }

    @Test
    fun `canBattle_isTrueWithExactly3TeamCards`() = runTest {
        val team = listOf(TestFixtures.fireCard, TestFixtures.waterCard, TestFixtures.grassCard)
        every { getOwned.execute() } returns flowOf(team)
        every { getTeam.execute()  } returns flowOf(team)
        every { getStats.execute() } returns flowOf(TestFixtures.emptyStats)

        assertTrue(vm().uiState.value.canBattle)
    }

    @Test
    fun `onEvent_toggleCardNotInTeam_callsUpdateWithTrue`() = runTest {
        every { getOwned.execute() } returns flowOf(listOf(TestFixtures.fireCard))
        every { getTeam.execute()  } returns flowOf(emptyList())
        every { getStats.execute() } returns flowOf(TestFixtures.emptyStats)
        coEvery { updateTeam.execute(any(), any(), any()) } returns true

        vm().onEvent(MyTeamUiEvent.ToggleCard(TestFixtures.fireCard.id))

        coVerify { updateTeam.execute(TestFixtures.fireCard.id, true, emptyList()) }
    }

    @Test
    fun `onEvent_toggleCardAlreadyInTeam_callsUpdateWithFalse`() = runTest {
        val currentTeam = listOf(TestFixtures.fireCard)
        every { getOwned.execute() } returns flowOf(currentTeam)
        every { getTeam.execute()  } returns flowOf(currentTeam)
        every { getStats.execute() } returns flowOf(TestFixtures.emptyStats)
        coEvery { updateTeam.execute(any(), any(), any()) } returns true

        vm().onEvent(MyTeamUiEvent.ToggleCard(TestFixtures.fireCard.id))

        coVerify { updateTeam.execute(TestFixtures.fireCard.id, false, currentTeam) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BattleViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getGymByName = mockk<GetGymByNameUseCase>()
    private val getTeam      = mockk<GetTeamUseCase>()
    private val getWeather   = mockk<GetWeatherConditionUseCase>()
    private val simulate     = mockk<SimulateBattleUseCase>()
    private val saveResult   = mockk<SaveBattleResultUseCase>(relaxed = true)
    private val dropItem     = mockk<DropHeldItemUseCase>()
    private val awardBadge   = mockk<AwardBadgeIfFirstWinUseCase>()

    private val testGym = Gym("Pewter", "City", 35.0, 139.0, "rock",
        listOf(TestFixtures.fireCard, TestFixtures.waterCard, TestFixtures.grassCard))

    @BeforeTest
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm(): BattleViewModel {
        coEvery { dropItem.execute(any()) } returns null
        coEvery { awardBadge.execute(any()) } returns null
        return BattleViewModel(getGymByName, getTeam, getWeather, simulate, saveResult, dropItem, awardBadge)
    }

    @Test
    fun `onEvent_loadWithFullTeamAndGym_transitionsToReady`() = runTest {
        val team = listOf(TestFixtures.fireCard, TestFixtures.waterCard, TestFixtures.grassCard)
        coEvery { getGymByName.execute("Pewter")   } returns testGym
        every   { getTeam.execute()                } returns flowOf(team)
        coEvery { getWeather.execute(any(), any()) } returns WeatherCondition.CLEAR

        val viewModel = vm()
        viewModel.onEvent(BattleUiEvent.Load("Pewter"))

        assertIs<BattleUiState.Ready>(viewModel.uiState.value)
        val ready = viewModel.uiState.value as BattleUiState.Ready
        assertEquals(3, ready.teamCards.size)
        assertEquals(3, ready.botCards.size)
        assertTrue(ready.canBattle)
    }

    @Test
    fun `onEvent_loadWithIncompleteTeam_transitionsToError`() = runTest {
        coEvery { getGymByName.execute(any()) } returns testGym
        every   { getTeam.execute()           } returns flowOf(listOf(TestFixtures.fireCard))

        val viewModel = vm()
        viewModel.onEvent(BattleUiEvent.Load("Pewter"))

        assertIs<BattleUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun `onEvent_loadWithGymNotFound_transitionsToError`() = runTest {
        coEvery { getGymByName.execute(any()) } returns null
        every   { getTeam.execute()           } returns flowOf(emptyList())

        val viewModel = vm()
        viewModel.onEvent(BattleUiEvent.Load("GymInexistente"))

        assertIs<BattleUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun `onEvent_fightWithReadyState_simulatesSavesAndEntersCombat`() = runTest {
        val team = listOf(TestFixtures.fireCard, TestFixtures.waterCard, TestFixtures.grassCard)
        coEvery { getGymByName.execute("Pewter")   } returns testGym
        every   { getTeam.execute()                } returns flowOf(team)
        coEvery { getWeather.execute(any(), any()) } returns WeatherCondition.SUNNY
        every   { simulate.execute(any(), any(), any(), any(), any()) } returns TestFixtures.playerWin

        val viewModel = vm()
        viewModel.onEvent(BattleUiEvent.Load("Pewter"))
        viewModel.onEvent(BattleUiEvent.Fight)

        assertIs<BattleUiState.Combat>(viewModel.uiState.value)
        assertNotNull(viewModel.lastResult.value)
        coVerify { saveResult.execute(any()) }
    }

    @Test
    fun `onEvent_fightWithoutReadyState_doesNothing`() = runTest {
        val viewModel = vm()
        viewModel.onEvent(BattleUiEvent.Fight)

        assertNull(viewModel.lastResult.value)
        coVerify(exactly = 0) { saveResult.execute(any()) }
    }

    @Test
    fun `onEvent_moveCard_reordersTheTeamForTheMatchups`() = runTest {
        val team = listOf(TestFixtures.fireCard, TestFixtures.waterCard, TestFixtures.grassCard)
        coEvery { getGymByName.execute("Pewter")   } returns testGym
        every   { getTeam.execute()                } returns flowOf(team)
        coEvery { getWeather.execute(any(), any()) } returns WeatherCondition.CLEAR

        val viewModel = vm()
        viewModel.onEvent(BattleUiEvent.Load("Pewter"))
        viewModel.onEvent(BattleUiEvent.MoveCard(0, up = false))

        val ready = viewModel.uiState.value as BattleUiState.Ready
        assertEquals(listOf(TestFixtures.waterCard.id, TestFixtures.fireCard.id, TestFixtures.grassCard.id),
                     ready.teamCards.map { it.id })
    }

    @Test
    fun `onEvent_moveCardOutOfBounds_keepsOrderUnchanged`() = runTest {
        val team = listOf(TestFixtures.fireCard, TestFixtures.waterCard, TestFixtures.grassCard)
        coEvery { getGymByName.execute("Pewter")   } returns testGym
        every   { getTeam.execute()                } returns flowOf(team)
        coEvery { getWeather.execute(any(), any()) } returns WeatherCondition.CLEAR

        val viewModel = vm()
        viewModel.onEvent(BattleUiEvent.Load("Pewter"))
        viewModel.onEvent(BattleUiEvent.MoveCard(0, up = true))

        val ready = viewModel.uiState.value as BattleUiState.Ready
        assertEquals(team.map { it.id }, ready.teamCards.map { it.id })
    }
}
