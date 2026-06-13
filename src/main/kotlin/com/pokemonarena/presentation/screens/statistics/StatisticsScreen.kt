package com.pokemonarena.presentation.screens.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokemonarena.domain.entity.*
import com.pokemonarena.presentation.theme.AppColors
import com.pokemonarena.presentation.theme.AppIcons
import com.pokemonarena.presentation.utils.LoadingIndicator
import com.pokemonarena.presentation.utils.WeatherLabel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val stats   = state.stats
    val history = state.history

    Scaffold(topBar = {
        TopAppBar(title = { Text("Estadísticas") },
            actions = {
                IconButton(onClick = { viewModel.onEvent(StatisticsUiEvent.Refresh) }) {
                    Icon(AppIcons.refresh, contentDescription = "Actualizar")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary))
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> LoadingIndicator()
                stats.totalBattles == 0 -> EmptyStats()
                else -> StatsContent(stats, history)
            }
        }
    }
}

@Composable
private fun EmptyStats() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(AppIcons.trophy, contentDescription = null,
                 tint = AppColors.dividerColor, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("¡Todavía no jugaste ninguna batalla!", style = MaterialTheme.typography.bodyLarge)
            Text("Desafiá un gimnasio para empezar.", style = MaterialTheme.typography.bodySmall,
                 color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun StatsContent(stats: UserStatistics, history: List<BattleResult>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("Total",    stats.totalBattles.toString(), Modifier.weight(1f))
                StatChip("Ganadas",  stats.totalWins.toString(),    Modifier.weight(1f), AppColors.successBackground)
                StatChip("Perdidas", stats.totalLosses.toString(),  Modifier.weight(1f), MaterialTheme.colorScheme.errorContainer)
                StatChip("Empates",  stats.totalDraws.toString(),   Modifier.weight(1f))
            }
        }
        
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row {
                        Text("% de victorias", Modifier.weight(1f))
                        Text("${(stats.winRate * 100).roundToInt()}%", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = stats.winRate, modifier = Modifier.fillMaxWidth().height(12.dp),
                        color = if (stats.winRate >= 0.5f) AppColors.successColor else MaterialTheme.colorScheme.error)
                }
            }
        }
        
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Racha actual", style = MaterialTheme.typography.labelSmall)
                        Text(stats.currentStreak.toString(), fontSize = 36.sp, fontWeight = FontWeight.Bold,
                             color = if (stats.currentStreak >= 3) AppColors.accentColor
                                     else MaterialTheme.colorScheme.onSurface)
                    }
                    VerticalDivider(Modifier.height(60.dp), color = AppColors.dividerColor)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mejor racha", style = MaterialTheme.typography.labelSmall)
                        Text(stats.bestStreak.toString(), fontSize = 36.sp, fontWeight = FontWeight.Bold,
                             color = AppColors.coinColor)
                    }
                }
            }
        }
        
        stats.favoritePokemon?.let { fav ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.star, contentDescription = null,
                             tint = AppColors.coinColor, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Pokémon más usado", style = MaterialTheme.typography.labelSmall,
                                 color = MaterialTheme.colorScheme.secondary)
                            Text(fav.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        if (history.isNotEmpty()) {
            item { Text("Batallas recientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(history, key = { it.date + it.playerCard.name }) { battle ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.battleOutcome(battle.winner), contentDescription = null,
                             modifier = Modifier.size(24.dp),
                             tint = when {
                                 battle.playerWon -> AppColors.successColor
                                 battle.isDraw    -> AppColors.drawColor
                                 else             -> AppColors.defeatColor
                             })
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(battle.gymName.ifBlank { "Batalla" }, style = MaterialTheme.typography.bodySmall,
                                 fontWeight = FontWeight.SemiBold)
                            WeatherLabel(battle.weatherCondition, color = AppColors.textSecondary,
                                         style = MaterialTheme.typography.labelSmall,
                                         fontWeight = FontWeight.Normal, iconSize = 12.dp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (battle.playerWon) "Victoria" else if (battle.isDraw) "Empate" else "Derrota",
                                 style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                 color = if (battle.playerWon) AppColors.successColor else MaterialTheme.colorScheme.error)
                            Text("%.3f".format(battle.playerScore), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier,
                     color: Color = MaterialTheme.colorScheme.surfaceVariant) {
    Card(colors = CardDefaults.cardColors(containerColor = color), modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
