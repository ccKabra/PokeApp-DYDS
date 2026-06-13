package com.pokemonarena.domain.usecase

import com.pokemonarena.FixedRandom
import com.pokemonarena.TestFixtures
import com.pokemonarena.domain.entity.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimulateBattleUseCaseTest {

    private val useCase = SimulateBattleUseCase(FixedRandom(0.99f))   // sin críticos

    @Test
    fun `execute_whenPlayerCardsAreStronger_playerWinsAllRounds`() {
        val result = useCase.execute(
            playerCards = listOf(TestFixtures.strongCard, TestFixtures.strongCard.copy(id = "s2"),
                                 TestFixtures.strongCard.copy(id = "s3")),
            botCards    = listOf(TestFixtures.weakCard, TestFixtures.weakCard.copy(id = "w2"),
                                 TestFixtures.weakCard.copy(id = "w3")),
            weather     = WeatherCondition.CLEAR,
            gym         = TestFixtures.gym()
        )
        assertEquals(Winner.PLAYER, result.winner)
        assertEquals(3, result.rounds.size)
        assertEquals(3, result.playerRoundWins)
        assertTrue(result.playerWon)
    }

    @Test
    fun `execute_whenBotCardsAreStronger_botWins`() {
        val result = useCase.execute(
            playerCards = listOf(TestFixtures.weakCard),
            botCards    = listOf(TestFixtures.strongCard),
            weather     = WeatherCondition.CLEAR,
            gym         = TestFixtures.gym()
        )
        assertEquals(Winner.BOT, result.winner)
    }

    @Test
    fun `execute_withIdenticalCards_returnsDraw`() {
        val result = useCase.execute(
            playerCards = listOf(TestFixtures.grassCard),
            botCards    = listOf(TestFixtures.grassCard.copy(id = "copy")),
            weather     = WeatherCondition.CLEAR,
            gym         = TestFixtures.gym()
        )
        assertEquals(Winner.DRAW, result.winner)
        assertTrue(result.isDraw)
    }

    @Test
    fun `execute_winnerIsDecidedByRoundsNotByTotalScore`() {
        // El jugador gana 2 rondas ajustadas; el bot gana 1 por paliza.
        // Por puntaje total ganaría el bot, pero por rondas gana el jugador.
        val slightlyStrong = TestFixtures.card("p1", pokemonDetail =
            TestFixtures.detail(types = listOf("normal"), stats = Stats(90, 90, 90, 90, 90, 90)))
        val slightlyWeak   = TestFixtures.card("b1", pokemonDetail =
            TestFixtures.detail(types = listOf("normal"), stats = Stats(80, 80, 80, 80, 80, 80)))
        val crusher        = TestFixtures.card("b3", pokemonDetail =
            TestFixtures.detail(types = listOf("normal"), stats = Stats(250, 250, 250, 250, 250, 250)))
        val sacrifice      = TestFixtures.card("p3", pokemonDetail =
            TestFixtures.detail(types = listOf("normal"), stats = Stats(10, 10, 10, 10, 10, 10)))

        val result = useCase.execute(
            playerCards = listOf(slightlyStrong, slightlyStrong.copy(id = "p2"), sacrifice),
            botCards    = listOf(slightlyWeak, slightlyWeak.copy(id = "b2"), crusher),
            weather     = WeatherCondition.CLEAR,
            gym         = TestFixtures.gym()
        )
        assertTrue(result.botScore > result.playerScore, "el bot debe sumar más puntaje total")
        assertEquals(Winner.PLAYER, result.winner)
        assertEquals(2, result.playerRoundWins)
        assertEquals(1, result.botRoundWins)
    }

    @Test
    fun `execute_appliesTypeMatchupMultipliers`() {
        val result = useCase.execute(
            playerCards = listOf(TestFixtures.fireCard),      // fire vs grass: ventaja
            botCards    = listOf(TestFixtures.grassCard),
            weather     = WeatherCondition.CLEAR,
            gym         = TestFixtures.gym()
        )
        val round = result.rounds.single()
        assertEquals(TypeMatchup.ADVANTAGE,    round.playerMatchup)
        assertEquals(TypeMatchup.DISADVANTAGE, round.botMatchup)
    }

    @Test
    fun `execute_withAlwaysLowRolls_playerMissesAndBotCrits`() {
        val lowRolls   = SimulateBattleUseCase(FixedRandom(0.0f))
        val normalCard = TestFixtures.card("n1", pokemonDetail =
            TestFixtures.detail(types = listOf("normal")))
        val result = lowRolls.execute(
            playerCards = listOf(normalCard),
            botCards    = listOf(normalCard.copy(id = "n2")),
            weather     = WeatherCondition.CLEAR,
            gym         = TestFixtures.gym()
        )
        val round = result.rounds.single()
        assertTrue(round.playerMissed)
        assertTrue(round.botCrit)
        assertEquals(0f, round.playerScore)
        assertEquals(Winner.BOT, round.winner)
        val expectedBot = lowRolls.scoreOf(normalCard, WeatherCondition.CLEAR) *
                          SimulateBattleUseCase.CRIT_MULTIPLIER
        assertEquals(expectedBot, round.botScore, 0.0001f)
    }

    @Test
    fun `execute_wideLensReducesMissChance`() {
        val justBelowReducedThreshold = SimulateBattleUseCase(FixedRandom(0.27f))
        val plain  = TestFixtures.card("n1", pokemonDetail = TestFixtures.detail(types = listOf("normal")))
        val lensed = plain.copy(id = "n2", heldItem = ItemCatalog.byId("wide-lens"))

        val withoutLens = justBelowReducedThreshold.execute(
            playerCards = listOf(plain), botCards = listOf(plain.copy(id = "b1")),
            weather = WeatherCondition.CLEAR, gym = TestFixtures.gym()
        ).rounds.single()
        val withLens = justBelowReducedThreshold.execute(
            playerCards = listOf(lensed), botCards = listOf(plain.copy(id = "b2")),
            weather = WeatherCondition.CLEAR, gym = TestFixtures.gym()
        ).rounds.single()

        assertTrue(withoutLens.playerMissed, "con roll 0.27 y miss base 0.30 debe fallar")
        assertTrue(!withLens.playerMissed, "con la Lupa el umbral baja a 0.25 y no debe fallar")
    }

    @Test
    fun `execute_neverCritsNorMisses_scoreEqualsBaseTimesMatchup`() {
        val result = useCase.execute(
            playerCards = listOf(TestFixtures.fireCard),
            botCards    = listOf(TestFixtures.grassCard),
            weather     = WeatherCondition.SUNNY,
            gym         = TestFixtures.gym()
        )
        val round = result.rounds.single()
        assertTrue(!round.playerCrit && !round.botCrit && !round.playerMissed)
        val expected = useCase.scoreOf(TestFixtures.fireCard, WeatherCondition.SUNNY) * TypeMatchup.ADVANTAGE
        assertEquals(expected, round.playerScore, 0.0001f)
    }

    @Test
    fun `execute_harderGymPaysMoreCoinsForSameVictory`() {
        fun winAt(difficulty: Int) = useCase.execute(
            playerCards = listOf(TestFixtures.strongCard),
            botCards    = listOf(TestFixtures.weakCard),
            weather     = WeatherCondition.CLEAR,
            gym         = TestFixtures.gym(difficulty)
        ).coinsDelta

        assertTrue(winAt(5) > winAt(1), "ganar en un gimnasio de dificultad 5 debe pagar más que en uno de dificultad 1")
    }

    @Test
    fun `scoreOf_withClearWeather_appliesNoMultiplier`() {
        val card     = TestFixtures.fireCard
        val score    = useCase.scoreOf(card, WeatherCondition.CLEAR)
        val expected = computeExpected(card.stats, 1.0f)
        assertEquals(expected, score, 0.0001f)
    }

    @Test
    fun `scoreOf_withFireTypeUnderSunny_applies150xMultiplier`() {
        val score    = useCase.scoreOf(TestFixtures.fireCard, WeatherCondition.SUNNY)
        val expected = computeExpected(TestFixtures.fireCard.stats, 1.5f)
        assertEquals(expected, score, 0.0001f)
    }

    @Test
    fun `scoreOf_withZeroStats_returnsZero`() {
        val deadCard = TestFixtures.card(pokemonDetail = TestFixtures.detail(stats = TestFixtures.zeroStats))
        assertEquals(0f, useCase.scoreOf(deadCard, WeatherCondition.CLEAR), 0.0001f)
    }

    private fun computeExpected(stats: Stats, multiplier: Float): Float =
        (stats.attack * 0.25f + stats.defense * 0.20f + stats.speed * 0.20f +
         stats.hp * 0.15f + stats.specialAttack * 0.10f + stats.specialDefense * 0.10f) / 255f * multiplier
}
