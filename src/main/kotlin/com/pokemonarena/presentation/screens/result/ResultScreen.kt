package com.pokemonarena.presentation.screens.result

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokemonarena.domain.entity.BattleRewards
import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.RoundResult
import com.pokemonarena.domain.entity.WeatherCondition
import com.pokemonarena.domain.entity.Winner
import com.pokemonarena.domain.usecase.SimulateBattleUseCase
import com.pokemonarena.presentation.navigation.Navigator
import com.pokemonarena.presentation.navigation.Screen
import com.pokemonarena.presentation.screens.battle.BattleViewModel
import com.pokemonarena.presentation.theme.AppColors
import com.pokemonarena.presentation.theme.AppIcons
import com.pokemonarena.domain.entity.ScoreBreakdown
import com.pokemonarena.presentation.utils.CardSprite
import com.pokemonarena.presentation.utils.CoinText
import com.pokemonarena.presentation.utils.WeatherLabel
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(battleVM: BattleViewModel, navigator: Navigator) {
    val result = battleVM.lastResult.collectAsState().value ?: run {
        Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Sin resultado.") }
        return
    }
    val firstWinReward by battleVM.firstWinReward.collectAsState()
    val gymRegion      by battleVM.lastRegion.collectAsState()

    var titleVisible  by remember { mutableStateOf(false) }
    var scoresVisible by remember { mutableStateOf(false) }
    var cardsVisible  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100); titleVisible  = true
        delay(400); scoresVisible = true
        delay(300); cardsVisible  = true
    }

    val playerCards = result.playerCards.ifEmpty { listOf(result.playerCard) }
    val botCards    = result.botCards.ifEmpty    { listOf(result.botCard)    }

    val winColor = when {
        result.playerWon -> AppColors.successColor
        result.isDraw    -> AppColors.drawColor
        else             -> AppColors.defeatColor
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Resultado") }, colors = TopAppBarDefaults.topAppBarColors(
            containerColor = winColor, titleContentColor = AppColors.textIconsColor))
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            AnimatedVisibility(titleVisible, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) {
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(winColor, winColor.copy(alpha = 0.7f))))
                        .padding(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(AppIcons.battleOutcome(result.winner), contentDescription = null,
                             tint = AppColors.textIconsColor, modifier = Modifier.size(56.dp))
                        Text(when { result.playerWon -> "¡Ganaste!"; result.isDraw -> "¡Empate!"; else -> "¡Perdiste!" },
                             fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.textIconsColor)
                        if (result.rounds.isNotEmpty())
                            Text("Rondas: ${result.playerRoundWins} - ${result.botRoundWins}",
                                 color = AppColors.textIconsColor, fontWeight = FontWeight.Bold)
                        if (result.gymName.isNotBlank())
                            Text("en ${result.gymName}", color = AppColors.textIconsColor.copy(alpha = 0.85f))
                    }
                }
            }

            firstWinReward?.let { reward ->
                AnimatedVisibility(titleVisible, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                            containerColor = AppColors.goldColor.copy(alpha = 0.18f))) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.trophy, contentDescription = "Medalla",
                                 tint = AppColors.goldColor, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("¡Primera victoria en ${result.gymName}!",
                                     fontWeight = FontWeight.ExtraBold, color = AppColors.textPrimary)
                                CoinText("Medalla conseguida · +${reward.coins} monedas de bono",
                                         color = AppColors.coinColor,
                                         style = MaterialTheme.typography.bodySmall)
                                Text("Item exclusivo: ${reward.item.name} — ${reward.item.description}",
                                     style = MaterialTheme.typography.labelSmall,
                                     fontWeight = FontWeight.Bold, color = AppColors.successColor)
                            }
                            KamelImage(asyncPainterResource(reward.item.imageUrl), reward.item.name,
                                contentScale = ContentScale.Fit, modifier = Modifier.size(40.dp),
                                onLoading = {}, onFailure = {})
                        }
                    }
                }
            }

            AnimatedVisibility(scoresVisible, enter = slideInVertically { it / 2 } + fadeIn()) {
                Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(6.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Puntaje total", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        val maxScore = maxOf(result.playerScore, result.botScore).coerceAtLeast(0.001f)
                        ResultScoreBar("Vos", result.playerScore, maxScore,
                            if (result.playerWon) AppColors.successColor else AppColors.defeatColor)
                        ResultScoreBar("Bot", result.botScore, maxScore,
                            if (!result.playerWon && !result.isDraw) AppColors.successColor else AppColors.infoColor)
                        HorizontalDivider(color = AppColors.dividerColor)
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            WeatherLabel(result.weatherCondition, color = AppColors.textSecondary,
                                         style = MaterialTheme.typography.bodySmall,
                                         fontWeight = FontWeight.Normal, iconSize = 15.dp)
                        }

                        val (coinText, coinColor) = when {
                            result.coinsDelta > 0 -> "+${result.coinsDelta} monedas ganadas" to AppColors.successColor
                            result.coinsDelta < 0 -> "${result.coinsDelta} monedas perdidas" to AppColors.defeatColor
                            else                  -> "Sin cambio de monedas"                 to AppColors.drawColor
                        }
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CoinText(coinText, color = coinColor, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            AnimatedVisibility(cardsVisible, enter = fadeIn(tween(500))) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enfrentamientos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    if (result.rounds.isNotEmpty()) {
                        result.rounds.forEachIndexed { i, round ->
                            ResultRoundCard(i + 1, round, result.weatherCondition)
                        }
                    } else {
                        playerCards.zip(botCards).forEachIndexed { i, (p, b) ->
                            ResultMatchupCard(i + 1, p, b)
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { navigator.navigateTo(Screen.Gyms(gymRegion)) },
                       colors = ButtonDefaults.buttonColors(containerColor = winColor),
                       modifier = Modifier.weight(1f).height(50.dp)) {
                    Text("Volver a Gimnasios", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { navigator.navigateTo(Screen.Home) },
                               modifier = Modifier.weight(1f).height(50.dp)) {
                    Text("Inicio")
                }
            }
        }
    }
}

@Composable
private fun ResultScoreBar(label: String, score: Float, maxScore: Float, color: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / maxScore,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "score_$label"
    )
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text("%.4f pts".format(score), style = MaterialTheme.typography.bodySmall, color = color)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))
                .background(color.copy(alpha = 0.15f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(7.dp)).background(color))
        }
    }
}

@Composable
private fun ResultRoundCard(index: Int, round: RoundResult, weather: WeatherCondition) {
    val roundColor = when (round.winner) {
        Winner.PLAYER -> AppColors.successColor
        Winner.BOT    -> AppColors.defeatColor
        Winner.DRAW   -> AppColors.drawColor
    }
    val barColor = when (round.winner) {
        Winner.PLAYER -> AppColors.successBackground
        Winner.BOT    -> AppColors.defeatBackground
        Winner.DRAW   -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
         colors = CardDefaults.cardColors(containerColor = barColor)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ronda $index", fontWeight = FontWeight.ExtraBold,
                     style = MaterialTheme.typography.labelMedium, color = roundColor)
                Spacer(Modifier.weight(1f))
                Text(when (round.winner) {
                    Winner.PLAYER -> "Ganaste la ronda"
                    Winner.BOT    -> "La ganó el bot"
                    Winner.DRAW   -> "Ronda empatada"
                }, style = MaterialTheme.typography.labelSmall, color = roundColor, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ResultRoundSide(round.playerCard, round.playerScore, round.playerCrit,
                                winner = round.winner == Winner.PLAYER, modifier = Modifier.weight(2f),
                                missed = round.playerMissed,
                                weather = weather, matchup = round.playerMatchup)
                Icon(AppIcons.battle, contentDescription = null, tint = AppColors.textSecondary,
                     modifier = Modifier.weight(0.6f).size(24.dp))
                ResultRoundSide(round.botCard, round.botScore, round.botCrit,
                                winner = round.winner == Winner.BOT, modifier = Modifier.weight(2f),
                                weather = weather, matchup = round.botMatchup)
            }
        }
    }
}

@Composable
private fun ResultRoundSide(card: Card, score: Float, crit: Boolean, winner: Boolean, modifier: Modifier,
                            missed: Boolean = false, weather: WeatherCondition, matchup: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        CardSprite(card, imageSize = 72.dp)
        Text(card.name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
             maxLines = 2, fontWeight = if (winner) FontWeight.ExtraBold else FontWeight.Medium)
        Text("%.3f pts".format(score), style = MaterialTheme.typography.labelSmall,
             color = if (winner) AppColors.successColor else AppColors.textSecondary,
             fontWeight = FontWeight.Bold)
        keyFactors(card, weather, matchup, crit, missed).forEach { (text, color) ->
            Text(text, style = MaterialTheme.typography.labelSmall,
                 color = color, fontWeight = FontWeight.Bold)
        }
    }
}

private fun keyFactors(card: Card, weather: WeatherCondition, matchup: Float,
                       crit: Boolean, missed: Boolean): List<Pair<String, Color>> {
    if (missed) return listOf("Falló el ataque" to AppColors.defeatColor)
    val b = ScoreBreakdown.of(card, weather, matchup, crit, missed = false)
    return buildList {
        card.heldItem?.let { if (b.itemMultiplier != 1f) add("${it.name} ×%.2f".format(b.itemMultiplier) to AppColors.successColor) }
        if (b.fatigueMultiplier < 1f)
            add("Fatiga ×%.2f".format(b.fatigueMultiplier) to AppColors.defeatColor)
        if (b.weatherMultiplier != 1f)
            add("Clima ×${b.weatherMultiplier}" to
                if (b.weatherMultiplier > 1f) AppColors.infoColor else AppColors.defeatColor)
        if (matchup != 1f)
            add("Tipo ×$matchup" to if (matchup > 1f) AppColors.successColor else AppColors.defeatColor)
        if (crit) add("Crítico ×${SimulateBattleUseCase.CRIT_MULTIPLIER}" to AppColors.critColor)
    }
}

@Composable
private fun ResultMatchupCard(index: Int, playerCard: Card, botCard: Card) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(2f)) {
                CardSprite(playerCard, imageSize = 72.dp)
                Text(playerCard.name, style = MaterialTheme.typography.labelSmall,
                     textAlign = TextAlign.Center, maxLines = 2, fontWeight = FontWeight.Medium)
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)) {
                    Text("$index", color = AppColors.textIconsColor, fontWeight = FontWeight.ExtraBold,
                         modifier = Modifier.align(Alignment.Center))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(2f)) {
                CardSprite(botCard, imageSize = 72.dp)
                Text(botCard.name, style = MaterialTheme.typography.labelSmall,
                     textAlign = TextAlign.Center, maxLines = 2, fontWeight = FontWeight.Medium)
            }
        }
    }
}
