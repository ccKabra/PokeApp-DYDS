package com.pokemonarena.presentation

import com.pokemonarena.FixedRandom
import com.pokemonarena.domain.entity.RogueLives
import com.pokemonarena.domain.entity.RogueMetaState
import com.pokemonarena.domain.entity.RogueNodeType
import com.pokemonarena.domain.entity.RogueRules
import com.pokemonarena.domain.entity.RogueSpecies
import com.pokemonarena.domain.entity.RogueUpgrades
import com.pokemonarena.domain.entity.Stats
import com.pokemonarena.domain.usecase.CashOutRogueRunUseCase
import com.pokemonarena.domain.usecase.ConsumeRogueLifeUseCase
import com.pokemonarena.domain.usecase.GetRogueLivesUseCase
import com.pokemonarena.domain.usecase.GetRogueMetaUseCase
import com.pokemonarena.domain.usecase.GetRoguePoolUseCase
import com.pokemonarena.domain.usecase.GetUserCoinsUseCase
import com.pokemonarena.domain.usecase.PurchaseRogueUpgradeUseCase
import com.pokemonarena.presentation.screens.rogue.RogueBattleOutcome
import com.pokemonarena.presentation.screens.rogue.RogueUiEvent
import com.pokemonarena.presentation.screens.rogue.RogueUiState
import com.pokemonarena.presentation.screens.rogue.RogueViewModel
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RogueViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getPool  = mockk<GetRoguePoolUseCase>()
    private val cashOut  = mockk<CashOutRogueRunUseCase>(relaxed = true)
    private val getMeta  = mockk<GetRogueMetaUseCase>()
    private val purchase = mockk<PurchaseRogueUpgradeUseCase>(relaxed = true)
    private val getCoins = mockk<GetUserCoinsUseCase>()
    private val getLives = mockk<GetRogueLivesUseCase>()
    private val consume  = mockk<ConsumeRogueLifeUseCase>()

    private val pool = listOf(
        RogueSpecies(1, "alfa",  "", listOf("normal"), Stats(12, 240, 10, 240, 10, 120), 1),
        RogueSpecies(2, "beta",  "", listOf("normal"), Stats(12, 240, 10, 240, 10, 120), 1),
        RogueSpecies(3, "gamma", "", listOf("normal"), Stats(12, 240, 10, 240, 10, 120), 1),
        RogueSpecies(9, "omega", "", listOf("dragon"), Stats(100, 120, 90, 110, 90, 100),
                     RogueSpecies.BOSS_TIER)
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getPool.execute() } returns pool
        every { getCoins.execute() } returns flowOf(500)
        every { getMeta.execute() } returns flowOf(RogueMetaState())
        coEvery { getLives.execute(any()) } returns RogueLives.full(0L)
        coEvery { consume.execute(any()) } returns RogueLives(RogueLives.MAX - 1, 0L)
    }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = RogueViewModel(getPool, cashOut, getMeta, purchase, getCoins, getLives, consume,
                                      now = { 0L }, random = FixedRandom(0.5f))

    @Test
    fun `init_landsOnTheLobbyWithCurrentCoins`() = runTest {
        val viewModel = vm()
        val state = assertIs<RogueUiState.Lobby>(viewModel.uiState.value)
        assertEquals(500, state.coins)
        viewModel.dispose()
    }

    @Test
    fun `start_offersADraftOfStarters`() = runTest {
        val viewModel = vm()
        viewModel.onEvent(RogueUiEvent.Start)

        val state = assertIs<RogueUiState.Draft>(viewModel.uiState.value)
        assertEquals(RogueRules.DRAFT_SIZE, state.starters.size)
        viewModel.dispose()
    }

    @Test
    fun `start_withoutLives_staysInLobbyWithNotice`() = runTest {
        coEvery { consume.execute(any()) } returns null
        val viewModel = vm()

        viewModel.onEvent(RogueUiEvent.Start)

        val state = assertIs<RogueUiState.Lobby>(viewModel.uiState.value)
        assertNotNull(state.purchaseNotice)
        viewModel.dispose()
    }

    @Test
    fun `pickStarter_beginsActOneWithOnePokemon`() = runTest {
        val viewModel = vm()
        viewModel.onEvent(RogueUiEvent.Start)
        viewModel.onEvent(RogueUiEvent.PickStarter(0))

        val state = assertIs<RogueUiState.Map>(viewModel.uiState.value)
        assertEquals(1, state.run.act)
        assertEquals(1, state.run.team.size)
        assertEquals(0, state.run.loot)
        assertTrue(state.run.reachableNodeIds.isNotEmpty())
        viewModel.dispose()
    }

    @Test
    fun `pickNode_fight_entersBattleAgainstAFullHpEnemy`() = runTest {
        val viewModel = vm()
        enterFirstFight(viewModel)

        val state = assertIs<RogueUiState.Battle>(viewModel.uiState.value)
        assertEquals(state.battle.enemy.maxHp, state.battle.enemy.currentHp)
        assertEquals(RogueNodeType.FIGHT, state.battle.node)
        viewModel.dispose()
    }

    @Test
    fun `combat_isWinnable_aStrongTeamDefeatsTheEnemyAndReachesReward`() = runTest {
        val viewModel = vm()
        enterFirstFight(viewModel)

        repeat(20) {
            when (val st = viewModel.uiState.value) {
                is RogueUiState.Battle ->
                    if (st.battle.outcome == RogueBattleOutcome.ONGOING) viewModel.onEvent(RogueUiEvent.AdvanceBattle)
                    else viewModel.onEvent(RogueUiEvent.ConcludeBattle)
                else -> {}
            }
        }

        val reward = assertIs<RogueUiState.Reward>(viewModel.uiState.value)
        assertTrue(reward.run.loot > 0, "ganar un combate deja oro")
        viewModel.dispose()
    }

    @Test
    fun `abandon_cashesOutGatheredGoldAndFinishesTheRun`() = runTest {
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
    fun `buyUpgrade_withoutEnoughCoins_showsANoticeInTheLobby`() = runTest {
        coEvery { purchase.execute(any()) } returns
            PurchaseRogueUpgradeUseCase.Result.InsufficientCoins(currentCoins = 0, cost = 999)
        val viewModel = vm()

        viewModel.onEvent(RogueUiEvent.BuyUpgrade(RogueUpgrades.SEED_GOLD.id))

        val state = assertIs<RogueUiState.Lobby>(viewModel.uiState.value)
        assertNotNull(state.purchaseNotice)
        viewModel.dispose()
    }

    private fun enterFirstFight(viewModel: RogueViewModel) {
        viewModel.onEvent(RogueUiEvent.Start)
        viewModel.onEvent(RogueUiEvent.PickStarter(0))
        val map = assertIs<RogueUiState.Map>(viewModel.uiState.value)
        val fightId = map.run.reachableNodeIds.first { map.run.node(it).type == RogueNodeType.FIGHT }
        viewModel.onEvent(RogueUiEvent.PickNode(fightId))
    }
}
