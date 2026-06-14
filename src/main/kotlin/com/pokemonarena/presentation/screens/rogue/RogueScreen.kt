package com.pokemonarena.presentation.screens.rogue

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.pokemonarena.domain.entity.RogueItem
import com.pokemonarena.domain.entity.RogueLives
import com.pokemonarena.domain.entity.RogueMapNode
import com.pokemonarena.domain.entity.RogueNodeType
import com.pokemonarena.domain.entity.RoguePokemon
import com.pokemonarena.domain.entity.RogueRules
import com.pokemonarena.domain.entity.RogueUpgrade
import com.pokemonarena.domain.entity.RogueUpgrades
import com.pokemonarena.domain.entity.TypeMatchup
import com.pokemonarena.domain.usecase.RogueStrike
import com.pokemonarena.presentation.theme.AppColors
import com.pokemonarena.presentation.theme.AppIcons
import com.pokemonarena.presentation.utils.CardImagePlaceholder
import com.pokemonarena.presentation.utils.CoinText
import com.pokemonarena.presentation.utils.TypeBadge
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RogueScreen(viewModel: RogueViewModel) {
    val state by viewModel.uiState.collectAsState()
    Box(Modifier.fillMaxSize().background(AppColors.legendaryDark)) {
        when (val s = state) {
            is RogueUiState.Loading  -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is RogueUiState.Lobby    -> LobbyContent(s, viewModel)
            is RogueUiState.Draft    -> DraftContent(s, viewModel)
            is RogueUiState.Map      -> MapContent(s, viewModel)
            is RogueUiState.Battle   -> BattleContent(s, viewModel)
            is RogueUiState.Capture  -> CaptureContent(s, viewModel)
            is RogueUiState.Event    -> EventContent(s, viewModel)
            is RogueUiState.Reward   -> RewardContent(s, viewModel)
            is RogueUiState.Manage   -> ManageContent(s, viewModel)
            is RogueUiState.Finished -> FinishedContent(s) { viewModel.onEvent(RogueUiEvent.OpenLobby) }
        }
    }
}


@Composable
private fun LobbyContent(state: RogueUiState.Lobby, viewModel: RogueViewModel) {
    var nowMs by remember { mutableStateOf(0L) }
    LaunchedEffect(state.lives) {
        while (true) { nowMs = System.currentTimeMillis(); delay(1000) }
    }
    val live     = state.lives.regenerated(nowMs)
    val canStart = nowMs == 0L || live.lives > 0

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
           horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(AppIcons.trophy, null, tint = AppColors.goldColor, modifier = Modifier.size(52.dp))
        Text("Expedición Rogue", color = AppColors.textIconsColor,
             style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("Ascendé por un mapa de nodos, armá tu equipo y vencé a los ${RogueRules.ACTS} jefes " +
             "para coronarte campeón. Cada victoria suma oro; si todo tu equipo cae, es Game Over.",
             color = AppColors.textIconsColor.copy(0.75f), textAlign = TextAlign.Center,
             style = MaterialTheme.typography.bodyMedium)

        Surface(shape = RoundedCornerShape(12.dp), color = AppColors.legendaryMid) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CoinText("Tu oro: ${state.coins}", color = AppColors.goldColor,
                         style = MaterialTheme.typography.titleMedium, iconSize = 20.dp)
                LivesBadge(live, nowMs)
            }
        }

        state.purchaseNotice?.let {
            Card(Modifier.fillMaxWidth(),
                 colors = CardDefaults.cardColors(containerColor = AppColors.infoColor.copy(0.18f))) {
                Text(it, Modifier.padding(12.dp), color = AppColors.textIconsColor,
                     style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }

        Text("Tienda permanente", color = AppColors.goldColor,
             style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold,
             modifier = Modifier.align(Alignment.Start))
        Text("Gastá tu oro en mejoras que arrancan activas en cada futura expedición.",
             color = AppColors.textIconsColor.copy(0.65f), style = MaterialTheme.typography.bodySmall,
             modifier = Modifier.align(Alignment.Start))

        RogueUpgrades.ALL.forEach { upgrade ->
            ShopUpgradeCard(upgrade, state.meta.levelOf(upgrade), state.coins) {
                viewModel.onEvent(RogueUiEvent.BuyUpgrade(upgrade.id))
            }
        }

        Button(onClick = { viewModel.onEvent(RogueUiEvent.Start) }, enabled = canStart,
               modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(12.dp),
               colors = ButtonDefaults.buttonColors(containerColor = AppColors.goldColor,
                                                    contentColor = AppColors.textPrimary)) {
            Text(if (canStart) "Comenzar expedición (−1 vida)"
                 else "Sin vidas — próxima en ${formatCountdown(live.msUntilNextLife(nowMs))}",
                 fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun LivesBadge(live: RogueLives, nowMs: Long) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(RogueLives.MAX) { index ->
            Icon(if (index < live.lives) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                 contentDescription = null,
                 tint = if (index < live.lives) AppColors.primaryColor else AppColors.textIconsColor.copy(0.3f),
                 modifier = Modifier.size(20.dp))
        }
        if (!live.isFull && nowMs != 0L) {
            Text("+1 en ${formatCountdown(live.msUntilNextLife(nowMs))}",
                 color = AppColors.textIconsColor.copy(0.7f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun formatCountdown(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

@Composable
private fun ShopUpgradeCard(upgrade: RogueUpgrade, level: Int, coins: Int, onBuy: () -> Unit) {
    val maxed = level >= upgrade.maxLevel
    val cost  = upgrade.costAt(level)
    val canAfford = !maxed && coins >= cost
    Card(Modifier.fillMaxWidth(),
         colors = CardDefaults.cardColors(containerColor = AppColors.legendaryMid)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("${upgrade.displayName}  ·  Nv $level/${upgrade.maxLevel}",
                     color = AppColors.textIconsColor, fontWeight = FontWeight.ExtraBold)
                Text(upgrade.description, color = AppColors.textIconsColor.copy(0.7f),
                     style = MaterialTheme.typography.bodySmall)
            }
            if (maxed) {
                Text("MÁX", color = AppColors.goldColor, fontWeight = FontWeight.ExtraBold)
            } else {
                Button(onClick = onBuy, enabled = canAfford, shape = RoundedCornerShape(10.dp),
                       colors = ButtonDefaults.buttonColors(containerColor = AppColors.goldColor,
                                                            contentColor = AppColors.textPrimary)) {
                    Text("$cost", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}


@Composable
private fun DraftContent(state: RogueUiState.Draft, viewModel: RogueViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp),
           horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Elegí tu inicial", color = AppColors.textIconsColor,
             style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text("Tu primer compañero. Vas a reclutar más en el camino (equipo de hasta ${RogueRules.TEAM_CAPACITY}).",
             color = AppColors.textIconsColor.copy(0.7f), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            state.starters.forEachIndexed { index, pokemon ->
                StarterCard(pokemon) { viewModel.onEvent(RogueUiEvent.PickStarter(index)) }
            }
        }
    }
}

@Composable
private fun StarterCard(pokemon: RoguePokemon, onPick: () -> Unit) {
    Card(Modifier.width(180.dp).clickable(onClick = onPick),
         elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RogueSprite(pokemon, 96.dp)
            Text(pokemon.species.displayName, fontWeight = FontWeight.ExtraBold)
            Row { pokemon.species.types.forEach { TypeBadge(it) } }
            StatLine("HP", pokemon.maxHp)
            StatLine("Ataque", pokemon.attack)
            StatLine("Defensa", pokemon.defense)
            StatLine("Velocidad", pokemon.speed)
        }
    }
}

@Composable
private fun StatLine(label: String, value: Int) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall,
             color = AppColors.textSecondary, modifier = Modifier.weight(1f))
        Text("$value", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}


@Composable
private fun MapContent(state: RogueUiState.Map, viewModel: RogueViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
           verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RunHeader(state.run)
        state.notice?.let { NoticeBanner(it) }
        Text("Elegí tu próximo destino", color = AppColors.textIconsColor,
             style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        NodeGraph(state.run) { viewModel.onEvent(RogueUiEvent.PickNode(it)) }
        NodeLegend()
        ReorderableTeamStrip(
            team       = state.run.team,
            onReorder  = { from, to -> viewModel.onEvent(RogueUiEvent.ReorderTeam(from, to)) },
            onManage   = { viewModel.onEvent(RogueUiEvent.OpenManage) }
        )
        TextButton(onClick = { viewModel.onEvent(RogueUiEvent.Abandon) }) {
            Text("Abandonar expedición (cobrás el oro juntado)",
                 color = AppColors.textIconsColor.copy(0.6f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun NodeGraph(run: RogueRunSnapshot, onPick: (Int) -> Unit) {
    val rows      = run.map.rows
    val reachable = run.reachableNodeIds.toSet()
    val nodeSize  = 46.dp
    val rowGap    = 74.dp
    val topPad    = 8.dp
    val height    = topPad + rowGap * (rows.size - 1) + nodeSize + 8.dp

    fun frac(node: RogueMapNode): Float {
        val width = rows[node.row].size
        val base  = if (width <= 1) 0.5f else node.col / (width - 1f)
        return 0.12f + base * 0.76f
    }

    BoxWithConstraints(Modifier.fillMaxWidth().height(height)) {
        val w = maxWidth
        Canvas(Modifier.matchParentSize()) {
            val gapPx  = rowGap.toPx()
            val topPx  = topPad.toPx() + nodeSize.toPx() / 2
            rows.forEachIndexed { r, rowNodes ->
                if (r == rows.size - 1) return@forEachIndexed
                rowNodes.forEach { node ->
                    node.next.forEach { nid ->
                        val target = run.node(nid)
                        val active = node.id == run.currentNodeId
                        drawLine(
                            color = if (active) AppColors.goldColor else AppColors.textIconsColor.copy(0.18f),
                            start = Offset(frac(node) * size.width, topPx + gapPx * r),
                            end   = Offset(frac(target) * size.width, topPx + gapPx * (r + 1)),
                            strokeWidth = if (active) 5f else 3f
                        )
                    }
                }
            }
        }
        rows.forEachIndexed { r, rowNodes ->
            rowNodes.forEach { node ->
                Box(Modifier.offset(x = w * frac(node) - nodeSize / 2, y = topPad + rowGap * r)) {
                    NodeChip(
                        node      = node,
                        size      = nodeSize,
                        cleared   = node.id in run.clearedNodeIds,
                        reachable = node.id in reachable,
                        current   = node.id == run.currentNodeId,
                        onPick    = { onPick(node.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NodeChip(node: RogueMapNode, size: Dp, cleared: Boolean, reachable: Boolean,
                     current: Boolean, onPick: () -> Unit) {
    val enabled = reachable && !cleared
    val tint    = nodeColor(node.type)
    val dim      = !enabled && !current && !cleared
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier.size(size).clip(CircleShape)
            .background(if (cleared) AppColors.legendaryMid else tint.copy(alpha = 0.92f))
            .border(BorderStroke(if (current || enabled) 3.dp else 1.dp,
                                 if (current) AppColors.goldColor
                                 else if (enabled) AppColors.textIconsColor else AppColors.textIconsColor.copy(0.25f)),
                    CircleShape)
            .alpha(if (dim) 0.4f else 1f)
            .clickable(enabled = enabled, onClick = onPick)) {
        Icon(nodeIcon(node.type), node.type.displayName, tint = Color.White,
             modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun NodeLegend() {
    val entries = listOf(
        RogueNodeType.FIGHT, RogueNodeType.CAPTURE, RogueNodeType.ITEM,
        RogueNodeType.CENTER, RogueNodeType.GOLD, RogueNodeType.EVENT, RogueNodeType.BOSS
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { type ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(18.dp).clip(CircleShape).background(nodeColor(type)),
                            contentAlignment = Alignment.Center) {
                            Icon(nodeIcon(type), null, tint = Color.White, modifier = Modifier.size(11.dp))
                        }
                        Spacer(Modifier.width(5.dp))
                        Text(type.displayName, color = AppColors.textIconsColor.copy(0.8f),
                             style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

private fun nodeIcon(type: RogueNodeType): ImageVector = when (type) {
    RogueNodeType.FIGHT   -> AppIcons.battle
    RogueNodeType.CAPTURE -> Icons.Filled.CatchingPokemon
    RogueNodeType.ITEM    -> AppIcons.cards
    RogueNodeType.CENTER  -> Icons.Filled.LocalHospital
    RogueNodeType.GOLD    -> AppIcons.coin
    RogueNodeType.EVENT   -> Icons.Filled.HelpOutline
    RogueNodeType.BOSS    -> AppIcons.trophy
}

private fun nodeColor(type: RogueNodeType): Color = when (type) {
    RogueNodeType.FIGHT   -> AppColors.primaryColor
    RogueNodeType.CAPTURE -> AppColors.successColor
    RogueNodeType.ITEM    -> AppColors.infoColor
    RogueNodeType.CENTER  -> Color(0xFFEC407A)
    RogueNodeType.GOLD    -> AppColors.coinColor
    RogueNodeType.EVENT   -> AppColors.epicColor
    RogueNodeType.BOSS    -> AppColors.accentColor
}


@Composable
private fun BattleContent(state: RogueUiState.Battle, viewModel: RogueViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RunHeader(state.run)
        EnemyLineup(state)
        BattleArena(state) {
            when (state.battle.outcome) {
                RogueBattleOutcome.ONGOING -> viewModel.onEvent(RogueUiEvent.AdvanceBattle)
                else                       -> viewModel.onEvent(RogueUiEvent.ConcludeBattle)
            }
        }
        Text("Combate automático — pelean según el orden de tu equipo",
             color = AppColors.goldColor, fontWeight = FontWeight.ExtraBold,
             style = MaterialTheme.typography.labelMedium)
        TeamLineup(state)
        BattleLog(state.battle.log, Modifier.weight(1f))
    }
}

@Composable
private fun EnemyLineup(state: RogueUiState.Battle) {
    val battle = state.battle
    if (battle.isBoss) {
        Text("JEFE — equipo de ${battle.enemiesTotal}", color = AppColors.accentColor,
             fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        battle.enemies.forEachIndexed { index, enemy ->
            val active = index == battle.enemyIndex && enemy.isAlive
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                   modifier = Modifier.alpha(if (enemy.isAlive) 1f else 0.35f)) {
                Box(Modifier.size(34.dp).clip(CircleShape)
                        .border(BorderStroke(if (active) 2.dp else 0.dp, AppColors.goldColor), CircleShape)) {
                    RogueSprite(enemy, 34.dp)
                }
                Text(if (enemy.isAlive) "${enemy.currentHp}" else "✖",
                     color = AppColors.textIconsColor.copy(0.8f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun BattleArena(state: RogueUiState.Battle, onStepDone: () -> Unit) {
    val player = state.run.team[state.battle.playerActiveIndex]
    val enemy  = state.battle.enemy

    val playerHp    = remember { Animatable(player.hpFraction) }
    val enemyHp     = remember { Animatable(enemy.hpFraction) }
    val playerLunge = remember { Animatable(0f) }
    val enemyLunge  = remember { Animatable(0f) }
    var playerDmg    by remember { mutableStateOf<Int?>(null) }
    var enemyDmg     by remember { mutableStateOf<Int?>(null) }
    var playerDmgKey by remember { mutableStateOf(0) }
    var enemyDmgKey  by remember { mutableStateOf(0) }
    val anim = rememberCoroutineScope()

    LaunchedEffect(state.battle.turnId) {
        val strikes = state.battle.lastDuel
        val enemyPre  = strikes.firstOrNull { it.isPlayerAttack }?.let { it.defenderHpAfter + it.damage } ?: enemy.currentHp
        val playerPre = strikes.firstOrNull { !it.isPlayerAttack }?.let { it.defenderHpAfter + it.damage } ?: player.currentHp
        playerHp.snapTo((playerPre.toFloat() / player.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f))
        enemyHp.snapTo((enemyPre.toFloat() / enemy.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f))

        for (strike in strikes) {
            val target = (strike.defenderHpAfter.toFloat() / strike.defenderMaxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
            if (strike.isPlayerAttack) {
                anim.launch { playerLunge.animateTo(22f, tween(110)); playerLunge.animateTo(0f, tween(240)) }
                enemyDmg = strike.damage; enemyDmgKey++
                enemyHp.animateTo(target, tween(380))
            } else {
                anim.launch { enemyLunge.animateTo(-22f, tween(110)); enemyLunge.animateTo(0f, tween(240)) }
                playerDmg = strike.damage; playerDmgKey++
                playerHp.animateTo(target, tween(380))
            }
            delay(440)
        }
        delay(280)
        onStepDone()
    }

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AppColors.legendaryMid)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CombatantColumn(player, playerHp.value, isEnemy = false, lungeX = playerLunge.value,
                            damage = playerDmg, damageKey = playerDmgKey, modifier = Modifier.weight(1f))
            Text("VS", color = AppColors.goldColor, fontWeight = FontWeight.ExtraBold,
                 fontSize = 22.sp, modifier = Modifier.padding(horizontal = 8.dp))
            CombatantColumn(enemy, enemyHp.value, isEnemy = true, lungeX = enemyLunge.value,
                            damage = enemyDmg, damageKey = enemyDmgKey, modifier = Modifier.weight(1f),
                            label = if (state.battle.isBoss) "Jefe" else "Salvaje")
        }
    }
}

@Composable
private fun CombatantColumn(pokemon: RoguePokemon, hpFraction: Float, isEnemy: Boolean,
                            lungeX: Float, damage: Int?, damageKey: Int,
                            modifier: Modifier = Modifier, label: String? = null) {
    val bob by rememberInfiniteTransition(label = "bob").animateFloat(
        initialValue = 0f, targetValue = if (isEnemy) -4f else 4f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "bobValue")

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(4.dp)) {
        label?.let {
            Text(it, color = AppColors.goldColor, style = MaterialTheme.typography.labelSmall,
                 fontWeight = FontWeight.ExtraBold)
        }
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.offset(x = lungeX.dp, y = bob.dp)) { RogueSprite(pokemon, 88.dp) }
            FloatingDamage(damage, damageKey)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(pokemon.species.displayName, color = AppColors.textIconsColor, fontWeight = FontWeight.ExtraBold)
            if (!isEnemy) { Spacer(Modifier.width(6.dp)); LevelBadge(pokemon.level) }
        }
        Row { pokemon.species.types.forEach { TypeBadge(it) } }
        HpBar(hpFraction, (hpFraction * pokemon.maxHp).roundToInt(), pokemon.maxHp)
        if (!isEnemy) {
            XpBar(pokemon.xpFraction)
            Text("ATQ ${pokemon.attack} · DEF ${pokemon.defense} · VEL ${pokemon.speed}",
                 color = AppColors.textIconsColor.copy(0.6f), style = MaterialTheme.typography.labelSmall)
            pokemon.item?.let {
                Text("⚙ ${it.name}", color = AppColors.infoColor,
                     style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TeamLineup(state: RogueUiState.Battle) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.run.team.forEachIndexed { index, member ->
            val isActive = index == state.battle.playerActiveIndex
            Card(
                border = if (isActive) BorderStroke(2.dp, AppColors.goldColor) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (member.isAlive) MaterialTheme.colorScheme.surface
                                     else AppColors.surfaceVariant)
            ) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    RogueSprite(member, 40.dp)
                    Text("${index + 1}· Nv ${member.level}", style = MaterialTheme.typography.labelSmall,
                         fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                         color = if (member.isAlive) AppColors.textPrimary else AppColors.textSecondary)
                    Text(if (member.isAlive) "${member.currentHp} HP" else "Debilitado",
                         style = MaterialTheme.typography.labelSmall,
                         color = if (member.isAlive) AppColors.successColor else AppColors.defeatColor)
                }
            }
        }
    }
}

@Composable
private fun BattleLog(log: List<RogueStrike>, modifier: Modifier = Modifier) {
    if (log.isEmpty()) return
    Card(modifier.fillMaxWidth()) {
        LazyColumn(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp),
                   reverseLayout = true) {
            items(log.reversed()) { strike ->
                Text(strikeText(strike), style = MaterialTheme.typography.labelSmall,
                     color = if (strike.isPlayerAttack) AppColors.successColor else AppColors.defeatColor)
            }
        }
    }
}

private fun strikeText(strike: RogueStrike): String {
    val effectiveness = when {
        strike.effectiveness > TypeMatchup.NEUTRAL -> " ¡Supereficaz! (x2)"
        strike.effectiveness < TypeMatchup.NEUTRAL -> " Poco eficaz… (x0.5)"
        else                                       -> ""
    }
    val faint = if (strike.defenderFainted) " ¡${strike.defenderName} se debilitó!" else ""
    return "${strike.attackerName} usó ${strike.moveName}: -${strike.damage} a ${strike.defenderName}.$effectiveness$faint"
}


@Composable
private fun CaptureContent(state: RogueUiState.Capture, viewModel: RogueViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
           verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RunHeader(state.run)
        Text("Un Pokémon salvaje quiere unirse", color = AppColors.textIconsColor,
             style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        state.candidates.forEachIndexed { index, pokemon ->
            Card(Modifier.fillMaxWidth().clickable { viewModel.onEvent(RogueUiEvent.PickCapture(index)) },
                 elevation = CardDefaults.cardElevation(3.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RogueSprite(pokemon, 56.dp)
                    Column {
                        Text("${pokemon.species.displayName} · Nv ${pokemon.level}", fontWeight = FontWeight.ExtraBold)
                        Row { pokemon.species.types.forEach { TypeBadge(it) } }
                        Text("HP ${pokemon.maxHp} · ATQ ${pokemon.attack} · DEF ${pokemon.defense} · VEL ${pokemon.speed}",
                             style = MaterialTheme.typography.labelSmall, color = AppColors.textSecondary)
                    }
                }
            }
        }
        TextButton(onClick = { viewModel.onEvent(RogueUiEvent.SkipCapture) }) {
            Text("Dejarlo ir", color = AppColors.textIconsColor.copy(0.7f))
        }
    }
}


@Composable
private fun EventContent(state: RogueUiState.Event, viewModel: RogueViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
           verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RunHeader(state.run)
        Card(Modifier.fillMaxWidth(),
             colors = CardDefaults.cardColors(containerColor = AppColors.epicColor.copy(0.2f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = AppColors.epicColor)
                    Spacer(Modifier.width(8.dp))
                    Text(state.event.title, color = AppColors.textIconsColor,
                         style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                }
                Text(state.event.narrative, color = AppColors.textIconsColor.copy(0.8f),
                     style = MaterialTheme.typography.bodyMedium)
            }
        }
        state.event.options.forEachIndexed { index, option ->
            Card(Modifier.fillMaxWidth().clickable { viewModel.onEvent(RogueUiEvent.PickEventOption(index)) },
                 elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(option.label, fontWeight = FontWeight.ExtraBold)
                    Text(option.description, style = MaterialTheme.typography.bodySmall,
                         color = AppColors.textSecondary)
                }
            }
        }
    }
}


@Composable
private fun RewardContent(state: RogueUiState.Reward, viewModel: RogueViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
           verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RunHeader(state.run)
        Text("¡Victoria! Agarrá una recompensa", color = AppColors.textIconsColor,
             style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        if (state.notes.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.successBackground)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    state.notes.forEach {
                        Text("★ $it", style = MaterialTheme.typography.labelSmall,
                             color = AppColors.successColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        state.options.forEachIndexed { index, option ->
            RewardCard(option) { viewModel.onEvent(RogueUiEvent.PickReward(index)) }
        }
    }
}

@Composable
private fun RewardCard(option: RogueRewardOption, onPick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onPick), elevation = CardDefaults.cardElevation(3.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            when (option) {
                is RogueRewardOption.Recruit -> {
                    RogueSprite(option.pokemon, 52.dp)
                    Column {
                        Text("Reclutar a ${option.pokemon.species.displayName} (Nv ${option.pokemon.level})",
                             fontWeight = FontWeight.ExtraBold)
                        Row { option.pokemon.species.types.forEach { TypeBadge(it) } }
                        Text("HP ${option.pokemon.maxHp} · ATQ ${option.pokemon.attack} · VEL ${option.pokemon.speed}",
                             style = MaterialTheme.typography.labelSmall, color = AppColors.textSecondary)
                    }
                }
                is RogueRewardOption.Heal -> RewardLine(Icons.Filled.Favorite, AppColors.successColor,
                    "Curación", "Tu equipo en pie recupera el ${(RogueRules.REWARD_HEAL_FRACTION * 100).toInt()}% del HP.")
                is RogueRewardOption.Blessing -> RewardLine(Icons.Filled.AutoAwesome, AppColors.epicColor,
                    option.blessing.displayName, option.blessing.description)
                is RogueRewardOption.Loot -> RewardLine(AppIcons.coin, AppColors.coinColor,
                    "Oro extra", "+${option.coins} al botín de la expedición.")
                is RogueRewardOption.Gear -> RewardLine(AppIcons.cards, AppColors.infoColor,
                    "Objeto: ${option.item.name}", option.item.description)
            }
        }
    }
}

@Composable
private fun RewardLine(icon: ImageVector, tint: Color, title: String, body: String) {
    Icon(icon, null, tint = tint, modifier = Modifier.size(36.dp))
    Column {
        Text(title, fontWeight = FontWeight.ExtraBold, color = tint)
        Text(body, style = MaterialTheme.typography.bodySmall, color = AppColors.textSecondary)
    }
}


@Composable
private fun ManageContent(state: RogueUiState.Manage, viewModel: RogueViewModel) {
    val run = state.run
    var selected by remember { mutableStateOf<Int?>(null) }
    if (selected != null && selected !in run.inventory.indices) selected = null

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
           verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RunHeader(run)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Equipo y mochila", color = AppColors.textIconsColor, modifier = Modifier.weight(1f),
                 style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Button(onClick = { viewModel.onEvent(RogueUiEvent.CloseManage) }, shape = RoundedCornerShape(10.dp),
                   colors = ButtonDefaults.buttonColors(containerColor = AppColors.goldColor,
                                                        contentColor = AppColors.textPrimary)) {
                Text("Volver al mapa", fontWeight = FontWeight.Bold)
            }
        }
        Text("Tocá un objeto de la mochila y después un Pokémon para equiparlo. Los objetos son " +
             "intercambiables: equipar a quien ya tiene uno los intercambia.",
             color = AppColors.textIconsColor.copy(0.7f), style = MaterialTheme.typography.bodySmall)

        Text("Mochila (${run.inventory.size})", color = AppColors.goldColor,
             fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
        if (run.inventory.isEmpty()) {
            Text("Vacía. Conseguís objetos en nodos de Objeto, eventos y recompensas de combate.",
                 color = AppColors.textIconsColor.copy(0.6f), style = MaterialTheme.typography.bodySmall)
        } else {
            run.inventory.forEachIndexed { i, item ->
                Card(Modifier.fillMaxWidth().clickable { selected = if (selected == i) null else i },
                     border = if (selected == i) BorderStroke(2.dp, AppColors.goldColor) else null) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(AppIcons.cards, null, tint = AppColors.infoColor, modifier = Modifier.size(28.dp))
                        Column {
                            Text(item.name, fontWeight = FontWeight.ExtraBold)
                            Text(gearBonusLabel(item), style = MaterialTheme.typography.labelSmall,
                                 color = AppColors.successColor)
                        }
                    }
                }
            }
        }

        Text("Equipo", color = AppColors.goldColor, fontWeight = FontWeight.ExtraBold,
             style = MaterialTheme.typography.titleSmall)
        run.team.forEachIndexed { index, member ->
            Card(Modifier.fillMaxWidth().clickable(enabled = selected != null) {
                    selected?.let { viewModel.onEvent(RogueUiEvent.EquipFromBag(it, index)); selected = null }
                 },
                 border = if (selected != null) BorderStroke(1.dp, AppColors.goldColor.copy(0.5f)) else null,
                 elevation = CardDefaults.cardElevation(3.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RogueSprite(member, 48.dp)
                    Column(Modifier.weight(1f)) {
                        Text("${index + 1}· ${member.species.displayName} · Nv ${member.level}",
                             fontWeight = FontWeight.Bold)
                        Text("HP ${member.maxHp} · ATQ ${member.attack} · DEF ${member.defense} · VEL ${member.speed}",
                             style = MaterialTheme.typography.labelSmall, color = AppColors.textSecondary)
                    }
                    member.item?.let { held ->
                        AssistChip(
                            onClick = { viewModel.onEvent(RogueUiEvent.UnequipToBag(index)) },
                            label = { Text("⚙ ${held.name}  ✕", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }
}

private fun gearBonusLabel(item: RogueItem): String = buildList {
    if (item.attackMult  > 1f) add("ATQ +${((item.attackMult  - 1f) * 100).toInt()}%")
    if (item.defenseMult > 1f) add("DEF +${((item.defenseMult - 1f) * 100).toInt()}%")
    if (item.speedMult   > 1f) add("VEL +${((item.speedMult   - 1f) * 100).toInt()}%")
    if (item.hpMult      > 1f) add("HP +${((item.hpMult       - 1f) * 100).toInt()}%")
}.joinToString(" · ")


@Composable
private fun FinishedContent(state: RogueUiState.Finished, onBack: () -> Unit) {
    val scale by animateFloatAsState(1f, tween(500), label = "trophy")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(32.dp)) {
            Icon(AppIcons.trophy, null, modifier = Modifier.size(64.dp).scale(scale),
                 tint = if (state.victory) AppColors.goldColor else AppColors.dividerColor)
            Text(if (state.victory) "¡Campeón! Venciste a los ${RogueRules.ACTS} jefes"
                 else "La expedición terminó en el Acto ${state.run.act}",
                 color = AppColors.textIconsColor, style = MaterialTheme.typography.headlineSmall,
                 fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text(if (state.victory) "Tu equipo conquistó la cima. Te llevás todo el oro juntado más un bonus de campeón."
                 else "Tu equipo cayó, pero cobrás todo el oro que juntaste en el camino.",
                 color = AppColors.textIconsColor.copy(0.75f), style = MaterialTheme.typography.bodyMedium,
                 textAlign = TextAlign.Center)
            CoinText("+${state.payout} de oro", color = AppColors.coinColor,
                     style = MaterialTheme.typography.titleLarge, iconSize = 24.dp)
            Button(onClick = onBack, shape = RoundedCornerShape(12.dp),
                   colors = ButtonDefaults.buttonColors(containerColor = AppColors.goldColor,
                                                        contentColor = AppColors.textPrimary)) {
                Text("Volver a la entrada", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}


@Composable
private fun RunHeader(run: RogueRunSnapshot) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AppColors.legendaryMid)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Acto ${run.act}/${RogueRules.ACTS}", color = AppColors.goldColor,
                     fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(12.dp))
                Text("Jefes: ${run.bossesDefeated}/${RogueRules.ACTS}",
                     color = AppColors.textIconsColor.copy(0.85f), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                CoinText("Botín: ${run.loot}", color = AppColors.coinColor)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(RogueRules.ACTS) { index ->
                    Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (index < run.bossesDefeated) AppColors.goldColor
                                        else AppColors.textIconsColor.copy(0.15f)))
                }
            }
            if (run.blessings.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    run.blessings.forEach { blessing ->
                        Surface(shape = RoundedCornerShape(8.dp), color = AppColors.epicColor.copy(0.3f)) {
                            Text(blessing.displayName, Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                 color = AppColors.textIconsColor, style = MaterialTheme.typography.labelSmall,
                                 fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeBanner(text: String) {
    Card(Modifier.fillMaxWidth(),
         colors = CardDefaults.cardColors(containerColor = AppColors.successBackground)) {
        Text(text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall,
             color = AppColors.successColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ReorderableTeamStrip(team: List<RoguePokemon>, onReorder: (Int, Int) -> Unit, onManage: () -> Unit) {
    val slot = 84.dp
    val slotPx = with(LocalDensity.current) { slot.toPx() }
    var dragging by remember { mutableStateOf(-1) }
    var dragDx   by remember { mutableStateOf(0f) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tu equipo (${team.count { it.isAlive }}/${team.size} en pie) — arrastrá para reordenar",
                     style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                     modifier = Modifier.weight(1f))
                TextButton(onClick = onManage) { Text("Equipo y mochila") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                team.forEachIndexed { index, member ->
                    val isDragged = index == dragging
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(slot)
                            .offset { IntOffset(if (isDragged) dragDx.roundToInt() else 0, 0) }
                            .pointerInput(member.species.pokeId, team.size) {
                                detectDragGestures(
                                    onDragStart = { dragging = index; dragDx = 0f },
                                    onDrag = { change, drag -> change.consume(); dragDx += drag.x },
                                    onDragEnd = {
                                        val shift  = (dragDx / slotPx).roundToInt()
                                        val target = (index + shift).coerceIn(0, team.lastIndex)
                                        if (target != index) onReorder(index, target)
                                        dragging = -1; dragDx = 0f
                                    },
                                    onDragCancel = { dragging = -1; dragDx = 0f }
                                )
                            }
                    ) {
                        Box(Modifier.size(50.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (isDragged) AppColors.goldColor.copy(0.25f) else Color.Transparent),
                            contentAlignment = Alignment.Center) {
                            RogueSprite(member, 46.dp)
                        }
                        Text("${index + 1}· Nv ${member.level}", style = MaterialTheme.typography.labelSmall,
                             color = if (member.isAlive) AppColors.textPrimary else AppColors.textSecondary)
                        Text(if (member.isAlive) "${member.currentHp}/${member.maxHp}" else "Debilitado",
                             style = MaterialTheme.typography.labelSmall,
                             color = if (member.isAlive) AppColors.successColor else AppColors.defeatColor)
                        member.item?.let {
                            Text("⚙ ${it.name}", style = MaterialTheme.typography.labelSmall,
                                 color = AppColors.infoColor, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun LevelBadge(level: Int) {
    Surface(shape = RoundedCornerShape(6.dp), color = AppColors.goldColor.copy(alpha = 0.85f)) {
        Text("Nv $level", Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
             color = AppColors.textPrimary, fontWeight = FontWeight.ExtraBold,
             style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun HpBar(fraction: Float, current: Int, max: Int) {
    val color = when {
        fraction > 0.5f  -> AppColors.successColor
        fraction > 0.25f -> AppColors.coinColor
        else             -> AppColors.defeatColor
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(2.dp)) {
        LinearProgressIndicator(progress = fraction.coerceIn(0f, 1f), color = color,
            trackColor = AppColors.textIconsColor.copy(0.15f),
            modifier = Modifier.width(130.dp).height(8.dp).clip(RoundedCornerShape(4.dp)))
        Text("$current / $max HP", color = AppColors.textIconsColor.copy(0.8f),
             style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun XpBar(fraction: Float) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(450), label = "xp")
    LinearProgressIndicator(progress = animated, color = AppColors.infoColor,
        trackColor = AppColors.textIconsColor.copy(0.12f),
        modifier = Modifier.width(130.dp).height(4.dp).clip(RoundedCornerShape(2.dp)))
}

@Composable
private fun FloatingDamage(damage: Int?, trigger: Int) {
    val anim    = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val fade    = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0 || damage == null) return@LaunchedEffect
        offsetY.snapTo(0f); fade.snapTo(1f)
        anim.launch { offsetY.animateTo(-44f, tween(700)) }
        fade.animateTo(0f, tween(700))
    }
    if (damage != null) {
        Text("-$damage", color = AppColors.accentColor, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp,
             modifier = Modifier.offset(y = offsetY.value.dp).alpha(fade.value))
    }
}

@Composable
private fun RogueSprite(pokemon: RoguePokemon, size: Dp) {
    Box(contentAlignment = Alignment.Center) {
        KamelImage(asyncPainterResource(pokemon.species.imageUrl), pokemon.species.displayName,
            contentScale = ContentScale.Fit, modifier = Modifier.size(size),
            onLoading = { CircularProgressIndicator(Modifier.size(size / 3)) },
            onFailure = { CardImagePlaceholder(size / 2) })
        if (!pokemon.isAlive) {
            Box(Modifier.size(size).clip(CircleShape).background(AppColors.legendaryDark.copy(alpha = 0.55f)))
        }
    }
}
