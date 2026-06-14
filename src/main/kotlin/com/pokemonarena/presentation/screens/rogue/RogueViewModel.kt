package com.pokemonarena.presentation.screens.rogue

import com.pokemonarena.domain.entity.RogueBlessing
import com.pokemonarena.domain.entity.RogueEvents
import com.pokemonarena.domain.entity.RogueItems
import com.pokemonarena.domain.entity.RogueLives
import com.pokemonarena.domain.entity.RogueMapFactory
import com.pokemonarena.domain.entity.RogueMapNode
import com.pokemonarena.domain.entity.RogueMetaState
import com.pokemonarena.domain.entity.RogueNodeType
import com.pokemonarena.domain.entity.RoguePokemon
import com.pokemonarena.domain.entity.RogueRules
import com.pokemonarena.domain.entity.RogueSpecies
import com.pokemonarena.domain.entity.RogueUpgrades
import com.pokemonarena.domain.usecase.CashOutRogueRunUseCase
import com.pokemonarena.domain.usecase.ConsumeRogueLifeUseCase
import com.pokemonarena.domain.usecase.GetRogueLivesUseCase
import com.pokemonarena.domain.usecase.GetRogueMetaUseCase
import com.pokemonarena.domain.usecase.GetRoguePoolUseCase
import com.pokemonarena.domain.usecase.GetUserCoinsUseCase
import com.pokemonarena.domain.usecase.PurchaseRogueUpgradeUseCase
import com.pokemonarena.domain.usecase.RogueBattleEngine
import com.pokemonarena.domain.usecase.RogueProgression
import com.pokemonarena.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.random.Random

class RogueViewModel(
    private val getPool:         GetRoguePoolUseCase,
    private val cashOut:         CashOutRogueRunUseCase,
    private val getMeta:         GetRogueMetaUseCase,
    private val purchaseUpgrade: PurchaseRogueUpgradeUseCase,
    private val getCoins:        GetUserCoinsUseCase,
    private val getLives:        GetRogueLivesUseCase,
    private val consumeLife:     ConsumeRogueLifeUseCase,
    private val now:             () -> Long = { System.currentTimeMillis() },
    private val random:          Random = Random.Default
) : BaseViewModel() {

    private val engine = RogueBattleEngine(random)

    private val _uiState = MutableStateFlow<RogueUiState>(RogueUiState.Loading)
    val uiState: StateFlow<RogueUiState> = _uiState.asStateFlow()

    private var pool:  List<RogueSpecies> = emptyList()
    private var meta:  RogueMetaState     = RogueMetaState()
    private var coins: Int                = 0
    private var lives: RogueLives         = RogueLives.full(0L)

    private var progression = RogueProgression(emptyList(), random)
    private var resolver     = RogueEventResolver(random, progression)

    init {
        scope.launch {
            lives = runCatching { getLives.execute(now()) }.getOrDefault(lives)
            combine(getCoins.execute(), getMeta.execute()) { c, m -> c to m }.collect { (c, m) ->
                coins = c; meta = m
                when (val s = _uiState.value) {
                    is RogueUiState.Loading -> _uiState.value = RogueUiState.Lobby(c, m, lives)
                    is RogueUiState.Lobby   -> _uiState.value = s.copy(coins = c, meta = m, lives = lives)
                    else                    -> {}
                }
            }
        }
    }

    fun onEvent(event: RogueUiEvent) {
        when (event) {
            is RogueUiEvent.OpenLobby       -> scope.launch { openLobby() }
            is RogueUiEvent.BuyUpgrade      -> scope.launch { buyUpgrade(event.upgradeId) }
            is RogueUiEvent.Start           -> scope.launch { startDraft() }
            is RogueUiEvent.PickStarter     -> beginRun(event.index)
            is RogueUiEvent.PickNode        -> enterNode(event.nodeId)
            is RogueUiEvent.AdvanceBattle   -> advanceBattle()
            is RogueUiEvent.ConcludeBattle  -> concludeBattle()
            is RogueUiEvent.PickCapture     -> pickCapture(event.index)
            is RogueUiEvent.SkipCapture     -> skipCapture()
            is RogueUiEvent.PickEventOption -> resolveEvent(event.index)
            is RogueUiEvent.PickReward      -> pickReward(event.index)
            is RogueUiEvent.OpenManage      -> openManage()
            is RogueUiEvent.CloseManage     -> closeManage()
            is RogueUiEvent.EquipFromBag    -> equipFromBag(event.itemIndex, event.memberIndex)
            is RogueUiEvent.UnequipToBag    -> unequipToBag(event.memberIndex)
            is RogueUiEvent.ReorderTeam     -> reorderTeam(event.from, event.to)
            is RogueUiEvent.Abandon         -> abandon()
        }
    }


    private suspend fun openLobby() {
        lives = runCatching { getLives.execute(now()) }.getOrDefault(lives)
        _uiState.value = RogueUiState.Lobby(coins, meta, lives)
    }

    private suspend fun buyUpgrade(upgradeId: String) {
        val upgrade = RogueUpgrades.byId(upgradeId) ?: return
        val notice = when (val result = purchaseUpgrade.execute(upgrade)) {
            is PurchaseRogueUpgradeUseCase.Result.Success ->
                "Mejoraste ${upgrade.displayName} a Nv ${result.newLevel}."
            is PurchaseRogueUpgradeUseCase.Result.InsufficientCoins ->
                "Te faltan monedas: ${upgrade.displayName} cuesta ${result.cost}."
            PurchaseRogueUpgradeUseCase.Result.MaxedOut ->
                "${upgrade.displayName} ya está al máximo."
        }
        _uiState.value = RogueUiState.Lobby(coins, meta, lives, purchaseNotice = notice)
    }


    private suspend fun startDraft() {
        val afterSpend = consumeLife.execute(now())
        if (afterSpend == null) {
            lives = runCatching { getLives.execute(now()) }.getOrDefault(lives)
            _uiState.value = RogueUiState.Lobby(coins, meta, lives,
                purchaseNotice = "No te quedan vidas. Esperá a que se recarguen (1 cada 15 min).")
            return
        }
        lives = afterSpend
        if (pool.isEmpty()) pool = runCatching { getPool.execute() }.getOrDefault(emptyList())
        if (pool.isEmpty()) return
        progression = RogueProgression(pool, random)
        resolver    = RogueEventResolver(random, progression)
        val level    = RogueRules.BASE_LEVEL + meta.starterLevelBonus
        val starters = progression.drawSpecies(tier = 1, count = RogueRules.DRAFT_SIZE).map { buildStarter(it, level) }
        _uiState.value = RogueUiState.Draft(starters)
    }

    private fun buildStarter(species: RogueSpecies, level: Int): RoguePokemon {
        var mon = RoguePokemon.of(species, level)
        if (meta.startingHpFactor > 1f) mon = mon.withMaxHpBoost(meta.startingHpFactor)
        if (meta.startsWithGear)        mon = mon.equip(RogueItems.random(random))
        return mon
    }

    private fun beginRun(index: Int) {
        val draft   = _uiState.value as? RogueUiState.Draft ?: return
        val starter = draft.starters.getOrNull(index) ?: return
        val run = RogueRunSnapshot(
            act       = 1,
            map       = RogueMapFactory.generate(1, random),
            team      = listOf(starter),
            blessings = if (meta.startsWithFortune) setOf(RogueBlessing.FORTUNA) else emptySet(),
            loot      = meta.startingGold
        )
        _uiState.value = RogueUiState.Map(run, "Acto 1: abrite paso por el mapa hasta el jefe.")
    }


    private fun enterNode(nodeId: Int) {
        val s = _uiState.value as? RogueUiState.Map ?: return
        if (nodeId !in s.run.reachableNodeIds) return
        val node = s.run.node(nodeId)
        val run  = s.run.copy(currentNodeId = nodeId)
        when (node.type) {
            RogueNodeType.FIGHT  -> startBattle(run, node, boss = false)
            RogueNodeType.BOSS   -> startBattle(run, node, boss = true)
            RogueNodeType.CAPTURE -> openCapture(run)
            RogueNodeType.ITEM   -> {
                val item = RogueItems.random(random)
                toMap(run.copy(inventory = run.inventory + item),
                      "Encontraste ${item.name}: lo guardaste en la mochila.")
            }
            RogueNodeType.CENTER -> toMap(run.copy(team = run.team.map { it.healedBy(RogueRules.CENTER_HEAL_FRACTION) }),
                                          "El Centro Pokémon curó por completo a tu equipo en pie.")
            RogueNodeType.GOLD   -> {
                val gold = RogueRules.lootWith(run.blessings, RogueRules.goldChest(random.nextFloat(), depthOf(run, node)))
                toMap(run.copy(loot = run.loot + gold), "Abriste un cofre de oro: +$gold.")
            }
            RogueNodeType.EVENT  -> _uiState.value = RogueUiState.Event(run, RogueEvents.random(random))
        }
    }

    private fun toMap(run: RogueRunSnapshot, notice: String?) {
        _uiState.value = RogueUiState.Map(run.copy(clearedNodeIds = markCleared(run)), notice)
    }

    private fun markCleared(run: RogueRunSnapshot): Set<Int> =
        run.currentNodeId?.let { run.clearedNodeIds + it } ?: run.clearedNodeIds

    private fun depthOf(run: RogueRunSnapshot, node: RogueMapNode): Int =
        RogueRules.depthOf(run.act, node.row)


    private fun startBattle(run: RogueRunSnapshot, node: RogueMapNode, boss: Boolean) {
        val enemies = if (boss) bossTeam(run.act) else listOf(wildEnemy(depthOf(run, node), run.act))
        val battle = RogueBattleState(
            node              = node.type,
            enemies           = enemies,
            playerActiveIndex = firstAliveIndex(run.team)
        )
        _uiState.value = RogueUiState.Battle(run, battle)
    }

    private fun wildEnemy(depth: Int, act: Int): RoguePokemon =
        RoguePokemon.enemyOf(progression.drawSpecies(RogueRules.tierForAct(act), 1).first(), RogueRules.enemyScale(depth))

    private fun bossTeam(act: Int): List<RoguePokemon> =
        progression.drawSpecies(RogueRules.bossTierForAct(act), RogueRules.bossTeamSize(act))
            .map { RoguePokemon.enemyOf(it, RogueRules.bossScale(act)) }

    private fun advanceBattle() {
        val s = _uiState.value as? RogueUiState.Battle ?: return
        val b = s.battle
        if (b.outcome != RogueBattleOutcome.ONGOING) return

        val playerIdx = firstAliveIndex(s.run.team)
        val enemyIdx  = b.enemies.indexOfFirst { it.isAlive }
        if (s.run.team.getOrNull(playerIdx)?.isAlive != true || enemyIdx < 0) return

        val duel    = engine.duel(s.run.team[playerIdx], b.enemies[enemyIdx], s.run.blessings)
        val team    = s.run.team.toMutableList().also { it[playerIdx] = duel.player }
        val enemies = b.enemies.toMutableList().also  { it[enemyIdx]  = duel.enemy }
        val outcome = when {
            enemies.none { it.isAlive } -> RogueBattleOutcome.WON
            team.none { it.isAlive }    -> RogueBattleOutcome.LOST
            else                        -> RogueBattleOutcome.ONGOING
        }
        _uiState.value = RogueUiState.Battle(
            s.run.copy(team = team),
            b.copy(enemies = enemies, enemyIndex = enemyIdx, playerActiveIndex = playerIdx,
                   log = (b.log + duel.strikes).takeLast(LOG_LIMIT), lastDuel = duel.strikes,
                   outcome = outcome, turnId = b.turnId + 1)
        )
    }

    private fun concludeBattle() {
        val s = _uiState.value as? RogueUiState.Battle ?: return
        when (s.battle.outcome) {
            RogueBattleOutcome.WON  -> if (s.battle.isBoss) onBossDefeated(s.run) else onFightWon(s.run)
            RogueBattleOutcome.LOST -> finishRun(s.run, victory = false)
            RogueBattleOutcome.ONGOING -> {}
        }
    }

    private fun onFightWon(run: RogueRunSnapshot) {
        val depth = depthOf(run, run.node(run.currentNodeId!!))
        val loot  = RogueRules.lootWith(run.blessings, RogueRules.fightLoot(depth))
        val (leveled, notes) = progression.grantXp(run.team, RogueRules.fightXp(depth))
        val team = leveled.map { it.healedBy(RogueRules.POST_FIGHT_HEAL_FRACTION) }
        val updated = run.copy(team = team, loot = run.loot + loot)
        _uiState.value = RogueUiState.Reward(updated, rewardOptionsFor(updated),
                                             listOf("Ganaste el combate: +$loot de oro.") + notes)
    }

    private fun onBossDefeated(run: RogueRunSnapshot) {
        val act      = run.act
        val loot     = RogueRules.lootWith(run.blessings, RogueRules.bossLoot(act))
        val (team, notes) = progression.grantXp(run.team, RogueRules.bossXp(act))
        val defeated = run.bossesDefeated + 1
        val cleared  = markCleared(run)
        val base     = run.copy(team = team, loot = run.loot + loot,
                                bossesDefeated = defeated, clearedNodeIds = cleared)
        if (defeated >= RogueRules.ACTS) {
            finishRun(base, victory = true)
        } else {
            val nextAct = act + 1
            val advanced = base.copy(act = nextAct, map = RogueMapFactory.generate(nextAct, random),
                                     currentNodeId = null, clearedNodeIds = emptySet())
            _uiState.value = RogueUiState.Map(advanced,
                "¡Jefe del Acto $act derrotado! Empieza el Acto $nextAct. " + notes.joinToString(" "))
        }
    }

    private fun finishRun(run: RogueRunSnapshot, victory: Boolean) {
        val payout = RogueRules.payout(run.loot, victory)
        _uiState.value = RogueUiState.Finished(run, victory, payout)
        scope.launch { runCatching { cashOut.execute(payout) } }
    }


    private fun openCapture(run: RogueRunSnapshot) {
        if (run.team.size >= RogueRules.TEAM_CAPACITY) {
            toMap(run, "Tu equipo está completo (${RogueRules.TEAM_CAPACITY}): no podés capturar más.")
            return
        }
        val candidates = progression.drawSpecies(RogueRules.recruitTier(run.act), count = 3)
            .map { progression.spawn(it, progression.recruitLevel(run.team)) }
        _uiState.value = RogueUiState.Capture(run, candidates)
    }

    private fun pickCapture(index: Int) {
        val s = _uiState.value as? RogueUiState.Capture ?: return
        val caught = s.candidates.getOrNull(index) ?: return
        toMap(s.run.copy(team = s.run.team + caught), "¡${caught.species.displayName} se unió a tu equipo!")
    }

    private fun skipCapture() {
        val s = _uiState.value as? RogueUiState.Capture ?: return
        toMap(s.run, "Dejaste ir al Pokémon salvaje.")
    }


    private fun resolveEvent(optionIndex: Int) {
        val s      = _uiState.value as? RogueUiState.Event ?: return
        val option = s.event.options.getOrNull(optionIndex) ?: return
        val result = resolver.resolve(s.run, option.effects)
        val message = result.messages.joinToString(" ").ifBlank { "Seguís tu camino." }
        when (result.next) {
            RogueEventResolver.Next.CONTINUE -> toMap(result.run, message)
            RogueEventResolver.Next.FIGHT    ->
                startBattle(result.run, result.run.node(result.run.currentNodeId!!), boss = false)
        }
    }


    private fun pickReward(index: Int) {
        val s = _uiState.value as? RogueUiState.Reward ?: return
        when (val option = s.options.getOrNull(index) ?: return) {
            is RogueRewardOption.Recruit ->
                toMap(s.run.copy(team = s.run.team + option.pokemon),
                      "¡${option.pokemon.species.displayName} se unió a tu equipo!")
            is RogueRewardOption.Heal ->
                toMap(s.run.copy(team = s.run.team.map { it.healedBy(RogueRules.REWARD_HEAL_FRACTION) }),
                      "Tu equipo recuperó parte del HP.")
            is RogueRewardOption.Blessing ->
                toMap(applyBlessing(s.run, option.blessing), "Bendición obtenida: ${option.blessing.displayName}.")
            is RogueRewardOption.Loot ->
                toMap(s.run.copy(loot = s.run.loot + option.coins), "Sumaste ${option.coins} de oro al botín.")
            is RogueRewardOption.Gear ->
                toMap(s.run.copy(inventory = s.run.inventory + option.item),
                      "${option.item.name} fue a tu mochila. Equipalo desde 'Equipo y mochila'.")
        }
    }


    private fun openManage() {
        val s = _uiState.value as? RogueUiState.Map ?: return
        _uiState.value = RogueUiState.Manage(s.run)
    }

    private fun closeManage() {
        val s = _uiState.value as? RogueUiState.Manage ?: return
        _uiState.value = RogueUiState.Map(s.run)
    }

    private fun equipFromBag(itemIndex: Int, memberIndex: Int) {
        val s      = _uiState.value as? RogueUiState.Manage ?: return
        val item   = s.run.inventory.getOrNull(itemIndex) ?: return
        val member = s.run.team.getOrNull(memberIndex) ?: return
        val bag    = s.run.inventory.toMutableList().also { it.removeAt(itemIndex) }
        member.item?.let { bag.add(it) }
        val team   = s.run.team.toMutableList().also { it[memberIndex] = member.withGear(item) }
        _uiState.value = RogueUiState.Manage(s.run.copy(team = team, inventory = bag))
    }

    private fun unequipToBag(memberIndex: Int) {
        val s      = _uiState.value as? RogueUiState.Manage ?: return
        val member = s.run.team.getOrNull(memberIndex) ?: return
        val held   = member.item ?: return
        val team   = s.run.team.toMutableList().also { it[memberIndex] = member.withGear(null) }
        _uiState.value = RogueUiState.Manage(s.run.copy(team = team, inventory = s.run.inventory + held))
    }

    private fun reorderTeam(from: Int, to: Int) {
        val run = currentRun() ?: return
        if (from !in run.team.indices || to !in run.team.indices || from == to) return
        val team = run.team.toMutableList().also { it.add(to, it.removeAt(from)) }
        updateRun(run.copy(team = team))
    }

    private fun currentRun(): RogueRunSnapshot? = when (val s = _uiState.value) {
        is RogueUiState.Map    -> s.run
        is RogueUiState.Manage -> s.run
        else                   -> null
    }

    private fun updateRun(run: RogueRunSnapshot) {
        when (val s = _uiState.value) {
            is RogueUiState.Map    -> _uiState.value = s.copy(run = run)
            is RogueUiState.Manage -> _uiState.value = s.copy(run = run)
            else                   -> {}
        }
    }

    private fun rewardOptionsFor(run: RogueRunSnapshot): List<RogueRewardOption> {
        val candidates = buildList {
            if (run.team.size < RogueRules.TEAM_CAPACITY) {
                val recruit = progression.spawn(
                    progression.drawSpecies(RogueRules.recruitTier(run.act), 1).first(),
                    progression.recruitLevel(run.team))
                add(RogueRewardOption.Recruit(recruit))
            }
            add(RogueRewardOption.Heal)
            add(RogueRewardOption.Gear(RogueItems.random(random)))
            (RogueBlessing.entries - run.blessings).randomOrNull(random)
                ?.let { add(RogueRewardOption.Blessing(it)) }
            add(RogueRewardOption.Loot(RogueRules.lootWith(run.blessings,
                RogueRules.goldChest(random.nextFloat(), RogueRules.depthOf(run.act, 0)))))
        }
        return candidates.shuffled(random).take(3)
    }

    private fun applyBlessing(run: RogueRunSnapshot, blessing: RogueBlessing): RogueRunSnapshot {
        val team = if (blessing == RogueBlessing.AGUANTE)
            run.team.map { if (it.isAlive) it.withMaxHpBoost(RogueRules.HP_BLESSING) else it }
        else run.team
        return run.copy(team = team, blessings = run.blessings + blessing)
    }

    private fun abandon() {
        val s = _uiState.value as? RogueUiState.Map ?: return
        finishRun(s.run, victory = false)
    }

    private fun firstAliveIndex(team: List<RoguePokemon>): Int =
        team.indexOfFirst { it.isAlive }.coerceAtLeast(0)

    private companion object {
        const val LOG_LIMIT = 10
    }
}
