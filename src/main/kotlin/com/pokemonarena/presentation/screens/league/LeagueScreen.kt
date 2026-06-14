package com.pokemonarena.presentation.screens.league

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokemonarena.domain.entity.BattleFatigue
import com.pokemonarena.domain.entity.BattleRewards
import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.Region
import com.pokemonarena.domain.entity.TeamRules
import com.pokemonarena.domain.entity.WeatherCondition
import com.pokemonarena.presentation.navigation.Navigator
import com.pokemonarena.presentation.navigation.Screen
import com.pokemonarena.presentation.screens.battle.BattleCombatContent
import com.pokemonarena.presentation.screens.battle.BattleStrategyContent
import com.pokemonarena.presentation.theme.AppColors
import com.pokemonarena.presentation.theme.AppIcons
import com.pokemonarena.presentation.utils.CardSprite
import com.pokemonarena.presentation.utils.CoinText
import com.pokemonarena.presentation.utils.HeldItemControls
import com.pokemonarena.presentation.utils.LoadingIndicator
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueScreen(viewModel: LeagueViewModel, navigator: Navigator, region: Region) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(region) { viewModel.onEvent(LeagueUiEvent.Load(region)) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Liga Pokémon de ${region.displayName}") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = AppColors.legendaryDark,
                titleContentColor = AppColors.goldColor))
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is LeagueUiState.Loading  -> LoadingIndicator()
                is LeagueUiState.Error    -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(s.message) }
                is LeagueUiState.Locked   -> LockedContent(s)
                is LeagueUiState.Lobby    -> LobbyContent(s) { viewModel.onEvent(LeagueUiEvent.Start) }
                is LeagueUiState.Prep     -> PrepContent(s, viewModel)
                is LeagueUiState.Strategy -> BattleStrategyContent(
                    gym = s.arena, teamCards = s.team, botCards = s.botTeam,
                    weather = WeatherCondition.CLEAR,
                    fightLabel = "¡PELEAR contra ${s.opponent.name}!",
                    onMove = { index, up -> viewModel.onEvent(LeagueUiEvent.MoveCard(index, up)) },
                    onFight = { viewModel.onEvent(LeagueUiEvent.ConfirmFight) })
                is LeagueUiState.Combat   -> BattleCombatContent(
                    result = s.result, weather = WeatherCondition.CLEAR,
                    itemDropNotice = s.itemDropNotice,
                    finalLabel = when {
                        !s.result.playerWon  -> "Terminar el desafío"
                        s.isLastOpponent     -> "Reclamar el campeonato"
                        else                 -> "Preparar el siguiente combate"
                    },
                    onContinue = { viewModel.onEvent(LeagueUiEvent.Continue) })
                is LeagueUiState.Finished -> FinishedContent(s, navigator) {
                    viewModel.onEvent(LeagueUiEvent.Load(region))
                }
            }
        }
    }
}

@Composable
private fun TrainerPortrait(imageUrl: String?, name: String, size: androidx.compose.ui.unit.Dp) {
    if (imageUrl != null) {
        KamelImage(asyncPainterResource(imageUrl), name,
            contentScale = ContentScale.Fit, modifier = Modifier.size(size),
            onLoading = {}, onFailure = {
                Icon(AppIcons.trophy, contentDescription = name,
                     tint = AppColors.goldColor, modifier = Modifier.size(size / 2))
            })
    } else {
        Icon(AppIcons.trophy, contentDescription = name,
             tint = AppColors.goldColor, modifier = Modifier.size(size / 2))
    }
}

@Composable
private fun LockedContent(state: LeagueUiState.Locked) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Filled.Lock, contentDescription = "Bloqueado",
                 tint = AppColors.dividerColor, modifier = Modifier.size(56.dp))
            Text("Liga bloqueada", style = MaterialTheme.typography.titleLarge,
                 fontWeight = FontWeight.ExtraBold)
            Text(state.message, textAlign = TextAlign.Center,
                 color = AppColors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun LobbyContent(state: LeagueUiState.Lobby, onStart: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
           horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(AppIcons.trophy, contentDescription = null,
             tint = AppColors.goldColor, modifier = Modifier.size(56.dp))
        Text(state.league.name, style = MaterialTheme.typography.headlineSmall,
             fontWeight = FontWeight.ExtraBold)
        if (state.alreadyChampion)
            Text("Ya sos campeón de esta Liga. Podés volver a desafiarla.",
                 color = AppColors.successColor, fontWeight = FontWeight.Bold,
                 style = MaterialTheme.typography.bodySmall)
        Text("${state.league.opponents.size} combates seguidos. Entre cada uno podés reorganizar tu equipo, " +
             "equipar items o curar fatiga — pero no comprar nada. Si perdés uno, el desafío termina.",
             textAlign = TextAlign.Center, color = AppColors.textSecondary,
             style = MaterialTheme.typography.bodySmall)
        state.league.opponents.forEachIndexed { i, opp ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}.", fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(26.dp))
                    TrainerPortrait(opp.imageUrl, opp.name, 56.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(opp.name, fontWeight = FontWeight.Bold)
                        Text("Especialista en ${opp.specialty}",
                             style = MaterialTheme.typography.labelSmall, color = AppColors.textSecondary)
                    }
                    Row { opp.cardPool.take(4).forEach { CardSprite(it, imageSize = 40.dp) } }
                }
            }
        }
        CoinText("Bono por conquistarla por primera vez: +${BattleRewards.FIRST_WIN_BONUS} y un item exclusivo",
                 color = AppColors.coinColor, style = MaterialTheme.typography.labelMedium)
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp),
               shape = RoundedCornerShape(12.dp),
               colors = ButtonDefaults.buttonColors(containerColor = AppColors.goldColor,
                                                    contentColor = AppColors.textPrimary)) {
            Text(if (state.alreadyChampion) "Desafiar de nuevo" else "Comenzar el desafío",
                 fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun PrepContent(state: LeagueUiState.Prep, viewModel: LeagueViewModel) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AppColors.legendaryMid)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                TrainerPortrait(state.opponent.imageUrl, state.opponent.name, 52.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Rival ${state.opponentIndex + 1}/${state.league.opponents.size}: ${state.opponent.name}",
                         color = AppColors.goldColor, fontWeight = FontWeight.ExtraBold)
                    Text("Especialista en ${state.opponent.specialty} · Preparate y peleá",
                         color = AppColors.textIconsColor.copy(0.7f),
                         style = MaterialTheme.typography.labelSmall)
                }
                Row { state.opponent.cardPool.take(4).forEach { CardSprite(it, imageSize = 38.dp) } }
            }
        }

        Text("Tu equipo (${state.team.size}/3) — tocá una carta para sumarla o sacarla",
             style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(state.owned, key = { it.id }) { card ->
                PrepCardRow(card,
                    inTeam  = state.team.any { it.id == card.id },
                    blocked = state.team.size >= TeamRules.SIZE && state.team.none { it.id == card.id },
                    availableItems = state.availableItems,
                    fatigueCures   = state.fatigueCures,
                    onToggle  = { viewModel.onEvent(LeagueUiEvent.ToggleCard(card.id)) },
                    onEquip   = { viewModel.onEvent(LeagueUiEvent.EquipItem(card.id, it)) },
                    onUnequip = { viewModel.onEvent(LeagueUiEvent.UnequipItem(card.id)) },
                    onCure    = { viewModel.onEvent(LeagueUiEvent.CureFatigue(card.id)) })
            }
        }

        Button(onClick = { viewModel.onEvent(LeagueUiEvent.Fight) },
               enabled = state.canFight,
               modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
            Text(if (state.canFight) "Armar los cruces contra ${state.opponent.name}"
                 else "Necesitás 3 cartas en el equipo", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun PrepCardRow(card: Card, inTeam: Boolean, blocked: Boolean,
                        availableItems: List<Pair<com.pokemonarena.domain.entity.Item, Int>>,
                        fatigueCures: Int,
                        onToggle: () -> Unit, onEquip: (String) -> Unit,
                        onUnequip: () -> Unit, onCure: () -> Unit) {
    Card(
        border = if (inTeam) BorderStroke(2.dp, AppColors.goldColor) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (inTeam) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(enabled = !blocked || inTeam, onClick = onToggle)
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (inTeam) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                 contentDescription = null, modifier = Modifier.size(18.dp),
                 tint = if (inTeam) MaterialTheme.colorScheme.primary else AppColors.dividerColor)
            Spacer(Modifier.width(8.dp))
            CardSprite(card, imageSize = 44.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(card.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("BST ${card.effectiveStats.total}", style = MaterialTheme.typography.labelSmall,
                         color = if (card.effectiveStats.total < card.stats.total) AppColors.defeatColor
                                 else AppColors.textSecondary)
                    if (card.timesUsed > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text("Fatiga -${BattleFatigue.penaltyPercent(card.timesUsed)}%",
                             style = MaterialTheme.typography.labelSmall, color = AppColors.defeatColor)
                        if (fatigueCures > 0) {
                            TextButton(onClick = onCure, modifier = Modifier.height(22.dp),
                                       contentPadding = PaddingValues(horizontal = 4.dp)) {
                                Text("Curar (x$fatigueCures)", fontSize = 10.sp,
                                     color = AppColors.successColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            HeldItemControls(card, availableItems, onEquip, onUnequip)
        }
    }
}

@Composable
private fun FinishedContent(state: LeagueUiState.Finished, navigator: Navigator, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(32.dp)) {
            Icon(AppIcons.trophy, contentDescription = null, modifier = Modifier.size(64.dp),
                 tint = if (state.won) AppColors.goldColor else AppColors.dividerColor)
            Text(if (state.won) "¡CAMPEÓN de ${state.league.name}!"
                 else "Desafío terminado: ${state.defeated}/${state.league.opponents.size} rivales vencidos",
                 style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold,
                 textAlign = TextAlign.Center)
            state.reward?.let { reward ->
                CoinText("Medalla de la Liga conseguida · +${reward.coins} monedas",
                         color = AppColors.coinColor)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KamelImage(asyncPainterResource(reward.item.imageUrl), reward.item.name,
                        contentScale = ContentScale.Fit, modifier = Modifier.size(32.dp),
                        onLoading = {}, onFailure = {})
                    Spacer(Modifier.width(6.dp))
                    Text("Item exclusivo: ${reward.item.name}",
                         fontWeight = FontWeight.Bold, color = AppColors.successColor,
                         style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRetry) { Text(if (state.won) "Volver al lobby" else "Reintentar") }
                OutlinedButton(onClick = { navigator.navigateTo(Screen.Home) }) { Text("Inicio") }
            }
        }
    }
}
