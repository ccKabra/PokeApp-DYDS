package com.pokemonarena.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RogueProgressionTest {

    private fun fireSpecies() =
        RogueSpecies(6, "charizard", "", listOf("fire", "flying"),
                     Stats(78, 84, 78, 109, 85, 100), tier = 3)

    @Test
    fun `starterSet_includesBasicAndAStabMove`() {
        val moves = RogueMoves.starterSet(listOf("fire"))

        assertTrue(RogueMoves.BASIC in moves)
        assertTrue(moves.any { it.type == "fire" }, "debe incluir un ataque de su tipo")
        assertTrue(moves.size >= 2)
    }

    @Test
    fun `nextLearnable_gatesStrongMovesByLevel`() {
        val starter = RogueMoves.starterSet(listOf("fire"))

        val atLow  = RogueMoves.nextLearnable(starter, listOf("fire"), level = 5)
        val atHigh = RogueMoves.nextLearnable(starter, listOf("fire"), level = 13)

        assertTrue(atLow == null || atLow.power < 1.4f, "a nivel bajo no se aprende lo más fuerte")
        assertTrue(atHigh != null && atHigh.power >= 1.15f, "a nivel alto se desbloquea algo más potente")
    }

    @Test
    fun `gainingXp_belowThreshold_onlyAccumulates`() {
        val pokemon = RoguePokemon.of(fireSpecies())
        val outcome = pokemon.gainingXp(1)

        assertEquals(0, outcome.levelsGained)
        assertEquals(pokemon.level, outcome.pokemon.level)
        assertEquals(1, outcome.pokemon.xp)
    }

    @Test
    fun `gainingXp_largeAmount_levelsUpBoostsStatsAndLearnsMoves`() {
        val pokemon = RoguePokemon.of(fireSpecies())
        val outcome = pokemon.gainingXp(2_000)

        assertTrue(outcome.levelsGained > 0)
        assertTrue(outcome.pokemon.level > pokemon.level)
        assertTrue(outcome.pokemon.maxHp > pokemon.maxHp, "sube el HP máximo")
        assertTrue(outcome.pokemon.attack > pokemon.attack, "sube el ataque")
        assertTrue(outcome.learnedMoves.isNotEmpty(), "aprende ataques nuevos al subir")
        assertTrue(outcome.pokemon.moves.size <= RogueRules.MOVE_CAP, "nunca supera el tope de ataques")
    }

    @Test
    fun `gainingXp_keepsDamageProportionWhenLevelingUp`() {
        val hurt = RoguePokemon.of(fireSpecies()).damaged(50)
        val outcome = hurt.gainingXp(2_000)

        assertTrue(outcome.pokemon.isAlive)
        assertTrue(outcome.pokemon.currentHp < outcome.pokemon.maxHp,
                   "el daño previo no se cura del todo al subir de nivel")
    }
}
