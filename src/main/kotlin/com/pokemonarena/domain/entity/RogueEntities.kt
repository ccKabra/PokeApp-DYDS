package com.pokemonarena.domain.entity

data class RogueSpecies(
    val pokeId:   Int,
    val name:     String,
    val imageUrl: String,
    val types:    List<String>,
    val stats:    Stats,
    val tier:     Int
) {
    val displayName: String get() = name.replaceFirstChar { it.uppercase() }
    val primaryType: String get() = types.firstOrNull() ?: "normal"

    companion object {
        const val BOSS_TIER = 4
    }
}

data class RoguePokemon(
    val species:   RogueSpecies,
    val level:     Int,
    val xp:        Int,
    val maxHp:     Int,
    val currentHp: Int,
    val attack:    Int,
    val defense:   Int,
    val speed:     Int,
    val moves:     List<RogueMove>,
    val item:      RogueItem? = null
) {
    val isAlive:     Boolean get() = currentHp > 0
    val hpFraction:  Float   get() = if (maxHp == 0) 0f else currentHp / maxHp.toFloat()
    val xpFraction:  Float   get() = (xp / RogueRules.xpToNext(level).toFloat()).coerceIn(0f, 1f)

    fun damaged(amount: Int): RoguePokemon =
        copy(currentHp = (currentHp - amount).coerceAtLeast(0))

    fun healedBy(fraction: Float): RoguePokemon =
        if (!isAlive) this
        else copy(currentHp = (currentHp + (maxHp * fraction).toInt()).coerceAtMost(maxHp))

    fun withMaxHpBoost(factor: Float): RoguePokemon {
        val newMax = (maxHp * factor).toInt()
        return copy(maxHp = newMax,
                    currentHp = if (isAlive) currentHp + newMax - maxHp else 0)
    }

    fun evolveInto(newSpecies: RogueSpecies): RoguePokemon {
        val ratio   = hpFraction
        val base    = of(newSpecies, level).copy(xp = xp, moves = moves)
        val geared  = item?.let { base.equip(it) } ?: base
        return geared.copy(
            currentHp = if (isAlive) (geared.maxHp * ratio).toInt().coerceAtLeast(1) else 0)
    }

    fun withGear(gear: RogueItem?): RoguePokemon {
        val ratio  = hpFraction
        val base   = of(species, level).copy(xp = xp, moves = moves)
        val geared = gear?.let { base.equip(it) } ?: base
        return geared.copy(
            currentHp = if (isAlive) (geared.maxHp * ratio).toInt().coerceAtLeast(1) else 0)
    }

    fun equip(gear: RogueItem): RoguePokemon {
        val newMax = (maxHp * gear.hpMult).toInt().coerceAtLeast(1)
        return copy(
            item      = gear,
            maxHp     = newMax,
            currentHp = if (isAlive) (currentHp + (newMax - maxHp)).coerceIn(1, newMax) else 0,
            attack    = (attack * gear.attackMult).toInt().coerceAtLeast(1),
            defense   = (defense * gear.defenseMult).toInt(),
            speed     = (speed * gear.speedMult).toInt()
        )
    }

    fun gainingXp(amount: Int): LevelUpOutcome {
        var lvl = level
        var acc = xp + amount
        var gained = 0
        var movePool = moves
        val learned = mutableListOf<RogueMove>()

        while (acc >= RogueRules.xpToNext(lvl)) {
            acc -= RogueRules.xpToNext(lvl)
            lvl++
            gained++
            RogueMoves.nextLearnable(movePool, species.types, lvl)?.let { newMove ->
                movePool = if (movePool.size < RogueRules.MOVE_CAP) movePool + newMove
                           else movePool.sortedBy { it.power }.drop(1) + newMove
                learned += newMove
            }
        }

        if (gained == 0) return LevelUpOutcome(copy(xp = acc), 0, emptyList())

        val leveled    = of(species, lvl)
        val newCurrent = (currentHp + (leveled.maxHp - maxHp)).coerceIn(1, leveled.maxHp)
        var rebuilt    = leveled.copy(currentHp = newCurrent, xp = acc, moves = movePool)
        if (item != null) rebuilt = rebuilt.equip(item)
        return LevelUpOutcome(rebuilt, gained, learned)
    }

    companion object {
        fun of(species: RogueSpecies, level: Int = RogueRules.BASE_LEVEL, scale: Float = 1f): RoguePokemon {
            val stats = species.stats.scaledBy(scale * RogueRules.levelScale(level))
            val hp    = stats.hp * RogueRules.HP_FACTOR
            return RoguePokemon(
                species   = species,
                level     = level,
                xp        = 0,
                maxHp     = hp,
                currentHp = hp,
                attack    = maxOf(stats.attack, stats.specialAttack),
                defense   = (stats.defense + stats.specialDefense) / 2,
                speed     = stats.speed,
                moves     = RogueMoves.starterSet(species.types)
            )
        }

        fun enemyOf(species: RogueSpecies, scale: Float): RoguePokemon =
            of(species, scale = scale).copy(moves = RogueMoves.enemySet(species.types))
    }
}

data class LevelUpOutcome(
    val pokemon:      RoguePokemon,
    val levelsGained: Int,
    val learnedMoves: List<RogueMove>
)

enum class RogueBlessing(val displayName: String, val description: String) {
    FURIA("Furia Salvaje", "Tus Pokémon hacen +25% de daño."),
    AGUANTE("Aguante Férreo", "+25% de HP máximo para los Pokémon en pie."),
    IMPETU("Ímpetu", "+30% de Velocidad: golpeás primero mucho más seguido."),
    VAMPIRISMO("Colmillo Vampiro", "Tus Pokémon se curan el 25% del daño que hacen."),
    FORTUNA("Amuleto de la Fortuna", "Todo el oro que juntes en la run aumenta un 50%.")
}

object RogueRules {
    const val TEAM_CAPACITY = 6
    const val DRAFT_SIZE    = 3
    const val HP_FACTOR     = 3

    const val ACTS          = 3
    const val ROWS_PER_ACT  = 8
    const val MIN_ROW_WIDTH = 2
    const val MAX_ROW_WIDTH = 4

    const val DEFENSE_FACTOR  = 0.5f
    const val MIN_VARIANCE    = 0.85f
    const val VARIANCE_RANGE  = 0.30f
    const val STAB_MULTIPLIER = 1.2f
    const val DUEL_ROUND_CAP  = 100

    const val BASE_LEVEL   = 5
    const val MOVE_CAP     = 4
    const val LEVEL_GROWTH = 0.06f

    const val ENEMY_BASE_SCALE  = 0.9f
    const val SCALE_STEP        = 0.05f
    const val BOSS_SCALE_STEP   = 0.10f

    const val DAMAGE_BLESSING    = 1.25f
    const val HP_BLESSING        = 1.25f
    const val SPEED_BLESSING     = 1.30f
    const val LIFESTEAL_FRACTION = 0.25f
    const val LOOT_BLESSING      = 1.5f

    const val CENTER_HEAL_FRACTION   = 1.0f
    const val REWARD_HEAL_FRACTION   = 0.40f
    const val POST_FIGHT_HEAL_FRACTION = 0.20f

    const val CHAMPION_BONUS = 400

    fun levelScale(level: Int): Float = 1f + LEVEL_GROWTH * (level - BASE_LEVEL)

    fun xpToNext(level: Int): Int = 10 + level * 4

    fun depthOf(act: Int, row: Int): Int = (act - 1) * (ROWS_PER_ACT + 1) + row + 1

    fun tierForAct(act: Int): Int = act.coerceIn(1, 3)

    fun recruitTier(act: Int): Int = (tierForAct(act) + 1).coerceAtMost(3)

    fun enemyScale(depth: Int): Float = ENEMY_BASE_SCALE + SCALE_STEP * (depth - 1)

    fun bossScale(act: Int): Float = 1f + BOSS_SCALE_STEP * (act - 1)

    fun bossTierForAct(act: Int): Int = if (act >= ACTS) RogueSpecies.BOSS_TIER else 3

    fun bossTeamSize(act: Int): Int = act.coerceIn(1, 2)

    fun fightXp(depth: Int): Int = 30 + depth * 8
    fun bossXp(act: Int): Int    = 120 + act * 40

    fun fightLoot(depth: Int): Int = 12 + depth * 6
    fun bossLoot(act: Int): Int    = 120 + act * 60
    fun goldChest(roll: Float, depth: Int): Int = 25 + depth * 4 + (roll.coerceIn(0f, 1f) * 30).toInt()

    fun lootWith(blessings: Set<RogueBlessing>, base: Int): Int =
        if (RogueBlessing.FORTUNA in blessings) (base * LOOT_BLESSING).toInt() else base

    fun payout(loot: Int, victory: Boolean): Int =
        if (victory) loot + CHAMPION_BONUS else loot
}
