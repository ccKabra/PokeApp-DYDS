package com.pokemonarena.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RogueEvolutionTest {

    private val oddish =
        RogueSpecies(43, "oddish", "", listOf("grass", "poison"), Stats(45, 50, 55, 75, 65, 30), tier = 1)
    private val gloom =
        RogueSpecies(44, "gloom", "", listOf("grass", "poison"), Stats(60, 65, 70, 85, 75, 40), tier = 2)

    @Test
    fun `nextStage_gatesEvolutionByLevel`() {
        assertEquals(null, RogueEvolutions.nextStage(43, level = 5), "a nivel bajo no evoluciona")
        assertEquals(44, RogueEvolutions.nextStage(43, level = 12), "alcanzado el umbral, evoluciona")
        assertEquals(45, RogueEvolutions.nextStage(44, level = 22), "y luego a la etapa final")
    }

    @Test
    fun `nextStage_returnsNullForNonEvolvingSpecies`() {
        assertEquals(null, RogueEvolutions.nextStage(143, level = 99), "snorlax no evoluciona")
    }

    @Test
    fun `nextStageAny_ignoresTheLevelGate`() {
        assertEquals(44, RogueEvolutions.nextStageAny(43), "la reliquia evoluciona sin importar el nivel")
        assertEquals(null, RogueEvolutions.nextStageAny(143), "snorlax sigue sin evolucionar")
    }

    @Test
    fun `evolveInto_adoptsNewSpeciesStatsAndKeepsLevel`() {
        val base = RoguePokemon.of(oddish, level = 12)
        val evolved = base.evolveInto(gloom)

        assertEquals(gloom, evolved.species)
        assertEquals(base.level, evolved.level, "la evolución conserva el nivel")
        assertTrue(evolved.maxHp > base.maxHp, "gloom es más fuerte que oddish")
    }

    @Test
    fun `evolveInto_preservesHpProportionAndEquippedItem`() {
        val gear = RogueItem("x", "Coraza", "test", attackMult = 1.5f)
        val hurt = RoguePokemon.of(oddish, level = 12).equip(gear).damaged(20)
        val ratioBefore = hurt.hpFraction

        val evolved = hurt.evolveInto(gloom)

        assertEquals(gear, evolved.item, "mantiene el item al evolucionar")
        assertTrue(kotlin.math.abs(evolved.hpFraction - ratioBefore) < 0.1f,
                   "conserva aproximadamente la proporción de HP")
    }

    @Test
    fun `evolveInto_doesNotReviveAFaintedPokemon`() {
        val fainted = RoguePokemon.of(oddish, level = 12).damaged(99_999)
        val evolved = fainted.evolveInto(gloom)
        assertTrue(!evolved.isAlive, "evolucionar no revive a un debilitado")
    }
}
