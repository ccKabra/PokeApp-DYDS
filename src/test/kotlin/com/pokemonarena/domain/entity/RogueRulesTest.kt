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
    fun `enemyScale_startsBelowOneAndGrowsWithDepth`() {
        assertEquals(RogueRules.ENEMY_BASE_SCALE, RogueRules.enemyScale(1))
        assertTrue(RogueRules.enemyScale(1) < 1f, "el primer rival es más débil que el jugador")
        assertTrue(RogueRules.enemyScale(7) > RogueRules.enemyScale(1))
    }

    @Test
    fun `bossScale_growsWithActs`() {
        assertEquals(1f, RogueRules.bossScale(1))
        assertTrue(RogueRules.bossScale(3) > RogueRules.bossScale(1))
    }

    @Test
    fun `bossTierForAct_onlyTheFinalActIsALegendary`() {
        assertEquals(3, RogueRules.bossTierForAct(1))
        assertEquals(3, RogueRules.bossTierForAct(RogueRules.ACTS - 1))
        assertEquals(RogueSpecies.BOSS_TIER, RogueRules.bossTierForAct(RogueRules.ACTS))
    }

    @Test
    fun `bossTeamSize_rampsButNeverOverwhelms`() {
        assertEquals(1, RogueRules.bossTeamSize(1))
        assertTrue(RogueRules.bossTeamSize(RogueRules.ACTS) <= 2)
    }

    @Test
    fun `loot_growsWithDepthAndBossesPayMore`() {
        assertTrue(RogueRules.fightLoot(5) > RogueRules.fightLoot(1))
        assertTrue(RogueRules.bossLoot(2) > RogueRules.fightLoot(2))
    }

    @Test
    fun `payout_championAddsBonus_defeatPaysWhatYouGathered`() {
        assertEquals(100 + RogueRules.CHAMPION_BONUS, RogueRules.payout(100, victory = true))
        assertEquals(100, RogueRules.payout(100, victory = false))
    }

    @Test
    fun `lootWith_fortuneBlessingIncreasesLoot`() {
        assertEquals(100, RogueRules.lootWith(emptySet(), 100))
        assertEquals(150, RogueRules.lootWith(setOf(RogueBlessing.FORTUNA), 100))
    }

    @Test
    fun `depthOf_combinesActAndRowMonotonically`() {
        assertEquals(1, RogueRules.depthOf(act = 1, row = 0))
        assertTrue(RogueRules.depthOf(act = 1, row = 3) > RogueRules.depthOf(act = 1, row = 0))
        assertTrue(RogueRules.depthOf(act = 2, row = 0) > RogueRules.depthOf(act = 1, row = RogueRules.ROWS_PER_ACT))
    }

    @Test
    fun `tierForAct_isCappedAtThree`() {
        assertEquals(1, RogueRules.tierForAct(1))
        assertEquals(2, RogueRules.tierForAct(2))
        assertEquals(3, RogueRules.tierForAct(3))
        assertEquals(3, RogueRules.tierForAct(9))
    }
}
