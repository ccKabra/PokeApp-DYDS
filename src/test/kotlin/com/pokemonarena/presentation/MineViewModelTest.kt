package com.pokemonarena.presentation

import com.pokemonarena.FixedRandom
import com.pokemonarena.domain.entity.MiningReward
import com.pokemonarena.domain.entity.MiningTier
import com.pokemonarena.domain.usecase.GetUserCoinsUseCase
import com.pokemonarena.domain.usecase.MineCoinsUseCase
import com.pokemonarena.domain.usecase.RegisterAimShotUseCase
import com.pokemonarena.presentation.screens.mine.MineUiEvent
import com.pokemonarena.presentation.screens.mine.MineViewModel
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
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MineViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mineCoins    = mockk<MineCoinsUseCase>()
    private val getUserCoins = mockk<GetUserCoinsUseCase>()
    private val aimShot      = mockk<RegisterAimShotUseCase>(relaxed = true)

    @BeforeTest
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    // FixedRandom(0.99) infla ~0.30 por click (explota al 4to);
    // FixedRandom(0.0) infla 0.07 por click (explota recién al 15to).
    private fun vm(random: Random) = MineViewModel(mineCoins, getUserCoins, aimShot, random)

    @Test
    fun `uiState_reflectsCurrentCoins`() = runTest {
        every { getUserCoins.execute() } returns flowOf(321)
        val viewModel = vm(FixedRandom(0.0f))
        assertEquals(321, viewModel.uiState.value.coins)
        viewModel.dispose()
    }

    @Test
    fun `onEvent_dig_accumulatesRewardsAndInflatesTheBalloon`() = runTest {
        every { getUserCoins.execute() } returns flowOf(0)
        coEvery { mineCoins.execute() } returns MiningReward(10, MiningTier.GREAT)
        val viewModel = vm(FixedRandom(0.0f))

        viewModel.onEvent(MineUiEvent.Dig)
        viewModel.onEvent(MineUiEvent.Dig)

        assertEquals(2, viewModel.uiState.value.clicks)
        assertEquals(20, viewModel.uiState.value.totalMined)
        assertTrue(viewModel.uiState.value.pressure > 0f, "el globo debe inflarse con cada golpe")
        viewModel.dispose()
    }

    @Test
    fun `onEvent_dig_explodesWhenPressureReachesTheLimit`() = runTest {
        every { getUserCoins.execute() } returns flowOf(0)
        coEvery { mineCoins.execute() } returns MiningReward(1, MiningTier.COMMON)
        val viewModel = vm(FixedRandom(0.99f))   // ~0.30 por click

        repeat(4) { viewModel.onEvent(MineUiEvent.Dig) }

        assertTrue(viewModel.uiState.value.isBroken, "al cuarto click el globo debe explotar")
        assertTrue(viewModel.uiState.value.cooldownSeconds > 0)
        viewModel.dispose()
    }

    @Test
    fun `onEvent_dig_whileExploded_isIgnored`() = runTest {
        every { getUserCoins.execute() } returns flowOf(0)
        coEvery { mineCoins.execute() } returns MiningReward(1, MiningTier.COMMON)
        val viewModel = vm(FixedRandom(0.99f))

        repeat(7) { viewModel.onEvent(MineUiEvent.Dig) }   // los últimos 3 pegan contra el globo roto

        assertEquals(4, viewModel.uiState.value.clicks)
        coVerify(exactly = 4) { mineCoins.execute() }
        viewModel.dispose()
    }

    @Test
    fun `pressure_deflatesToZeroAfterPausingTheClicks`() = runTest {
        every { getUserCoins.execute() } returns flowOf(0)
        coEvery { mineCoins.execute() } returns MiningReward(1, MiningTier.COMMON)
        val viewModel = vm(FixedRandom(0.99f))

        repeat(2) { viewModel.onEvent(MineUiEvent.Dig) }
        assertTrue(viewModel.uiState.value.pressure > 0.5f)

        testDispatcher.scheduler.advanceTimeBy(2_000)      // pausa: pasa el umbral de desinflado
        testDispatcher.scheduler.runCurrent()

        assertEquals(0f, viewModel.uiState.value.pressure)
        assertFalse(viewModel.uiState.value.isBroken, "desinflarse no es explotar")
        viewModel.dispose()
    }

    @Test
    fun `explosion_recoversAfterTheCooldownWithPressureAtZero`() = runTest {
        every { getUserCoins.execute() } returns flowOf(0)
        coEvery { mineCoins.execute() } returns MiningReward(1, MiningTier.COMMON)
        val viewModel = vm(FixedRandom(0.99f))

        repeat(4) { viewModel.onEvent(MineUiEvent.Dig) }
        assertTrue(viewModel.uiState.value.isBroken)

        testDispatcher.scheduler.advanceTimeBy(7_000)      // pasa el cooldown completo
        testDispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isBroken)
        assertEquals(0f, viewModel.uiState.value.pressure)
        viewModel.dispose()
    }
}
