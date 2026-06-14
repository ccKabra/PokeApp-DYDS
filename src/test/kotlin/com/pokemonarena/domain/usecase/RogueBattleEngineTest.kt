package com.pokemonarena.domain.usecase

import com.pokemonarena.FixedRandom
import com.pokemonarena.domain.entity.RogueBlessing
import com.pokemonarena.domain.entity.RoguePokemon
import com.pokemonarena.domain.entity.RogueSpecies
import com.pokemonarena.domain.entity.Stats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RogueBattleEngineTest {

    private val engine = RogueBattleEngine(FixedRandom(0.5f))

    private fun mon(attack: Int = 80, defense: Int = 60, speed: Int = 80,
                    hp: Int = 80, type: String = "normal", name: String = "mon") =
        RoguePokemon.of(RogueSpecies(1, name, "", listOf(type),
                                     Stats(hp, attack, defense, attack, defense, speed), 1))

    @Test
    fun `duel_fasterPokemonStrikesFirst`() {
        val fast = mon(speed = 120, name = "veloz", hp = 400, defense = 999)
        val slow = mon(speed = 30, name = "lento", hp = 400, defense = 999)

        val result = engine.duel(fast, slow, emptySet())

        assertTrue(result.strikes.first().isPlayerAttack)
        assertEquals("Veloz", result.strikes.first().attackerName)
    }

    @Test
    fun `duel_isWinnable_strongPlayerKnocksOutTheEnemy`() {
        val killer = mon(attack = 9_999, speed = 120)
        val victim = mon(hp = 10, speed = 10, defense = 1)

        val result = engine.duel(killer, victim, emptySet())

        assertTrue(result.enemyFainted, "el rival SÍ puede ser derrotado")
        assertTrue(result.player.isAlive, "el jugador sobrevive")
        assertTrue(!result.enemy.isAlive)
    }

    @Test
    fun `duel_endsWithExactlyOneFainted`() {
        val a = mon(attack = 300, speed = 90, hp = 60)
        val b = mon(attack = 300, speed = 50, hp = 60)

        val result = engine.duel(a, b, emptySet())

        assertTrue(result.playerFainted != result.enemyFainted,
                   "un duelo termina cuando uno cae, no ambos")
    }

    @Test
    fun `duel_damageIsAtLeastOne`() {
        val weak = mon(attack = 1, speed = 120, hp = 400)
        val tank = mon(defense = 999, speed = 10, hp = 400)

        val result = engine.duel(weak, tank, emptySet())

        assertTrue(result.strikes.all { it.damage >= 1 })
    }

    @Test
    fun `duel_superEffectiveHitsHarderThanNotVeryEffective`() {
        val water = mon(type = "water", speed = 120, attack = 80, hp = 400)
        val fireEnemy  = mon(type = "fire",  speed = 10, hp = 9_000, defense = 1, name = "fuego")
        val waterEnemy = mon(type = "water", speed = 10, hp = 9_000, defense = 1, name = "agua")

        val superEff = engine.duel(water, fireEnemy, emptySet()).strikes.first()
        val notEff   = engine.duel(water, waterEnemy, emptySet()).strikes.first()

        assertTrue(superEff.damage > notEff.damage, "agua sobre fuego (x2) pega más que sobre agua (x0.5)")
        assertEquals(2f, superEff.effectiveness)
    }

    @Test
    fun `duel_vampirismHealsThePlayer`() {
        val player = mon(attack = 9_999, speed = 120).damaged(100)
        val enemy  = mon(hp = 10, speed = 10, defense = 1)

        val healed   = engine.duel(player, enemy, setOf(RogueBlessing.VAMPIRISMO))
        val unhealed = engine.duel(player, enemy, emptySet())

        assertTrue(healed.player.currentHp > unhealed.player.currentHp)
    }

    @Test
    fun `duel_furyIncreasesDamage`() {
        val player = mon(speed = 120, hp = 400)
        val enemy  = mon(speed = 10, hp = 9_000, defense = 1)

        val withFury    = engine.duel(player, enemy, setOf(RogueBlessing.FURIA)).strikes.first()
        val withoutFury = engine.duel(player, enemy, emptySet()).strikes.first()

        assertTrue(withFury.damage > withoutFury.damage)
    }

    @Test
    fun `duel_impetusBlessingWinsTheSpeedRace`() {
        val player = mon(speed = 100, hp = 400, defense = 999)
        val enemy  = mon(speed = 110, hp = 400, defense = 999)

        val result = engine.duel(player, enemy, setOf(RogueBlessing.IMPETU))

        assertTrue(result.strikes.first().isPlayerAttack,
                   "con +30% de velocidad, 100 le gana a 110")
    }

    @Test
    fun `duel_recordsRemainingHpConsistently`() {
        val player = mon(speed = 120, hp = 400)
        val enemy  = mon(speed = 10, hp = 9_000, defense = 1)

        val strike = engine.duel(player, enemy, emptySet()).strikes.first()

        assertEquals(strike.defenderMaxHp - strike.damage, strike.defenderHpAfter)
    }
}
