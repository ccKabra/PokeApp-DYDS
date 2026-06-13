package com.pokemonarena.presentation.screens.battle

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokemonarena.domain.entity.*
import com.pokemonarena.domain.usecase.SimulateBattleUseCase
import com.pokemonarena.presentation.theme.AppColors
import com.pokemonarena.presentation.theme.AppIcons
import com.pokemonarena.presentation.utils.*
import kotlinx.coroutines.delay

@Composable
fun BattleStrategyContent(
    gym: Gym, teamCards: List<Card>, botCards: List<Card>, weather: WeatherCondition,
    fightLabel: String, onMove: (Int, Boolean) -> Unit, onFight: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WeatherBanner(weather, gym)

        Text("Armá tus cruces", style = MaterialTheme.typography.titleMedium,
             fontWeight = FontWeight.ExtraBold, color = AppColors.textPrimary)
        Text("Cada ronda enfrenta tu carta contra la del rival en la misma fila. " +
             "Usá las flechas para reordenar tu equipo y buscar ventaja de tipo.",
             style = MaterialTheme.typography.bodySmall, color = AppColors.textSecondary)

        teamCards.forEachIndexed { i, playerCard ->
            val botCard = botCards.getOrNull(i) ?: return@forEachIndexed
            MatchupRow(
                roundNumber = i + 1,
                playerCard  = playerCard,
                botCard     = botCard,
                weather     = weather,
                canMoveUp   = i > 0,
                canMoveDown = i < teamCards.lastIndex,
                onMove      = { up -> onMove(i, up) }
            )
        }

        Button(
            onClick  = onFight,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(12.dp)
        ) {
            Icon(AppIcons.battle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(fightLabel, fontWeight = FontWeight.ExtraBold,
                 style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun WeatherBanner(weather: WeatherCondition, gym: Gym) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                WeatherLabel(weather, color = AppColors.textPrimary)
                if (weather.boostedTypes.isNotEmpty())
                    Text("Potencia: ${weather.boostedTypes.joinToString()} ×${weather.multiplier}",
                         style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.End) {
                DifficultyStars(gym.difficulty)
                CoinText("Premio hasta ${BattleRewards.maxRewardFor(gym.difficulty)}",
                         color = AppColors.textSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun DifficultyStars(difficulty: Int) {
    Row {
        repeat(5) { i ->
            Icon(AppIcons.star, contentDescription = null, modifier = Modifier.size(15.dp),
                 tint = if (i < difficulty) AppColors.coinColor else AppColors.dividerColor)
        }
    }
}

@Composable
private fun MatchupRow(
    roundNumber: Int, playerCard: Card, botCard: Card, weather: WeatherCondition,
    canMoveUp: Boolean, canMoveDown: Boolean, onMove: (Boolean) -> Unit
) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {

            Column {
                IconButton(onClick = { onMove(true) }, enabled = canMoveUp, modifier = Modifier.size(28.dp)) {
                    Text("▲", fontSize = 11.sp,
                         color = if (canMoveUp) MaterialTheme.colorScheme.primary else AppColors.dividerColor)
                }
                IconButton(onClick = { onMove(false) }, enabled = canMoveDown, modifier = Modifier.size(28.dp)) {
                    Text("▼", fontSize = 11.sp,
                         color = if (canMoveDown) MaterialTheme.colorScheme.primary else AppColors.dividerColor)
                }
            }

            MiniCard(playerCard, Modifier.weight(2f))

            Column(Modifier.weight(1.4f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("R$roundNumber", fontWeight = FontWeight.ExtraBold,
                     color = AppColors.textSecondary, style = MaterialTheme.typography.labelSmall)
                MultiplierHints(playerCard, botCard, weather)
            }

            MiniCard(botCard, Modifier.weight(2f), alignEnd = true)
        }
    }
}

@Composable
private fun MultiplierHints(playerCard: Card, botCard: Card, weather: WeatherCondition) {
    val typeMult    = TypeMatchup.multiplier(playerCard.primaryType, botCard.primaryType)
    val weatherMult = TypeEffectiveness.multiplierFor(playerCard.primaryType, weather)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            typeMult > 1f -> HintChip("Ventaja ×$typeMult", AppColors.successColor)
            typeMult < 1f -> HintChip("Desventaja ×$typeMult", AppColors.defeatColor)
            else          -> HintChip("Neutro", AppColors.drawColor)
        }
        weatherMult?.let {
            HintChip("Clima ×$it", if (it >= 1f) AppColors.infoColor else AppColors.defeatColor)
        }
    }
}

@Composable
private fun HintChip(text: String, color: Color) {
    Box(Modifier.padding(vertical = 1.dp).clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MiniCard(card: Card, modifier: Modifier = Modifier, alignEnd: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start) {
        if (!alignEnd) CardSprite(card, imageSize = 52.dp)
        Column(Modifier.padding(horizontal = 8.dp),
               horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
            Text(card.name, style = MaterialTheme.typography.bodySmall,
                 fontWeight = FontWeight.Bold, maxLines = 1, color = AppColors.textPrimary)
            Text("${card.primaryType.replaceFirstChar { it.uppercase() }} · BST ${card.effectiveStats.total}",
                 style = MaterialTheme.typography.labelSmall,
                 color = if (card.heldItem != null) AppColors.successColor else AppColors.textSecondary)
            card.heldItem?.let { item ->
                Text(item.name, style = MaterialTheme.typography.labelSmall,
                     fontWeight = FontWeight.Bold, color = AppColors.successColor)
            }
        }
        if (alignEnd) CardSprite(card, imageSize = 52.dp)
    }
}

@Composable
fun BattleCombatContent(
    result: BattleResult, weather: WeatherCondition,
    itemDropNotice: String?, finalLabel: String, onContinue: () -> Unit
) {
    var revealedRounds by remember(result) { mutableStateOf(0) }
    var showFinal      by remember(result) { mutableStateOf(false) }

    LaunchedEffect(result) {
        delay(400)
        repeat(result.rounds.size) {
            revealedRounds = it + 1
            delay(1100)
        }
        showFinal = true
    }

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemDropNotice?.let { notice ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(notice, Modifier.padding(12.dp),
                     color = MaterialTheme.colorScheme.onErrorContainer,
                     style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
        result.rounds.forEachIndexed { i, round ->
            AnimatedVisibility(
                visible = i < revealedRounds,
                enter   = slideInVertically(spring(Spring.DampingRatioMediumBouncy)) { it / 2 } + fadeIn()
            ) {
                RoundCard(i + 1, round, weather)
            }
        }

        AnimatedVisibility(showFinal, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) {
            FinalBanner(result, finalLabel, onContinue)
        }
    }
}

@Composable
private fun RoundCard(number: Int, round: RoundResult, weather: WeatherCondition) {
    var showMath by remember(round) { mutableStateOf(false) }
    val playerWins = round.winner == Winner.PLAYER
    val botWins    = round.winner == Winner.BOT
    val barColor = when (round.winner) {
        Winner.PLAYER -> AppColors.successBackground
        Winner.BOT    -> AppColors.defeatBackground
        Winner.DRAW   -> MaterialTheme.colorScheme.surface
    }
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp),
         colors = CardDefaults.cardColors(containerColor = barColor)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ronda $number", fontWeight = FontWeight.ExtraBold,
                     style = MaterialTheme.typography.labelMedium, color = AppColors.textSecondary)
                Spacer(Modifier.weight(1f))
                Text(when (round.winner) {
                    Winner.PLAYER -> "Turno ganado"
                    Winner.BOT    -> "Turno perdido"
                    Winner.DRAW   -> "Turno empatado"
                }, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                   color = when (round.winner) {
                       Winner.PLAYER -> AppColors.successColor
                       Winner.BOT    -> AppColors.defeatColor
                       Winner.DRAW   -> AppColors.drawColor
                   })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CombatSide(round.playerCard, round.playerScore, round.playerCrit,
                           round.playerMatchup, winner = playerWins, Modifier.weight(1f),
                           missed = round.playerMissed)
                Text("VS", fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 10.dp),
                     color = AppColors.textSecondary)
                CombatSide(round.botCard, round.botScore, round.botCrit,
                           round.botMatchup, winner = botWins, Modifier.weight(1f), alignEnd = true)
            }
            TextButton(onClick = { showMath = !showMath },
                       modifier = Modifier.height(26.dp),
                       contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(if (showMath) "Ocultar cálculo ▲" else "¿De dónde salen los números? ▼",
                     style = MaterialTheme.typography.labelSmall,
                     color = AppColors.textSecondary)
            }
            if (showMath) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScoreBreakdownColumn(round.playerCard, weather, round.playerMatchup,
                        round.playerCrit, round.playerMissed, round.playerScore,
                        Modifier.weight(1f))
                    ScoreBreakdownColumn(round.botCard, weather, round.botMatchup,
                        round.botCrit, missed = false, score = round.botScore,
                        Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CombatSide(
    card: Card, score: Float, crit: Boolean, matchup: Float,
    winner: Boolean, modifier: Modifier = Modifier, alignEnd: Boolean = false,
    missed: Boolean = false
) {
    val scale by animateFloatAsState(if (winner) 1.06f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "side")
    Column(modifier.scale(scale),
           horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!alignEnd) CardSprite(card, imageSize = 52.dp)
            Column(Modifier.padding(horizontal = 8.dp),
                   horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
                Text(card.name, style = MaterialTheme.typography.bodySmall, color = AppColors.textPrimary,
                     fontWeight = if (winner) FontWeight.ExtraBold else FontWeight.Normal, maxLines = 1)
                Text("%.3f pts".format(score), style = MaterialTheme.typography.labelSmall,
                     color = if (winner) AppColors.successColor else AppColors.textSecondary,
                     fontWeight = FontWeight.Bold)
            }
            if (alignEnd) CardSprite(card, imageSize = 52.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (missed) HintChip("¡FALLÓ EL ATAQUE!", AppColors.defeatColor)
            if (crit) HintChip("¡CRÍTICO! ×${SimulateBattleUseCase.CRIT_MULTIPLIER}", AppColors.critColor)
            if (!missed && matchup > 1f) HintChip("Tipo ×$matchup", AppColors.successColor)
            if (!missed && matchup < 1f) HintChip("Tipo ×$matchup", AppColors.defeatColor)
        }
    }
}

@Composable
private fun FinalBanner(result: BattleResult, finalLabel: String, onContinue: () -> Unit) {
    val color = when {
        result.playerWon -> AppColors.successColor
        result.isDraw    -> AppColors.drawColor
        else             -> AppColors.defeatColor
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.clip(RoundedCornerShape(16.dp)).background(color.copy(0.12f))
                .padding(horizontal = 24.dp, vertical = 12.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.battleOutcome(result.winner), contentDescription = null,
                         tint = color, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(when {
                        result.playerWon -> "¡Victoria ${result.playerRoundWins}-${result.botRoundWins}!"
                        result.isDraw    -> "Empate ${result.playerRoundWins}-${result.botRoundWins}"
                        else             -> "Derrota ${result.playerRoundWins}-${result.botRoundWins}"
                    }, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, color = color)
                }
                CoinText(if (result.coinsDelta >= 0) "+${result.coinsDelta}" else "${result.coinsDelta}",
                         color = color, style = MaterialTheme.typography.titleSmall)
            }
        }
        Button(onClick = onContinue, shape = RoundedCornerShape(12.dp),
               colors = ButtonDefaults.buttonColors(containerColor = color)) {
            Text(finalLabel, fontWeight = FontWeight.Bold)
        }
    }
}
