package com.pokemonarena.presentation.screens.rogue

import com.pokemonarena.domain.entity.RogueBlessing
import com.pokemonarena.domain.entity.RogueEvolutions
import com.pokemonarena.domain.entity.RogueItems
import com.pokemonarena.domain.entity.RogueNodeType
import com.pokemonarena.domain.entity.RoguePokemon
import com.pokemonarena.domain.entity.RogueRules
import com.pokemonarena.domain.entity.RogueSpecies
import com.pokemonarena.domain.usecase.CashOutRogueRunUseCase
import com.pokemonarena.domain.usecase.GetRoguePoolUseCase
import com.pokemonarena.domain.usecase.RogueBattleEngine
import com.pokemonarena.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class RogueViewModel(
    private val getPool: GetRoguePoolUseCase,
    private val cashOut: CashOutRogueRunUseCase,
    private val random:  Random = Random.Default
) : BaseViewModel() {

    private val engine = RogueBattleEngine(random)

    private val _uiState = MutableStateFlow<RogueUiState>(RogueUiState.Idle)
    val uiState: StateFlow<RogueUiState> = _uiState.asStateFlow()

    private var pool: List<RogueSpecies> = emptyList()

    fun onEvent(event: RogueUiEvent) {
        when (event) {
            is RogueUiEvent.Start       -> scope.launch { start() }
            is RogueUiEvent.PickStarter -> pickStarter(event.index)
            is RogueUiEvent.PickNode    -> pickNode(event.index)
            is RogueUiEvent.Attack      -> attack(event.moveIndex)
            is RogueUiEvent.SetActive   -> setActive(event.index)
            is RogueUiEvent.PickReward  -> pickReward(event.index)
            is RogueUiEvent.EquipOn     -> equipOn(event.index)
            is RogueUiEvent.Flee        -> flee()
            is RogueUiEvent.SpendHope   -> spendHope()
            is RogueUiEvent.Abandon     -> abandon()
            is RogueUiEvent.BackToIdle  -> _uiState.value = RogueUiState.Idle
        }
    }

    private suspend fun start() {
        if (pool.isEmpty()) pool = runCatching { getPool.execute() }.getOrDefault(emptyList())
        if (pool.isEmpty()) return
        val starters = drawSpecies(tier = 1, count = RogueRules.DRAFT_SIZE).map { RoguePokemon.of(it) }
        _uiState.value = RogueUiState.Draft(starters)
    }

    private fun pickStarter(index: Int) {
        val s = _uiState.value as? RogueUiState.Draft ?: return
        val starter = s.starters.getOrNull(index) ?: return
        val run = RogueRunSnapshot(floor = 1, team = listOf(starter))
        _uiState.value = pathChoice(run, "Comienza el ascenso. ¡Elegí bien tu camino!")
    }

    private fun pickNode(index: Int) {
        val s = _uiState.value as? RogueUiState.PathChoice ?: return
        val node = s.options.getOrNull(index) ?: return
        when (node) {
            RogueNodeType.REST -> {
                val healed = s.run.team.map { it.healedBy(RogueRules.REST_HEAL_FRACTION) }
                advance(s.run.copy(team = healed), "Tu equipo descansó junto a la fogata y recuperó energías.")
            }
            RogueNodeType.TREASURE -> {
                val coins = RogueRules.lootWith(s.run.blessings, RogueRules.treasureLoot(random.nextFloat()))
                advance(s.run.copy(loot = s.run.loot + coins), "¡Encontraste un cofre con $coins monedas!")
            }
            RogueNodeType.DOJO -> {
                val (trained, notes) = grantXp(s.run.team, RogueRules.dojoXp(s.run.floor))
                val team = trained.map { it.healedBy(RogueRules.DOJO_HEAL_FRACTION) }
                val msg = if (notes.isEmpty()) "Entrenaste duro en el dojo y recuperaste algo de HP."
                          else "Entrenamiento en el dojo (+HP): " + notes.joinToString(" ")
                advance(s.run.copy(team = team), msg)
            }
            else -> startBattle(s.run, node)
        }
    }

    private fun startBattle(run: RogueRunSnapshot, node: RogueNodeType) {
        val tier    = if (node == RogueNodeType.BOSS) RogueSpecies.BOSS_TIER
                      else RogueRules.tierForFloor(run.floor)
        val species = drawSpecies(tier, count = 1).first()
        val enemy   = RoguePokemon.enemyOf(species, RogueRules.enemyScaleFor(run.floor, node))
        _uiState.value = RogueUiState.Battle(run.copy(activeIndex = firstAliveIndex(run.team)), node, enemy)
    }

    // El rival NO puede ser derrotado (Armadura Argumental): no existe rama de victoria.
    // El jugador solo puede sobrevivir, huir (salvo del jefe) o caer.
    private fun attack(moveIndex: Int) {
        val s = _uiState.value as? RogueUiState.Battle ?: return
        if (s.awaitingSwap) return
        val move     = s.active.moves.getOrNull(moveIndex) ?: return
        val result   = engine.exchange(s.active, move, s.enemy, s.run.blessings)
        val team     = s.run.team.toMutableList().also { it[s.run.activeIndex] = result.player }
        val run      = s.run.copy(team = team)
        val log      = (s.log + result.strikes).takeLast(LOG_LIMIT)
        val survived = s.turnsSurvived + 1
        val enrages  = s.enrageCount + if (result.enemyEnraged) 1 else 0
        when {
            !result.player.isAlive ->
                if (team.any { it.isAlive })
                    _uiState.value = s.copy(run = run, enemy = result.enemy, log = log,
                                            awaitingSwap = true, turnId = s.turnId + 1,
                                            lastTurn = result.strikes, turnsSurvived = survived,
                                            enrageCount = enrages, taunt = result.taunt)
                else finishRun(run)
            else -> _uiState.value = s.copy(run = run, enemy = result.enemy, log = log,
                                            turnId = s.turnId + 1, lastTurn = result.strikes,
                                            turnsSurvived = survived, enrageCount = enrages,
                                            taunt = result.taunt)
        }
    }

    private fun flee() {
        val s = _uiState.value as? RogueUiState.Battle ?: return
        if (!s.canFlee) return
        escape(s, s.run, "Aguantaste lo suficiente y lograste escapar con vida.")
    }

    private fun spendHope() {
        val s = _uiState.value as? RogueUiState.Battle ?: return
        if (!s.canSpendHope) return
        val run = s.run.copy(hopeTokens = s.run.hopeTokens - 1)
        escape(s, run, "Gastaste una Ficha de Esperanza y huiste de milagro. Te quedan ${run.hopeTokens}.")
    }

    /** Huir no es ganar: da algo de XP y un botín "carroñeado", y la expedición sigue. */
    private fun escape(s: RogueUiState.Battle, run: RogueRunSnapshot, message: String) {
        val xp = (RogueRules.xpReward(s.run.floor, s.node) * RogueRules.SURVIVAL_XP_FACTOR).toInt()
        val (team, notes) = grantXp(run.team, xp)
        val updated = run.copy(team = team)
        _uiState.value = RogueUiState.Reward(updated, rewardOptionsFor(updated), listOf(message) + notes)
    }

    private fun setActive(index: Int) {
        val s = _uiState.value as? RogueUiState.Battle ?: return
        val target = s.run.team.getOrNull(index) ?: return
        if (!target.isAlive || index == s.run.activeIndex) return

        if (s.awaitingSwap) {
            _uiState.value = s.copy(run = s.run.copy(activeIndex = index), awaitingSwap = false)
            return
        }
        val result = engine.enemyFreeStrike(target, s.enemy)
        val team   = s.run.team.toMutableList().also { it[index] = result.player }
        val run    = s.run.copy(team = team, activeIndex = index)
        val log    = (s.log + result.strikes).takeLast(LOG_LIMIT)
        when {
            result.player.isAlive   -> _uiState.value = s.copy(run = run, log = log,
                                            turnId = s.turnId + 1, lastTurn = result.strikes)
            team.any { it.isAlive }  -> _uiState.value = s.copy(run = run, log = log, awaitingSwap = true,
                                            turnId = s.turnId + 1, lastTurn = result.strikes)
            else                     -> finishRun(run)
        }
    }

    private fun pickReward(index: Int) {
        val s = _uiState.value as? RogueUiState.Reward ?: return
        when (val option = s.options.getOrNull(index) ?: return) {
            is RogueRewardOption.Recruit ->
                advance(s.run.copy(team = s.run.team + option.pokemon),
                        "¡${option.pokemon.species.displayName} se unió a tu expedición!")
            is RogueRewardOption.Heal ->
                advance(s.run.copy(team = s.run.team.map { it.healedBy(RogueRules.REWARD_HEAL_FRACTION) }),
                        "Tu equipo recuperó parte del HP.")
            is RogueRewardOption.Blessing ->
                advance(applyBlessing(s.run, option.blessing),
                        "Bendición obtenida: ${option.blessing.displayName}.")
            is RogueRewardOption.Loot ->
                advance(s.run.copy(loot = s.run.loot + option.coins),
                        "Sumaste ${option.coins} monedas al botín.")
            is RogueRewardOption.Gear ->
                _uiState.value = RogueUiState.EquipGear(s.run, option.item)
        }
    }

    private fun equipOn(index: Int) {
        val s = _uiState.value as? RogueUiState.EquipGear ?: return
        val target = s.run.team.getOrNull(index) ?: return
        val team = s.run.team.toMutableList().also { it[index] = target.equip(s.item) }
        advance(s.run.copy(team = team),
                "${target.species.displayName} equipó ${s.item.name}.")
    }

    private fun rewardOptionsFor(run: RogueRunSnapshot): List<RogueRewardOption> {
        val recruitLevel = run.team.maxOfOrNull { it.level } ?: RogueRules.BASE_LEVEL
        val candidates = buildList {
            if (run.team.size < RogueRules.TEAM_CAPACITY)
                add(RogueRewardOption.Recruit(
                    spawn(drawSpecies(RogueRules.tierForFloor(run.floor), 1).first(), recruitLevel)))
            add(RogueRewardOption.Heal)
            add(RogueRewardOption.Gear(RogueItems.random(random)))
            (RogueBlessing.entries - run.blessings).randomOrNull(random)
                ?.let { add(RogueRewardOption.Blessing(it)) }
            add(RogueRewardOption.Loot(
                RogueRules.lootWith(run.blessings, RogueRules.treasureLoot(random.nextFloat()))))
        }
        return candidates.shuffled(random).take(3)
    }

    private fun applyBlessing(run: RogueRunSnapshot, blessing: RogueBlessing): RogueRunSnapshot {
        val team = if (blessing == RogueBlessing.AGUANTE)
            run.team.map { it.withMaxHpBoost(RogueRules.HP_BLESSING) }
        else run.team
        return run.copy(team = team, blessings = run.blessings + blessing)
    }

    /** Reparte XP a los Pokémon en pie y arma los avisos de nivel / ataques / evoluciones. */
    private fun grantXp(team: List<RoguePokemon>, amount: Int): Pair<List<RoguePokemon>, List<String>> {
        val notes = mutableListOf<String>()
        val updated = team.map { member ->
            if (!member.isAlive) return@map member
            val outcome = member.gainingXp(amount)
            if (outcome.levelsGained > 0)
                notes += "${outcome.pokemon.species.displayName} subió a Nv ${outcome.pokemon.level}."
            outcome.learnedMoves.forEach {
                notes += "${outcome.pokemon.species.displayName} aprendió ${it.name}."
            }
            var pokemon = outcome.pokemon
            val evolved = evolvedSpecies(pokemon.species, pokemon.level)
            if (evolved.pokeId != pokemon.species.pokeId) {
                notes += "¡${pokemon.species.displayName} evolucionó a ${evolved.displayName}!"
                pokemon = pokemon.evolveInto(evolved)
            }
            pokemon
        }
        return updated to notes
    }

    /** Sigue la cadena evolutiva hasta donde el nivel lo permita (especies presentes en el pool). */
    private fun evolvedSpecies(species: RogueSpecies, level: Int): RogueSpecies {
        var current = species
        while (true) {
            val nextId = RogueEvolutions.nextStage(current.pokeId, level) ?: break
            current = pool.firstOrNull { it.pokeId == nextId } ?: break
        }
        return current
    }

    /** Crea un Pokémon ya evolucionado acorde a su nivel (para reclutas que llegan crecidos). */
    private fun spawn(species: RogueSpecies, level: Int): RoguePokemon =
        RoguePokemon.of(evolvedSpecies(species, level), level)

    private fun advance(run: RogueRunSnapshot, notice: String) {
        _uiState.value = pathChoice(
            run.copy(floor = run.floor + 1, activeIndex = firstAliveIndex(run.team)), notice)
    }

    private fun pathChoice(run: RogueRunSnapshot, notice: String? = null): RogueUiState.PathChoice =
        RogueUiState.PathChoice(run, nodeOptionsFor(run.floor), notice)

    private fun nodeOptionsFor(floor: Int): List<RogueNodeType> =
        if (floor >= RogueRules.FLOORS) listOf(RogueNodeType.BOSS)
        else listOf(RogueNodeType.FIGHT, RogueNodeType.ELITE, RogueNodeType.REST,
                    RogueNodeType.TREASURE, RogueNodeType.DOJO).shuffled(random).take(3)

    private fun abandon() {
        val s = _uiState.value as? RogueUiState.PathChoice ?: return
        finishRun(s.run)
    }

    // La victoria es imposible: toda expedición termina en derrota (con su botín consuelo).
    private fun finishRun(run: RogueRunSnapshot) {
        val payout = RogueRules.payout(run.loot, victory = false)
        _uiState.value = RogueUiState.Finished(run, victory = false, payout = payout)
        scope.launch { runCatching { cashOut.execute(payout) } }
    }

    private fun drawSpecies(tier: Int, count: Int): List<RogueSpecies> {
        val ofTier = pool.filter { it.tier == tier }.ifEmpty { pool }
        return ofTier.shuffled(random).take(count)
    }

    private fun firstAliveIndex(team: List<RoguePokemon>): Int =
        team.indexOfFirst { it.isAlive }.coerceAtLeast(0)

    private companion object {
        const val LOG_LIMIT = 8
    }
}
