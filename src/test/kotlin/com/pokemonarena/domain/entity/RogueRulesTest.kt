package com.pokemonarena.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RogueRulesTest {

    private fun species(stats: Stats = Stats(80, 90, 70, 60, 50, 85)) =
        RogueSpecies(1, "testmon", "", listOf("normal"), stats, tier = 1)

    @Test
    fun `of_buildsPokemonFromStats`() {
        val pokemon = RoguePokemon.of(species())

        assertEquals(80 * RogueRules.HP_FACTOR, pokemon.maxHp)
        assertEquals(pokemon.maxHp, pokemon.currentHp)
        assertEquals(90, pokemon.attack, "usa el mejor entre ataque y ataque especial")
        assertEquals(60, pokemon.defense, "promedia defensa y defensa especial")
    }

    @Test
    fun `damaged_neverGoesBelowZero_andHealOnlyAffectsAlive`() {
        val pokemon = RoguePokemon.of(species())
        val fainted = pokemon.damaged(99_999)

        assertEquals(0, fainted.currentHp)
        assertEquals(0, fainted.healedBy(1f).currentHp, "un debilitado no se cura")
        assertTrue(pokemon.damaged(10).healedBy(0.5f).currentHp > pokemon.damaged(10).currentHp)
    }

    @Test
    fun `withMaxHpBoost_keepsFaintedAtZero`() {
        val fainted = RoguePokemon.of(species()).damaged(99_999)
        val boosted = fainted.withMaxHpBoost(RogueRules.HP_BLESSING)

        assertEquals(0, boosted.currentHp)
        assertTrue(boosted.maxHp > fainted.maxHp)
    }

    @Test
    fun `enemyScaleFor_growsWithFloorsAndElites`() {
        val floor1 = RogueRules.enemyScaleFor(1, RogueNodeType.FIGHT)
        val floor7 = RogueRules.enemyScaleFor(7, RogueNodeType.FIGHT)
        val elite7 = RogueRules.enemyScaleFor(7, RogueNodeType.ELITE)

        assertEquals(1f, floor1)
        assertTrue(floor7 > floor1)
        assertTrue(elite7 > floor7)
    }

    @Test
    fun `victoryLoot_elitePaysDoubleAndGrowsWithFloor`() {
        assertTrue(RogueRules.victoryLoot(5, RogueNodeType.FIGHT) >
                   RogueRules.victoryLoot(1, RogueNodeType.FIGHT))
        assertEquals(RogueRules.victoryLoot(3, RogueNodeType.FIGHT) * 2,
                     RogueRules.victoryLoot(3, RogueNodeType.ELITE))
    }

    @Test
    fun `payout_victoryAddsBonus_defeatPaysHalf`() {
        assertEquals(100 + RogueRules.BOSS_BONUS, RogueRules.payout(100, victory = true))
        assertEquals(50, RogueRules.payout(100, victory = false))
    }

    @Test
    fun `lootWith_fortuneBlessingIncreasesLoot`() {
        val base = 100
        assertEquals(base, RogueRules.lootWith(emptySet(), base))
        assertEquals(150, RogueRules.lootWith(setOf(RogueBlessing.FORTUNA), base))
    }

    @Test
    fun `tierForFloor_coversAllFloors`() {
        assertEquals(1, RogueRules.tierForFloor(1))
        assertEquals(1, RogueRules.tierForFloor(4))
        assertEquals(2, RogueRules.tierForFloor(5))
        assertEquals(2, RogueRules.tierForFloor(8))
        assertEquals(3, RogueRules.tierForFloor(9))
        assertEquals(3, RogueRules.tierForFloor(RogueRules.FLOORS))
    }
}
