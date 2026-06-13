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
    val typeMultiplier:  Float,
    val isPlayerAttack:  Boolean,
    val defenderFainted: Boolean,
    val defenderHpAfter: Int,
    val defenderMaxHp:   Int
)

data class RogueExchange(
    val player:       RoguePokemon,
    val enemy:        RoguePokemon,
    val strikes:      List<RogueStrike>,
    val enemyEnraged: Boolean = false,
    val taunt:        String?  = null
)

/**
 * Motor del "Combate Imposible".
 *
 * REGLA DE DISEÑO INVIOLABLE: el rival tiene **Armadura Argumental** y JAMÁS puede ser
 * derrotado. Todo golpe que sería letal lo deja en [RogueRules.ENEMY_HP_FLOOR] HP y lo
 * enfurece (se cura y pega más fuerte). Además el rival escala cada turno. La victoria por
 * KO es, por lo tanto, estructuralmente inalcanzable: ningún [RogueStrike] del jugador puede
 * marcar `defenderFainted = true`. La frustración es intencional. Ver [RogueRules].
 */
class RogueBattleEngine(private val random: Random = Random.Default) {

    /** Un turno completo. El rival SIEMPRE contraataca: no hay KO que lo impida. */
    fun exchange(player: RoguePokemon, playerMove: RogueMove, enemy: RoguePokemon,
                 blessings: Set<RogueBlessing>): RogueExchange {
        var p = player
        var e = enemy
        val enemyMove = enemy.moves.randomOrNull(random) ?: RogueMoves.BASIC
        val strikes = mutableListOf<RogueStrike>()
        var enraged = false

        fun playerAttacks() {
            val boost = if (RogueBlessing.FURIA in blessings) RogueRules.DAMAGE_BLESSING else 1f
            val (damage, mult) = resolve(p, e, playerMove, boost)
            // Armadura Argumental: el rival nunca baja del piso de HP.
            val wouldBeHp = e.currentHp - damage
            e = e.copy(currentHp = wouldBeHp.coerceAtLeast(RogueRules.ENEMY_HP_FLOOR))
            if (wouldBeHp <= RogueRules.ENEMY_HP_FLOOR) {
                enraged = true
                e = e.enraged()   // "¿Creíste que ganabas?": se cura y se potencia.
            }
            if (RogueBlessing.VAMPIRISMO in blessings) {
                val heal = (damage * RogueRules.LIFESTEAL_FRACTION).toInt()
                p = p.copy(currentHp = (p.currentHp + heal).coerceAtMost(p.maxHp))
            }
            strikes += RogueStrike(p.species.displayName, e.species.displayName, playerMove.name,
                                   damage, mult, isPlayerAttack = true, defenderFainted = false,
                                   defenderHpAfter = e.currentHp, defenderMaxHp = e.maxHp)
        }

        fun enemyAttacks() {
            val (damage, mult) = resolve(e, p, enemyMove, boost = 1f)
            p = p.damaged(damage)
            strikes += RogueStrike(e.species.displayName, p.species.displayName, enemyMove.name,
                                   damage, mult, isPlayerAttack = false, defenderFainted = !p.isAlive,
                                   defenderHpAfter = p.currentHp, defenderMaxHp = p.maxHp)
        }

        if (effectiveSpeed(p, blessings) >= e.speed) {
            playerAttacks()
            enemyAttacks()
        } else {
            enemyAttacks()
            if (p.isAlive) playerAttacks()
        }

        // El rival se vuelve más letal con cada turno: la presión empuja a huir, no a insistir.
        e = e.rampedUp()
        val taunt = if (enraged) RogueRules.ENRAGE_TAUNTS.randomOrNull(random) else null
        return RogueExchange(p, e, strikes, enemyEnraged = enraged, taunt = taunt)
    }

    /** Golpe gratis del rival al Pokémon entrante en un cambio voluntario. */
    fun enemyFreeStrike(incoming: RoguePokemon, enemy: RoguePokemon): RogueExchange {
        val move = enemy.moves.randomOrNull(random) ?: RogueMoves.BASIC
        val (damage, mult) = resolve(enemy, incoming, move, boost = 1f)
        val hit = incoming.damaged(damage)
        val strike = RogueStrike(enemy.species.displayName, incoming.species.displayName, move.name,
                                 damage, mult, isPlayerAttack = false, defenderFainted = !hit.isAlive,
                                 defenderHpAfter = hit.currentHp, defenderMaxHp = hit.maxHp)
        return RogueExchange(hit, enemy, listOf(strike))
    }

    private fun effectiveSpeed(pokemon: RoguePokemon, blessings: Set<RogueBlessing>): Float =
        pokemon.speed * (if (RogueBlessing.IMPETU in blessings) RogueRules.SPEED_BLESSING else 1f)

    private fun resolve(attacker: RoguePokemon, defender: RoguePokemon,
                        move: RogueMove, boost: Float): Pair<Int, Float> {
        val mult     = TypeMatchup.multiplier(move.type, defender.species.primaryType)
        val stab     = if (move.type in attacker.species.types) RogueRules.STAB_MULTIPLIER else 1f
        val variance = RogueRules.MIN_VARIANCE + random.nextFloat() * RogueRules.VARIANCE_RANGE
        val raw = attacker.attack * move.power * mult * stab * variance * boost -
                  defender.defense * RogueRules.DEFENSE_FACTOR
        return raw.roundToInt().coerceAtLeast(1) to mult
    }
}
