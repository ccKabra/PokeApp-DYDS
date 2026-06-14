package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.entity.RogueBlessing
import com.pokemonarena.domain.entity.RogueMove
import com.pokemonarena.domain.entity.RogueMoves
import com.pokemonarena.domain.entity.RoguePokemon
import com.pokemonarena.domain.entity.RogueRules
import com.pokemonarena.domain.entity.TypeMatchup
import kotlin.math.roundToInt
import kotlin.random.Random

data class RogueStrike(
    val attackerName:    String,
    val defenderName:    String,
    val moveName:        String,
    val damage:          Int,
    val effectiveness:   Float,
    val isPlayerAttack:  Boolean,
    val defenderFainted: Boolean,
    val defenderHpAfter: Int,
    val defenderMaxHp:   Int
)

data class RogueDuel(
    val player:        RoguePokemon,
    val enemy:         RoguePokemon,
    val strikes:       List<RogueStrike>,
    val playerFainted: Boolean,
    val enemyFainted:  Boolean
)

class RogueBattleEngine(private val random: Random = Random.Default) {

    fun duel(player: RoguePokemon, enemy: RoguePokemon, blessings: Set<RogueBlessing>): RogueDuel {
        var p = player
        var e = enemy
        val strikes = mutableListOf<RogueStrike>()
        val playerFirst = effectiveSpeed(p, blessings) >= e.speed

        var guard = 0
        while (p.isAlive && e.isAlive && guard++ < RogueRules.DUEL_ROUND_CAP) {
            if (playerFirst) {
                strike(p, e, isPlayer = true, blessings, strikes).let { (a, d) -> p = a; e = d }
                if (!e.isAlive) break
                strike(e, p, isPlayer = false, blessings, strikes).let { (a, d) -> e = a; p = d }
            } else {
                strike(e, p, isPlayer = false, blessings, strikes).let { (a, d) -> e = a; p = d }
                if (!p.isAlive) break
                strike(p, e, isPlayer = true, blessings, strikes).let { (a, d) -> p = a; e = d }
            }
        }
        return RogueDuel(p, e, strikes, playerFainted = !p.isAlive, enemyFainted = !e.isAlive)
    }

    private fun strike(attacker: RoguePokemon, defender: RoguePokemon, isPlayer: Boolean,
                       blessings: Set<RogueBlessing>, log: MutableList<RogueStrike>): Pair<RoguePokemon, RoguePokemon> {
        val move        = bestMove(attacker, defender)
        val (damage, eff) = damageOf(attacker, defender, move, if (isPlayer) blessings else emptySet())
        val hit         = defender.damaged(damage)

        var updatedAttacker = attacker
        if (isPlayer && RogueBlessing.VAMPIRISMO in blessings) {
            val heal = (damage * RogueRules.LIFESTEAL_FRACTION).toInt()
            updatedAttacker = attacker.copy(currentHp = (attacker.currentHp + heal).coerceAtMost(attacker.maxHp))
        }

        log += RogueStrike(attacker.species.displayName, defender.species.displayName, move.name,
                           damage, eff, isPlayerAttack = isPlayer, defenderFainted = !hit.isAlive,
                           defenderHpAfter = hit.currentHp, defenderMaxHp = hit.maxHp)
        return updatedAttacker to hit
    }

    private fun bestMove(attacker: RoguePokemon, defender: RoguePokemon): RogueMove =
        attacker.moves.maxByOrNull { move ->
            move.power *
                TypeMatchup.effectiveness(move.type, defender.species.primaryType) *
                stabFor(move, attacker)
        } ?: RogueMoves.BASIC

    private fun damageOf(attacker: RoguePokemon, defender: RoguePokemon,
                         move: RogueMove, blessings: Set<RogueBlessing>): Pair<Int, Float> {
        val eff      = TypeMatchup.effectiveness(move.type, defender.species.primaryType)
        val stab     = stabFor(move, attacker)
        val variance = RogueRules.MIN_VARIANCE + random.nextFloat() * RogueRules.VARIANCE_RANGE
        val fury     = if (RogueBlessing.FURIA in blessings) RogueRules.DAMAGE_BLESSING else 1f
        val raw = attacker.attack * move.power * eff * stab * variance * fury -
                  defender.defense * RogueRules.DEFENSE_FACTOR
        return raw.roundToInt().coerceAtLeast(1) to eff
    }

    private fun stabFor(move: RogueMove, attacker: RoguePokemon): Float =
        if (move.type in attacker.species.types) RogueRules.STAB_MULTIPLIER else 1f

    private fun effectiveSpeed(pokemon: RoguePokemon, blessings: Set<RogueBlessing>): Float =
        pokemon.speed * (if (RogueBlessing.IMPETU in blessings) RogueRules.SPEED_BLESSING else 1f)
}
