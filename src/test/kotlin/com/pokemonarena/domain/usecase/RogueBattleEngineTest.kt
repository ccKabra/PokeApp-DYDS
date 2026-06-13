package com.pokemonarena.domain.usecase

import com.pokemonarena.FixedRandom
import com.pokemonarena.domain.entity.RogueBlessing
import com.pokemonarena.domain.entity.RogueMove
import com.pokemonarena.domain.entity.RoguePokemon
import com.pokemonarena.domain.entity.RogueRules
import com.pokemonarena.domain.entity.RogueSpecies
import com.pokemonarena.domain.entity.Stats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RogueBattleEngineTest {

    private val engine = RogueBattleEngine(FixedRandom(0.5f))

    private fun pokemonOf(attack: Int = 80, defense: Int = 60, speed: Int = 80,
                          hp: Int = 80, type: String = "normal", name: String = "mon") =
        RoguePokemon.of(RogueSpecies(1, name, "", listOf(type),
                                     Stats(hp, attack, defense, attack, defense, speed), 1))

    private fun move(type: String, power: Float = 1f) = RogueMove("prueba", type, power)

    @Test
    fun `exchange_fasterPokemonStrikesFirst`() {
        val fast = pokemonOf(speed = 120, name = "veloz")
        val slow = pokemonOf(speed = 30, name = "lento")

        val result = engine.exchange(fast, move("normal"), slow, emptySet())

        assertTrue(result.strikes.first().isPlayerAttack)
        assertEquals("Veloz", result.strikes.first().attackerName)
    }

    @Test
    fun `exchange_enemyNeverFaintsEvenUnderLethalDamage`() {
        val killer = pokemonOf(attack = 99_999, speed = 120)
        val victim = pokemonOf(hp = 10, speed = 10, defense = 1)

        repeat(50) {
            val result = engine.exchange(killer, move("normal", 9f), victim, emptySet())
            assertTrue(result.enemy.isAlive, "el rival nunca puede ser derrotado")
            assertTrue(result.enemy.currentHp >= RogueRules.ENEMY_HP_FLOOR)
            assertTrue(result.strikes.none { it.isPlayerAttack && it.defenderFainted },
                       "ningún golpe del jugador puede debilitar al rival")
        }
    }

    @Test
    fun `exchange_lethalBlowEnragesHealsAndTaunts`() {
        val killer = pokemonOf(attack = 99_999, speed = 120)
        val victim = pokemonOf(hp = 10, speed = 10, defense = 1)

        val result = engine.exchange(killer, move("normal", 9f), victim, emptySet())

        assertTrue(result.enemyEnraged, "un golpe letal lo enfurece")
        assertTrue(result.taunt != null, "y suelta una burla")
        assertTrue(result.enemy.currentHp > RogueRules.ENEMY_HP_FLOOR, "se cura al enfurecerse")
    }

    @Test
    fun `exchange_enemyAlwaysCounterattacks`() {
        val fast  = pokemonOf(speed = 120)
        val enemy = pokemonOf(speed = 10, hp = 300)

        val result = engine.exchange(fast, move("normal"), enemy, emptySet())

        assertEquals(2, result.strikes.size, "ningún KO evita el contraataque del rival")
        assertTrue(result.strikes[0].isPlayerAttack)
        assertTrue(!result.strikes[1].isPlayerAttack)
    }

    @Test
    fun `exchange_enemyRampsUpEachTurn`() {
        val player = pokemonOf(speed = 120, hp = 500)
        val enemy  = pokemonOf(speed = 10, hp = 400)

        val after = engine.exchange(player, move("normal"), enemy, emptySet()).enemy

        assertTrue(after.attack > enemy.attack, "el rival se vuelve más letal cada turno")
    }

    @Test
    fun `exchange_slowerPlayerReceivesTheFirstHit`() {
        val slowPlayer = pokemonOf(speed = 10)
        val fastEnemy  = pokemonOf(speed = 120)

        val result = engine.exchange(slowPlayer, move("normal"), fastEnemy, emptySet())

        assertTrue(!result.strikes.first().isPlayerAttack)
    }

    @Test
    fun `exchange_damageIsAtLeastOne`() {
        val weak = pokemonOf(attack = 1)
        val tank = pokemonOf(defense = 999, speed = 10)

        val result = engine.exchange(weak, move("normal", 0.1f), tank, emptySet())

        assertTrue(result.strikes.all { it.damage >= 1 })
    }

    @Test
    fun `exchange_recordsTheMoveNameAndRemainingHp`() {
        val player = pokemonOf(speed = 120)
        val enemy  = pokemonOf(speed = 10, hp = 300)

        val strike = engine.exchange(player, move("fire"), enemy, emptySet()).strikes.first()

        assertEquals("prueba", strike.moveName)
        assertEquals(strike.defenderHpAfter, strike.defenderMaxHp - strike.damage)
    }

    @Test
    fun `exchange_vampirismHealsThePlayer`() {
        val player = pokemonOf(speed = 120).damaged(100)
        val enemy  = pokemonOf(speed = 10, hp = 300)

        val healed   = engine.exchange(player, move("normal"), enemy, setOf(RogueBlessing.VAMPIRISMO))
        val unhealed = engine.exchange(player, move("normal"), enemy, emptySet())

        assertTrue(healed.player.currentHp > unhealed.player.currentHp)
    }

    @Test
    fun `exchange_furyIncreasesDamage`() {
        val player = pokemonOf(speed = 120)
        val enemy  = pokemonOf(speed = 10, hp = 300)

        val withFury    = engine.exchange(player, move("normal"), enemy, setOf(RogueBlessing.FURIA))
        val withoutFury = engine.exchange(player, move("normal"), enemy, emptySet())

        assertTrue(withFury.strikes.first().damage > withoutFury.strikes.first().damage)
    }

    @Test
    fun `exchange_stabBoostsDamageForMatchingType`() {
        val fire = pokemonOf(type = "fire", speed = 120)
        val enemy = pokemonOf(type = "normal", speed = 10, hp = 400)

        val stab   = engine.exchange(fire, move("fire"), enemy, emptySet())
        val offType = engine.exchange(fire, move("normal"), enemy, emptySet())

        assertTrue(stab.strikes.first().damage > offType.strikes.first().damage,
                   "un ataque del propio tipo pega más por STAB")
    }

    @Test
    fun `exchange_impetusBlessingWinsTheSpeedTie`() {
        val player = pokemonOf(speed = 100)
        val enemy  = pokemonOf(speed = 110)

        val result = engine.exchange(player, move("normal"), enemy, setOf(RogueBlessing.IMPETU))

        assertTrue(result.strikes.first().isPlayerAttack,
                   "con +30% de velocidad, 100 le gana a 110")
    }

    @Test
    fun `enemyFreeStrike_hitsOnlyTheIncomingPokemon`() {
        val incoming = pokemonOf()
        val enemy    = pokemonOf(attack = 100)

        val result = engine.enemyFreeStrike(incoming, enemy)

        assertEquals(1, result.strikes.size)
        assertTrue(!result.strikes.single().isPlayerAttack)
        assertTrue(result.player.currentHp < incoming.currentHp)
        assertEquals(enemy, result.enemy)
    }
}
