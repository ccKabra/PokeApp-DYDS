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

    /** Cura un porcentaje del HP máximo. Los debilitados no se recuperan. */
    fun healedBy(fraction: Float): RoguePokemon =
        if (!isAlive) this
        else copy(currentHp = (currentHp + (maxHp * fraction).toInt()).coerceAtMost(maxHp))

    fun withMaxHpBoost(factor: Float): RoguePokemon {
        val newMax = (maxHp * factor).toInt()
        return copy(maxHp = newMax,
                    currentHp = if (isAlive) currentHp + newMax - maxHp else 0)
    }

    /** El rival absorbió un golpe letal: se cura y se potencia. La burla cósmica del modo. */
    fun enraged(): RoguePokemon = copy(
        currentHp = (currentHp + (maxHp * RogueRules.ENRAGE_HEAL_FRACTION).toInt()).coerceAtMost(maxHp),
        attack    = (attack * RogueRules.ENRAGE_ATTACK_GROWTH).toInt().coerceAtLeast(attack + 1),
        speed     = (speed  * RogueRules.ENRAGE_SPEED_GROWTH).toInt().coerceAtLeast(speed)
    )

    /** Escalada por turno: el rival se vuelve más letal cuanto más te quedás. */
    fun rampedUp(): RoguePokemon =
        copy(attack = (attack * RogueRules.ENEMY_TURN_RAMP).toInt().coerceAtLeast(attack + 1))

    /** Evoluciona a una nueva especie: adopta sus stats al nivel actual, preservando
     *  la proporción de HP, la XP, el item equipado y los ataques aprendidos. */
    fun evolveInto(newSpecies: RogueSpecies): RoguePokemon {
        val ratio   = hpFraction
        val base    = of(newSpecies, level).copy(xp = xp, moves = moves)
        val geared  = item?.let { base.equip(it) } ?: base
        return geared.copy(
            currentHp = if (isAlive) (geared.maxHp * ratio).toInt().coerceAtLeast(1) else 0)
    }

    /** Equipa un item, aplicando sus multiplicadores sobre las stats actuales. */
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

    /** Suma XP, resolviendo todas las subidas de nivel encadenadas (stats + ataques nuevos). */
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
        // El nivel reconstruye stats desde la base: re-aplicamos el item para no perderlo.
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
    FORTUNA("Amuleto de la Fortuna", "Todo el botín de monedas que juntes aumenta un 50%.")
}

enum class RogueNodeType(val displayName: String, val description: String) {
    FIGHT("Combate", "Un Pokémon salvaje te cierra el paso. Botín y experiencia normales."),
    ELITE("Élite", "Un rival temible: pega más fuerte, pero deja botín y XP dobles."),
    REST("Descanso", "Una fogata segura: los Pokémon en pie recuperan parte del HP."),
    TREASURE("Tesoro", "Un cofre olvidado lleno de monedas."),
    DOJO("Dojo", "Entrenás a fondo: todo el equipo gana experiencia y puede subir de nivel."),
    BOSS("Jefe Final", "El guardián legendario de la cima. Vencelo y cobrá todo con bonus.")
}

object RogueRules {
    const val FLOORS        = 12   // caminos más largos: más tiempo para reclutar y evolucionar
    const val TEAM_CAPACITY = 3
    const val DRAFT_SIZE    = 3
    const val HP_FACTOR     = 3

    const val DEFENSE_FACTOR  = 0.65f   // la defensa mitiga más: los items/tanques importan
    const val MIN_VARIANCE    = 0.85f
    const val VARIANCE_RANGE  = 0.30f
    const val STAB_MULTIPLIER = 1.2f

    const val BASE_LEVEL   = 5
    const val MOVE_CAP     = 4
    const val LEVEL_GROWTH = 0.06f

    const val FLOOR_SCALE_STEP = 0.08f
    const val ELITE_MULTIPLIER = 1.35f

    const val DAMAGE_BLESSING    = 1.25f
    const val HP_BLESSING        = 1.25f
    const val SPEED_BLESSING     = 1.30f
    const val LIFESTEAL_FRACTION = 0.25f
    const val LOOT_BLESSING      = 1.5f

    const val REST_HEAL_FRACTION   = 0.45f
    const val REWARD_HEAL_FRACTION = 0.40f
    const val DOJO_HEAL_FRACTION   = 0.30f   // el dojo además recupera algo de HP

    const val BOSS_BONUS             = 250
    const val DEFEAT_PAYOUT_FRACTION = 0.5f

    // ─────────────────── Combate Imposible (Modo Maldito) ───────────────────
    // El rival tiene "Armadura Argumental": no se lo puede derrotar NUNCA. La victoria por
    // KO es inalcanzable por diseño y la imposibilidad está cubierta por tests. El jugador
    // no puede ganar, pero SÍ puede sobrevivir y huir (salvo del jefe). Para "des-maldecir"
    // el modo habría que reintroducir una rama de victoria en RogueViewModel; no hay flag
    // mágico porque la frustración es deliberada y debe quedar a la vista.
    const val ENEMY_HP_FLOOR       = 1       // el rival jamás baja de 1 HP
    const val ENRAGE_HEAL_FRACTION = 0.5f    // al absorber un golpe letal, se cura medio HP
    const val ENRAGE_ATTACK_GROWTH = 1.20f   // ...y pega un 20% más fuerte
    const val ENRAGE_SPEED_GROWTH  = 1.10f
    const val ENEMY_TURN_RAMP      = 1.045f  // +4.5% de ataque por turno: insistir castiga, pero da aire
    const val TURNS_TO_ESCAPE      = 6       // sobrevivir N turnos habilita la huida (alcanzable con buen juego)
    const val HOPE_TOKENS_START    = 3       // recurso escaso para huir antes de tiempo
    const val SURVIVAL_XP_FACTOR   = 0.6f    // huir da algo de XP, pero menos que "ganar"

    val ENRAGE_TAUNTS = listOf(
        "El rival se rió en tu cara y se levantó de un salto.",
        "—¿En serio pensaste que ibas a ganar? —se burló el rival.",
        "Plot twist: el rival tenía Armadura Argumental™.",
        "El rival usó SEGUNDO AIRE… y un tercero, y un cuarto.",
        "Las reglas del juego te miran y niegan con la cabeza.",
        "Casi. Pero 'casi' no cuenta, y el rival lo sabe muy bien.",
        "El rival sacó 1 HP de la galera. Cosas del guion.",
        "Esto no es un error: es una característica narrativa.",
        "El rival ya leyó el final del libro. Spoiler: gana él."
    )

    fun levelScale(level: Int): Float = 1f + LEVEL_GROWTH * (level - BASE_LEVEL)

    fun xpToNext(level: Int): Int = 10 + level * 4

    fun xpReward(floor: Int, node: RogueNodeType): Int {
        val base = 8 + floor * 3
        return if (node == RogueNodeType.ELITE || node == RogueNodeType.BOSS) base * 2 else base
    }

    fun dojoXp(floor: Int): Int = 40 + floor * 8   // el dojo ahora empuja niveles y evoluciones

    fun tierForFloor(floor: Int): Int = when {
        floor <= 4 -> 1
        floor <= 8 -> 2
        else       -> 3
    }

    fun enemyScaleFor(floor: Int, node: RogueNodeType): Float = when (node) {
        RogueNodeType.BOSS  -> 1f
        RogueNodeType.ELITE -> baseScale(floor) * ELITE_MULTIPLIER
        else                -> baseScale(floor)
    }

    private fun baseScale(floor: Int): Float = 1f + FLOOR_SCALE_STEP * (floor - 1)

    fun victoryLoot(floor: Int, node: RogueNodeType): Int {
        val base = 12 + 5 * floor
        return when (node) {
            RogueNodeType.ELITE, RogueNodeType.BOSS -> base * 2
            else                                    -> base
        }
    }

    fun treasureLoot(roll: Float): Int = 25 + (roll.coerceIn(0f, 1f) * 35).toInt()

    fun lootWith(blessings: Set<RogueBlessing>, base: Int): Int =
        if (RogueBlessing.FORTUNA in blessings) (base * LOOT_BLESSING).toInt() else base

    fun payout(loot: Int, victory: Boolean): Int =
        if (victory) loot + BOSS_BONUS
        else (loot * DEFEAT_PAYOUT_FRACTION).toInt()
}
