package com.pokemonarena.presentation.screens.rogue

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokemonarena.domain.entity.RogueMove
import com.pokemonarena.domain.entity.RogueItem
import com.pokemonarena.domain.entity.RogueNodeType
import com.pokemonarena.domain.entity.RoguePokemon
import com.pokemonarena.domain.entity.RogueRules
import com.pokemonarena.domain.entity.TypeMatchup
import com.pokemonarena.domain.usecase.RogueStrike
import com.pokemonarena.presentation.theme.AppColors
import com.pokemonarena.presentation.theme.AppIcons
import com.pokemonarena.presentation.theme.typeColors
import com.pokemonarena.presentation.utils.CardImagePlaceholder
import com.pokemonarena.presentation.utils.CoinText
import com.pokemonarena.presentation.utils.TypeBadge
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RogueScreen(viewModel: RogueViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text("Expedición Rogue") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = AppColors.legendaryDark,
                titleContentColor = AppColors.goldColor))
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is RogueUiState.Idle       -> IdleContent { viewModel.onEvent(RogueUiEvent.Start) }
                is RogueUiState.Draft      -> DraftContent(s, viewModel)
                is RogueUiState.PathChoice -> PathContent(s, viewModel)
                is RogueUiState.Battle     -> BattleContent(s, viewModel)
                is RogueUiState.Reward     -> RewardContent(s, viewModel)
                is RogueUiState.EquipGear  -> EquipContent(s, viewModel)
                is RogueUiState.Finished   -> FinishedContent(s) {
                    viewModel.onEvent(RogueUiEvent.BackToIdle)
                }
            }
        }
    }
}

// ---------- Intro ----------

@Composable
private fun IdleContent(onStart: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
           horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(AppIcons.streak, contentDescription = null,
             tint = AppColors.goldColor, modifier = Modifier.size(56.dp))
        Text("Expedición Rogue", style = MaterialTheme.typography.headlineSmall,
             fontWeight = FontWeight.ExtraBold)
        Text("Un ascenso de ${RogueRules.FLOORS} pisos contra rivales con Armadura Argumental™. " +
             "Atención: estos combates NO se pueden ganar. En serio. El objetivo es sobrevivir " +
             "y escapar el mayor tiempo posible… hasta el jefe.",
             textAlign = TextAlign.Center, color = AppColors.textSecondary,
             style = MaterialTheme.typography.bodyMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RuleLine("Elegís 1 de ${RogueRules.DRAFT_SIZE} Pokémon iniciales al azar. Después reclutás más.")
                RuleLine("El rival es INVENCIBLE: cada golpe letal lo deja en 1 HP… y lo enfurece. Insistir lo empeora.")
                RuleLine("No podés ganar, pero podés HUIR: aguantá ${RogueRules.TURNS_TO_ESCAPE} turnos y escapás al siguiente piso.")
                RuleLine("¿Apurado? Gastá una de tus ${RogueRules.HOPE_TOKENS_START} Fichas de Esperanza para huir ya. Son escasas: usalas con cabeza.")
                RuleLine("Subís de nivel, aprendés ataques, juntás bendiciones y equipás items: los vas a necesitar para aguantar.")
                RuleLine("El HP persiste entre combates. Un Pokémon debilitado no vuelve en toda la expedición.")
                RuleLine("En la cima espera el jefe legendario. No se puede vencer NI huir. Es el final, y es cruel.")
                RuleLine("Cobrás la mitad del botín que juntes, ganes o no (spoiler: no). Nunca perdés monedas.")
            }
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp),
               shape = RoundedCornerShape(12.dp),
               colors = ButtonDefaults.buttonColors(containerColor = AppColors.goldColor,
                                                    contentColor = AppColors.textPrimary)) {
            Text("Comenzar la expedición", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun RuleLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("•", color = AppColors.goldColor, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = AppColors.textPrimary)
    }
}

// ---------- Draft ----------

@Composable
private fun DraftContent(state: RogueUiState.Draft, viewModel: RogueViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp),
           horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Elegí tu inicial", style = MaterialTheme.typography.titleLarge,
             fontWeight = FontWeight.ExtraBold)
        Text("Será tu único compañero hasta que reclutes más en el camino.",
             color = AppColors.textSecondary, style = MaterialTheme.typography.bodySmall)
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
            Spacer(Modifier.height(2.dp))
            Text("Ataques: ${pokemon.moves.joinToString { it.name }}",
                 style = MaterialTheme.typography.labelSmall, color = AppColors.textSecondary)
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

// ---------- Elección de camino ----------

@Composable
private fun PathContent(state: RogueUiState.PathChoice, viewModel: RogueViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
           verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RunHeader(state.run)
        state.notice?.let {
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.successBackground)) {
                Text(it, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall,
                     color = AppColors.successColor, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(if (state.run.floor >= RogueRules.FLOORS) "La cima te espera…"
             else "Piso ${state.run.floor}: elegí tu camino",
             style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        state.options.forEachIndexed { index, node ->
            NodeCard(node) { viewModel.onEvent(RogueUiEvent.PickNode(index)) }
        }
        TeamSummary(state.run)
        TextButton(onClick = { viewModel.onEvent(RogueUiEvent.Abandon) }) {
            Text("Abandonar expedición (cobrás la mitad del botín)",
                 color = AppColors.textSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun NodeCard(node: RogueNodeType, onPick: () -> Unit) {
    val highlight = node == RogueNodeType.BOSS || node == RogueNodeType.ELITE
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onPick),
        border = if (highlight) BorderStroke(2.dp, AppColors.goldColor) else null,
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(nodeIcon(node), contentDescription = node.displayName,
                 tint = if (highlight) AppColors.goldColor else MaterialTheme.colorScheme.primary,
                 modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(node.displayName, fontWeight = FontWeight.ExtraBold)
                Text(node.description, style = MaterialTheme.typography.bodySmall,
                     color = AppColors.textSecondary)
            }
        }
    }
}

private fun nodeIcon(node: RogueNodeType): ImageVector = when (node) {
    RogueNodeType.FIGHT    -> AppIcons.battle
    RogueNodeType.ELITE    -> AppIcons.streak
    RogueNodeType.REST     -> Icons.Filled.Favorite
    RogueNodeType.TREASURE -> AppIcons.coin
    RogueNodeType.DOJO     -> AppIcons.rate
    RogueNodeType.BOSS     -> AppIcons.trophy
}

// ---------- Batalla ----------

@Composable
private fun BattleContent(state: RogueUiState.Battle, viewModel: RogueViewModel) {
    var animating by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RunHeader(state.run)
        BattleArena(state) { animating = it }

        state.taunt?.let { TauntBanner(it) }
        SurvivalMeter(state)

        if (state.awaitingSwap) {
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.defeatBackground)) {
                Text("¡Tu Pokémon se debilitó! Elegí al siguiente tocándolo en la banca.",
                     Modifier.padding(12.dp), color = AppColors.defeatColor,
                     fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            }
        }

        BenchRow(state, viewModel)
        MoveButtons(state.active.moves, enabled = !state.awaitingSwap && !animating) {
            viewModel.onEvent(RogueUiEvent.Attack(it))
        }
        EscapeControls(state, viewModel, enabled = !animating)
        BattleLog(state.log, Modifier.weight(1f))
    }
}

@Composable
private fun TauntBanner(taunt: String) {
    Card(colors = CardDefaults.cardColors(containerColor = AppColors.epicColor.copy(alpha = 0.18f))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null,
                 tint = AppColors.epicColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(taunt, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
                 color = AppColors.epicColor)
        }
    }
}

@Composable
private fun SurvivalMeter(state: RogueUiState.Battle) {
    if (state.isBoss) {
        Card(colors = CardDefaults.cardColors(containerColor = AppColors.legendaryDark)) {
            Text("⚠ Jefe Final: no se puede vencer ni huir. Solo queda resistir lo inevitable.",
                 Modifier.padding(12.dp), color = AppColors.goldColor,
                 fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    val progress by animateFloatAsState(
        (state.turnsSurvived.toFloat() / RogueRules.TURNS_TO_ESCAPE).coerceIn(0f, 1f),
        tween(400), label = "survival")
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(if (state.canFlee) "¡Aguantaste lo suficiente! Ya podés huir."
             else "Resistencia para huir: faltan ${state.turnsToEscape} turnos",
             style = MaterialTheme.typography.labelSmall,
             color = if (state.canFlee) AppColors.successColor else AppColors.textSecondary,
             fontWeight = FontWeight.SemiBold)
        LinearProgressIndicator(
            progress   = progress,
            color      = if (state.canFlee) AppColors.successColor else AppColors.coinColor,
            trackColor = AppColors.dividerColor.copy(0.4f),
            modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
    }
}

@Composable
private fun EscapeControls(state: RogueUiState.Battle, viewModel: RogueViewModel, enabled: Boolean) {
    if (state.isBoss) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { viewModel.onEvent(RogueUiEvent.Flee) },
               enabled = enabled && state.canFlee && !state.awaitingSwap,
               modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp),
               colors = ButtonDefaults.buttonColors(containerColor = AppColors.successColor)) {
            Text("HUIR", fontWeight = FontWeight.ExtraBold)
        }
        OutlinedButton(onClick = { viewModel.onEvent(RogueUiEvent.SpendHope) },
               enabled = enabled && state.canSpendHope && !state.awaitingSwap,
               modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp)) {
            Text("Ficha (${state.run.hopeTokens}) ✦", fontWeight = FontWeight.Bold,
                 style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun BattleArena(state: RogueUiState.Battle, onAnimating: (Boolean) -> Unit) {
    val active = state.active
    val enemy  = state.enemy

    // Las barras de HP se interpolan solas hacia el valor final del estado.
    val playerHp by animateFloatAsState(active.hpFraction, tween(450), label = "playerHp")
    val enemyHp  by animateFloatAsState(enemy.hpFraction,  tween(450), label = "enemyHp")

    val anim = rememberCoroutineScope()
    val playerLunge = remember { Animatable(0f) }
    val enemyLunge  = remember { Animatable(0f) }
    var playerDmg   by remember { mutableStateOf<Int?>(null) }
    var enemyDmg    by remember { mutableStateOf<Int?>(null) }
    var playerDmgKey by remember { mutableStateOf(0) }
    var enemyDmgKey  by remember { mutableStateOf(0) }

    LaunchedEffect(state.turnId) {
        if (state.turnId == 0) return@LaunchedEffect
        onAnimating(true)
        state.lastTurn.forEach { strike ->
            if (strike.isPlayerAttack) {
                anim.launch { playerLunge.animateTo(22f, tween(110)); playerLunge.animateTo(0f, tween(220)) }
                enemyDmg = strike.damage; enemyDmgKey++
            } else {
                anim.launch { enemyLunge.animateTo(-22f, tween(110)); enemyLunge.animateTo(0f, tween(220)) }
                playerDmg = strike.damage; playerDmgKey++
            }
            delay(680)
        }
        onAnimating(false)
    }

    Card(Modifier.fillMaxWidth(),
         colors = CardDefaults.cardColors(containerColor = AppColors.legendaryMid)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CombatantColumn(active, hpFraction = playerHp, isEnemy = false,
                            lungeX = playerLunge.value, damage = playerDmg, damageKey = playerDmgKey,
                            modifier = Modifier.weight(1f))
            Text("VS", color = AppColors.goldColor, fontWeight = FontWeight.ExtraBold,
                 fontSize = 22.sp, modifier = Modifier.padding(horizontal = 8.dp))
            CombatantColumn(enemy, hpFraction = enemyHp, isEnemy = true,
                            lungeX = enemyLunge.value, damage = enemyDmg, damageKey = enemyDmgKey,
                            modifier = Modifier.weight(1f), label = state.node.displayName)
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
            Box(Modifier.offset(x = lungeX.dp, y = bob.dp)) { RogueSprite(pokemon, 90.dp) }
            FloatingDamage(damage, damageKey)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(pokemon.species.displayName, color = AppColors.textIconsColor,
                 fontWeight = FontWeight.ExtraBold)
            if (!isEnemy) {
                Spacer(Modifier.width(6.dp))
                LevelBadge(pokemon.level)
            }
        }
        Row { pokemon.species.types.forEach { TypeBadge(it) } }
        HpBar(hpFraction, pokemon.currentHp, pokemon.maxHp)
        if (!isEnemy) {
            XpBar(pokemon.xpFraction)
            Text("ATQ ${pokemon.attack} · DEF ${pokemon.defense} · VEL ${pokemon.speed}",
                 color = AppColors.textIconsColor.copy(0.6f),
                 style = MaterialTheme.typography.labelSmall)
            pokemon.item?.let {
                Text("⚙ ${it.name}", color = AppColors.infoColor,
                     style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FloatingDamage(damage: Int?, trigger: Int) {
    val anim    = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val fade    = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0 || damage == null) return@LaunchedEffect
        offsetY.snapTo(0f); fade.snapTo(1f)
        anim.launch { offsetY.animateTo(-46f, tween(700)) }
        fade.animateTo(0f, tween(700))
    }
    if (damage != null) {
        Text("-$damage", color = AppColors.accentColor, fontWeight = FontWeight.ExtraBold,
             fontSize = 22.sp,
             modifier = Modifier.offset(y = offsetY.value.dp).alpha(fade.value))
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
        LinearProgressIndicator(
            progress   = fraction.coerceIn(0f, 1f),
            color      = color,
            trackColor = AppColors.textIconsColor.copy(0.15f),
            modifier   = Modifier.width(130.dp).height(8.dp).clip(RoundedCornerShape(4.dp))
        )
        Text("$current / $max HP", color = AppColors.textIconsColor.copy(0.8f),
             style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun XpBar(fraction: Float) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(450), label = "xp")
    LinearProgressIndicator(
        progress   = animated,
        color      = AppColors.infoColor,
        trackColor = AppColors.textIconsColor.copy(0.12f),
        modifier   = Modifier.width(130.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
    )
}

@Composable
private fun MoveButtons(moves: List<RogueMove>, enabled: Boolean, onMove: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        moves.withIndex().chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (index, move) ->
                    MoveButton(move, enabled, Modifier.weight(1f)) { onMove(index) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MoveButton(move: RogueMove, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = typeColors[move.type] ?: Color.Gray
    Button(onClick = onClick, enabled = enabled, modifier = modifier.height(48.dp),
           shape = RoundedCornerShape(12.dp),
           colors = ButtonDefaults.buttonColors(containerColor = color,
                                                contentColor = Color.White)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(move.name, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            Text(move.type.replaceFirstChar { it.uppercase() },
                 style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.85f))
        }
    }
}

@Composable
private fun BenchRow(state: RogueUiState.Battle, viewModel: RogueViewModel) {
    if (state.run.team.size <= 1) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        state.run.team.forEachIndexed { index, member ->
            val isActive = index == state.run.activeIndex
            Card(
                border = if (isActive) BorderStroke(2.dp, AppColors.goldColor) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (member.isAlive) MaterialTheme.colorScheme.surface
                                     else AppColors.surfaceVariant),
                modifier = Modifier.clickable(enabled = member.isAlive && !isActive) {
                    viewModel.onEvent(RogueUiEvent.SetActive(index))
                }
            ) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    RogueSprite(member, 40.dp)
                    Text("${member.species.displayName} · Nv ${member.level}",
                         style = MaterialTheme.typography.labelSmall,
                         fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                         color = if (member.isAlive) AppColors.textPrimary else AppColors.textSecondary)
                    Text(if (member.isAlive) "${member.currentHp} HP" else "Debilitado",
                         style = MaterialTheme.typography.labelSmall,
                         color = if (member.isAlive) AppColors.successColor else AppColors.defeatColor)
                }
            }
        }
        if (!state.awaitingSwap) {
            Text("Cambiar cuesta el turno:\nel rival pega gratis.",
                 style = MaterialTheme.typography.labelSmall, color = AppColors.textSecondary)
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
                     color = if (strike.isPlayerAttack) AppColors.successColor
                             else AppColors.defeatColor)
            }
        }
    }
}

private fun strikeText(strike: RogueStrike): String {
    val effectiveness = when {
        strike.typeMultiplier > TypeMatchup.NEUTRAL -> " ¡Muy eficaz!"
        strike.typeMultiplier < TypeMatchup.NEUTRAL -> " Poco eficaz…"
        else                                        -> ""
    }
    val faint = if (strike.defenderFainted) " ¡${strike.defenderName} se debilitó!" else ""
    return "${strike.attackerName} usó ${strike.moveName}: -${strike.damage} HP a ${strike.defenderName}.$effectiveness$faint"
}

// ---------- Recompensas ----------

@Composable
private fun RewardContent(state: RogueUiState.Reward, viewModel: RogueViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
           verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RunHeader(state.run)
        Text("Escapaste con vida — agarrá algo antes de seguir",
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
    Card(Modifier.fillMaxWidth().clickable(onClick = onPick),
         elevation = CardDefaults.cardElevation(3.dp)) {
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
                is RogueRewardOption.Heal -> {
                    Icon(Icons.Filled.Favorite, contentDescription = null,
                         tint = AppColors.successColor, modifier = Modifier.size(36.dp))
                    Column {
                        Text("Curación", fontWeight = FontWeight.ExtraBold)
                        Text("Los Pokémon en pie recuperan el ${(RogueRules.REWARD_HEAL_FRACTION * 100).toInt()}% de su HP.",
                             style = MaterialTheme.typography.bodySmall, color = AppColors.textSecondary)
                    }
                }
                is RogueRewardOption.Blessing -> {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null,
                         tint = AppColors.epicColor, modifier = Modifier.size(36.dp))
                    Column {
                        Text(option.blessing.displayName, fontWeight = FontWeight.ExtraBold,
                             color = AppColors.epicColor)
                        Text(option.blessing.description,
                             style = MaterialTheme.typography.bodySmall, color = AppColors.textSecondary)
                    }
                }
                is RogueRewardOption.Loot -> {
                    Icon(AppIcons.coin, contentDescription = null,
                         tint = AppColors.coinColor, modifier = Modifier.size(36.dp))
                    Column {
                        Text("Botín extra", fontWeight = FontWeight.ExtraBold)
                        CoinText("+${option.coins} al botín de la expedición")
                    }
                }
                is RogueRewardOption.Gear -> {
                    Icon(AppIcons.cards, contentDescription = null,
                         tint = AppColors.infoColor, modifier = Modifier.size(36.dp))
                    Column {
                        Text("Item: ${option.item.name}", fontWeight = FontWeight.ExtraBold,
                             color = AppColors.infoColor)
                        Text(option.item.description,
                             style = MaterialTheme.typography.bodySmall, color = AppColors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun EquipContent(state: RogueUiState.EquipGear, viewModel: RogueViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
           verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RunHeader(state.run)
        Card(colors = CardDefaults.cardColors(containerColor = AppColors.infoColor.copy(alpha = 0.12f))) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(AppIcons.cards, contentDescription = null,
                     tint = AppColors.infoColor, modifier = Modifier.size(36.dp))
                Column {
                    Text(state.item.name, fontWeight = FontWeight.ExtraBold, color = AppColors.infoColor)
                    Text(state.item.description, style = MaterialTheme.typography.bodySmall,
                         color = AppColors.textSecondary)
                    Text(gearBonusLabel(state.item), style = MaterialTheme.typography.labelSmall,
                         color = AppColors.successColor, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("¿A quién se lo equipás?", style = MaterialTheme.typography.titleMedium,
             fontWeight = FontWeight.ExtraBold)
        state.run.team.forEachIndexed { index, member ->
            Card(Modifier.fillMaxWidth().clickable { viewModel.onEvent(RogueUiEvent.EquipOn(index)) },
                 elevation = CardDefaults.cardElevation(3.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RogueSprite(member, 48.dp)
                    Column(Modifier.weight(1f)) {
                        Text("${member.species.displayName} · Nv ${member.level}",
                             fontWeight = FontWeight.Bold)
                        Text("HP ${member.maxHp} · ATQ ${member.attack} · DEF ${member.defense} · VEL ${member.speed}",
                             style = MaterialTheme.typography.labelSmall, color = AppColors.textSecondary)
                        member.item?.let {
                            Text("Ya lleva: ${it.name} (se le sumará el nuevo)",
                                 style = MaterialTheme.typography.labelSmall, color = AppColors.coinColor)
                        }
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

// ---------- Final ----------

@Composable
private fun FinishedContent(state: RogueUiState.Finished, onBack: () -> Unit) {
    val scale by animateFloatAsState(1f, tween(500), label = "trophy")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.spacedBy(12.dp),
               modifier = Modifier.padding(32.dp)) {
            val reachedBoss = state.run.floor >= RogueRules.FLOORS
            Icon(AppIcons.trophy, contentDescription = null,
                 modifier = Modifier.size(64.dp).scale(scale),
                 tint = AppColors.dividerColor)
            Text(if (reachedBoss) "El jefe te aplastó (como estaba escrito)"
                 else "La expedición terminó en el piso ${state.run.floor}",
                 style = MaterialTheme.typography.headlineSmall,
                 fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text(if (reachedBoss)
                     "Llegaste hasta la cima… para descubrir que no había forma de ganar. " +
                     "Nunca la hubo. Pero qué viaje, ¿no? Rescatás la mitad del botín."
                 else "Tu equipo cayó. Rescatás la mitad del botín que juntaste. " +
                      "Ganar no era una opción; sobrevivir un poco más, sí.",
                 color = AppColors.textSecondary, style = MaterialTheme.typography.bodyMedium,
                 textAlign = TextAlign.Center)
            CoinText("+${state.payout} monedas", color = AppColors.coinColor,
                     style = MaterialTheme.typography.titleLarge, iconSize = 24.dp)
            Button(onClick = onBack, shape = RoundedCornerShape(12.dp)) {
                Text("Volver a la entrada", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ---------- Comunes ----------

@Composable
private fun RunHeader(run: RogueRunSnapshot) {
    Card(Modifier.fillMaxWidth(),
         colors = CardDefaults.cardColors(containerColor = AppColors.legendaryDark)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Piso ${run.floor.coerceAtMost(RogueRules.FLOORS)}/${RogueRules.FLOORS}",
                     color = AppColors.goldColor, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                Text("✦ ${run.hopeTokens}", color = AppColors.textIconsColor,
                     style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(12.dp))
                CoinText("Botín: ${run.loot}", color = AppColors.coinColor)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(RogueRules.FLOORS) { index ->
                    Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (index < run.floor) AppColors.goldColor
                                        else AppColors.textIconsColor.copy(0.15f)))
                }
            }
            if (run.blessings.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    run.blessings.forEach { blessing ->
                        Surface(shape = RoundedCornerShape(8.dp),
                                color = AppColors.epicColor.copy(alpha = 0.25f)) {
                            Text(blessing.displayName,
                                 Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                 color = AppColors.textIconsColor,
                                 style = MaterialTheme.typography.labelSmall,
                                 fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamSummary(run: RogueRunSnapshot) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Tu expedición (${run.team.count { it.isAlive }}/${run.team.size} en pie)",
                 style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                run.team.forEach { member ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RogueSprite(member, 48.dp)
                        Text("${member.species.displayName} · Nv ${member.level}",
                             style = MaterialTheme.typography.labelSmall,
                             color = if (member.isAlive) AppColors.textPrimary else AppColors.textSecondary)
                        Text(if (member.isAlive) "${member.currentHp}/${member.maxHp} HP" else "Debilitado",
                             style = MaterialTheme.typography.labelSmall,
                             color = if (member.isAlive) AppColors.successColor else AppColors.defeatColor)
                        member.item?.let {
                            Text("⚙ ${it.name}", style = MaterialTheme.typography.labelSmall,
                                 color = AppColors.infoColor)
                        }
                    }
                }
            }
        }
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
            Box(Modifier.size(size).clip(CircleShape)
                    .background(AppColors.legendaryDark.copy(alpha = 0.55f)))
        }
    }
}
