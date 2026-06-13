package com.pokemonarena.presentation

import com.pokemonarena.FixedRandom
import com.pokemonarena.domain.entity.RogueNodeType
import com.pokemonarena.domain.entity.RoguePokemon
import com.pokemonarena.domain.entity.RogueRules
import com.pokemonarena.domain.entity.RogueSpecies
import com.pokemonarena.domain.entity.Stats
import com.pokemonarena.domain.usecase.CashOutRogueRunUseCase
import com.pokemonarena.domain.usecase.GetRoguePoolUseCase
import com.pokemonarena.presentation.screens.rogue.RogueRunSnapshot
import com.pokemonarena.presentation.screens.rogue.RogueUiEvent
import com.pokemonarena.presentation.screens.rogue.RogueUiState
import com.pokemonarena.presentation.screens.rogue.RogueViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RogueViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getPool = mockk<GetRoguePoolUseCase>()
    private val cashOut = mockk<CashOutRogueRunUseCase>(relaxed = true)

    private val pool = listOf(
        RogueSpecies(1, "alfa",  "", listOf("normal"), Stats(60, 70, 50, 40, 40, 80), 1),
        RogueSpecies(2, "beta",  "", listOf("fire"),   Stats(55, 65, 45, 70, 45, 70), 1),
        RogueSpecies(3, "gamma", "", listOf("water"),  Stats(65, 60, 55, 55, 50, 60), 1),
        RogueSpecies(4, "delta", "", listOf("grass"),  Stats(50, 60, 40, 60, 40, 90), 1),
        RogueSpecies(9, "jefe",  "", listOf("dragon"), Stats(100, 120, 90, 110, 90, 100),
                     RogueSpecies.BOSS_TIER)
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getPool.execute() } returns pool
    }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = RogueViewModel(getPool, cashOut, FixedRandom(0.5f))

    @Test
    fun `start_offersADraftOfThreeStarters`() = runTest {
        val viewModel = vm()
        viewModel.onEvent(RogueUiEvent.Start)

        val state = assertIs<RogueUiState.Draft>(viewModel.uiState.value)
        assertEquals(RogueRules.DRAFT_SIZE, state.starters.size)
        viewModel.dispose()
    }

    @Test
    fun `pickStarter_beginsTheRunOnFloorOneWithOnePokemon`() = runTest {
        val viewModel = vm()
        viewModel.onEvent(RogueUiEvent.Start)
        viewModel.onEvent(RogueUiEvent.PickStarter(0))

        val state = assertIs<RogueUiState.PathChoice>(viewModel.uiState.value)
        assertEquals(1, state.run.floor)
        assertEquals(1, state.run.team.size)
        assertEquals(0, state.run.loot)
        assertTrue(state.options.isNotEmpty())
        viewModel.dispose()
    }

    @Test
    fun `pickNode_fight_entersBattleAgainstAFullHpEnemy`() = runTest {
        val viewModel = vm()
        viewModel.onEvent(RogueUiEvent.Start)
        viewModel.onEvent(RogueUiEvent.PickStarter(0))

        val path  = assertIs<RogueUiState.PathChoice>(viewModel.uiState.value)
        viewModel.onEvent(RogueUiEvent.PickNode(path.options.indexOf(RogueNodeType.FIGHT)))

        val state = assertIs<RogueUiState.Battle>(viewModel.uiState.value)
        assertEquals(state.enemy.maxHp, state.enemy.currentHp)
        viewModel.dispose()
    }

    @Test
    fun `combat_isUnwinnable_attackingForeverEndsInDefeatAndNeverRewards`() = runTest {
        val viewModel = vm()
        viewModel.onEvent(RogueUiEvent.Start)
        viewModel.onEvent(RogueUiEvent.PickStarter(0))
        val path = assertIs<RogueUiState.PathChoice>(viewModel.uiState.value)
        viewModel.onEvent(RogueUiEvent.PickNode(path.options.indexOf(RogueNodeType.FIGHT)))
        assertIs<RogueUiState.Battle>(viewModel.uiState.value)

        var sawReward = false
        repeat(500) {
            when (val st = viewModel.uiState.value) {
                is RogueUiState.Battle ->
                    if (st.awaitingSwap) {
                        val alive = st.run.team.indexOfFirst { it.isAlive }
                        if (alive >= 0) viewModel.onEvent(RogueUiEvent.SetActive(alive))
                    } else viewModel.onEvent(RogueUiEvent.Attack(0))
                is RogueUiState.Reward -> sawReward = true
                else -> {}
            }
        }

        val end = assertIs<RogueUiState.Finished>(viewModel.uiState.value)
        assertTrue(!end.victory, "la victoria en combate es imposible")
        assertTrue(!sawReward, "atacar nunca debe producir una recompensa de victoria")
        viewModel.dispose()
    }

    @Test
    fun `spendingHopeToken_inANormalFight_escapesToRewardAndDecrementsTokens`() = runTest {
        val viewModel = vm()
        viewModel.onEvent(RogueUiEvent.Start)
        viewModel.onEvent(RogueUiEvent.PickStarter(0))
        val path = assertIs<RogueUiState.PathChoice>(viewModel.uiState.value)
        viewModel.onEvent(RogueUiEvent.PickNode(path.options.indexOf(RogueNodeType.FIGHT)))

        val battle = assertIs<RogueUiState.Battle>(viewModel.uiState.value)
        assertTrue(battle.canSpendHope)

        viewModel.onEvent(RogueUiEvent.SpendHope)

        val reward = assertIs<RogueUiState.Reward>(viewModel.uiState.value)
        assertEquals(RogueRules.HOPE_TOKENS_START - 1, reward.run.hopeTokens)
        viewModel.dispose()
    }

    @Test
    fun `bossBattle_allowsNeitherFleeingNorHopeTokens`() {
        val mon  = RoguePokemon.of(pool.first())
        val boss = RogueUiState.Battle(
            run   = RogueRunSnapshot(floor = RogueRules.FLOORS, team = listOf(mon), hopeTokens = 99),
            node  = RogueNodeType.BOSS,
            enemy = RoguePokemon.of(pool.last()),
            turnsSurvived = 9_999
        )

        assertTrue(!boss.canFlee, "del jefe no se puede huir")
        assertTrue(!boss.canSpendHope, "las fichas no sirven contra el jefe")
    }

    @Test
    fun `abandon_cashesOutHalfTheLootAndFinishesTheRun`() = runTest {
        val viewModel = vm()
        viewModel.onEvent(RogueUiEvent.Start)
        viewModel.onEvent(RogueUiEvent.PickStarter(0))

        viewModel.onEvent(RogueUiEvent.Abandon)

        val state = assertIs<RogueUiState.Finished>(viewModel.uiState.value)
        assertTrue(!state.victory)
        assertEquals(RogueRules.payout(state.run.loot, victory = false), state.payout)
        coVerify(exactly = 1) { cashOut.execute(state.payout) }
        viewModel.dispose()
    }

    @Test
    fun `backToIdle_returnsToTheEntrance`() = runTest {
        val viewModel = vm()
        viewModel.onEvent(RogueUiEvent.Start)
        viewModel.onEvent(RogueUiEvent.PickStarter(0))
        viewModel.onEvent(RogueUiEvent.Abandon)

        viewModel.onEvent(RogueUiEvent.BackToIdle)

        assertIs<RogueUiState.Idle>(viewModel.uiState.value)
        viewModel.dispose()
    }
}
