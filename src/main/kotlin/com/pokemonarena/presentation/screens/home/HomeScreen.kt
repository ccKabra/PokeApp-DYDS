package com.pokemonarena.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import com.pokemonarena.domain.entity.BattleResult
import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.Region
import com.pokemonarena.presentation.navigation.Navigator
import com.pokemonarena.presentation.navigation.Screen
import com.pokemonarena.presentation.theme.AppColors
import com.pokemonarena.presentation.theme.AppIcons
import com.pokemonarena.presentation.utils.CardSprite
import com.pokemonarena.presentation.utils.CoinText
import com.pokemonarena.presentation.utils.ErrorMessage
import com.pokemonarena.presentation.utils.LoadingIndicator
import com.pokemonarena.presentation.utils.TypeBadge
import com.pokemonarena.presentation.utils.WeatherLabel
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, navigator: Navigator) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text("Inicio") }, colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary))
    }) { padding ->
        when {
            state.isLoading -> LoadingIndicator()
            state.error != null -> ErrorMessage(state.error!!,
                onRetry = { viewModel.onEvent(HomeUiEvent.Refresh) })
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { StatsRow(state) }
                item { BadgesSection(state, navigator) }
                item { TeamSection(state.teamCards, navigator) }
                item { RecentBattlesSection(state.recentBattles) }
                item { QuickActionsRow(navigator) }
            }
        }
    }
}

@Composable
private fun BadgesSection(state: HomeUiState, navigator: Navigator) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.trophy, contentDescription = null,
                     tint = AppColors.goldColor, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Medallas (${state.earnedCount}/${state.totalBadges})",
                     fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium,
                     modifier = Modifier.weight(1f))
            }
            Region.entries.forEach { region ->
                val regionLocked = region !in state.unlockedRegions
                val regionGyms   = state.gyms.filter { it.region == region }
                val league       = state.leagues.firstOrNull { it.region == region }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(region.displayName, fontWeight = FontWeight.ExtraBold,
                         style = MaterialTheme.typography.labelMedium,
                         color = if (regionLocked) AppColors.textSecondary.copy(0.6f)
                                 else AppColors.textPrimary)
                    if (regionLocked) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.Lock, contentDescription = "Región bloqueada",
                             tint = AppColors.textSecondary.copy(0.6f), modifier = Modifier.size(13.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    if (!regionLocked)
                        TextButton(onClick = { navigator.navigateTo(Screen.Gyms(region)) },
                                   modifier = Modifier.height(26.dp)) {
                            Text("Ir →", style = MaterialTheme.typography.labelSmall)
                        }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    regionGyms.forEach { gym ->
                        BadgeChip(gym.name.removePrefix("Gimnasio "), gym.badgeImageUrl,
                                  earned = gym.name in state.earnedBadges,
                                  locked = regionLocked, modifier = Modifier.weight(1f))
                    }
                    league?.let {
                        BadgeChip("Liga", null, earned = it.name in state.earnedBadges,
                                  locked = regionLocked, isLeague = true, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(label: String, badgeImageUrl: String?, earned: Boolean,
                      locked: Boolean, modifier: Modifier, isLeague: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                .background(when {
                    earned -> AppColors.goldColor.copy(0.15f)
                    locked -> AppColors.dividerColor.copy(0.15f)
                    else   -> AppColors.dividerColor.copy(0.3f)
                }),
            contentAlignment = Alignment.Center) {
            when {
                earned && badgeImageUrl != null ->
                    KamelImage(asyncPainterResource(badgeImageUrl), "Medalla de $label",
                        contentScale = ContentScale.Fit, modifier = Modifier.size(32.dp),
                        onLoading = {}, onFailure = {
                            Icon(AppIcons.trophy, contentDescription = null,
                                 tint = AppColors.goldColor, modifier = Modifier.size(22.dp))
                        })
                earned -> Icon(if (isLeague) AppIcons.trophy else AppIcons.star,
                               contentDescription = "Medalla de $label",
                               tint = AppColors.goldColor, modifier = Modifier.size(22.dp))
                locked -> Icon(Icons.Filled.Lock, contentDescription = "Bloqueada",
                               tint = AppColors.textSecondary.copy(0.35f), modifier = Modifier.size(16.dp))
                else -> Text("?", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                             color = AppColors.textSecondary.copy(0.5f))
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, fontSize = 9.sp,
             textAlign = TextAlign.Center,
             color = if (earned) AppColors.textPrimary else AppColors.textSecondary.copy(0.7f))
    }
}

@Composable
private fun StatsRow(state: HomeUiState) {
    val stats = state.stats
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(AppIcons.battle, "Batallas", stats.totalBattles.toString(), Modifier.weight(1f))
        StatCard(AppIcons.trophy, "Victorias", stats.totalWins.toString(), Modifier.weight(1f), AppColors.successColor)
        StatCard(AppIcons.rate, "Win rate", "${(stats.winRate * 100).roundToInt()}%", Modifier.weight(1f),
                 if (stats.winRate >= 0.5f) AppColors.successColor else MaterialTheme.colorScheme.error)
        StatCard(AppIcons.streak, "Racha", stats.currentStreak.toString(), Modifier.weight(1f),
                 if (stats.currentStreak >= 3) AppColors.accentColor else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, modifier: Modifier,
                     valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = label, tint = valueColor, modifier = Modifier.size(26.dp))
            Text(value, fontWeight = FontWeight.ExtraBold,
                 style = MaterialTheme.typography.headlineSmall, color = valueColor)
            Text(label, style = MaterialTheme.typography.labelSmall,
                 color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun TeamSection(teamCards: List<Card>, navigator: Navigator) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Tu equipo", fontWeight = FontWeight.Bold,
                     style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (teamCards.size < 3) TextButton(onClick = { navigator.navigateTo(Screen.MyTeam) }) {
                    Text("Completar equipo →")
                }
            }
            if (teamCards.isEmpty()) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer).padding(16.dp)) {
                    Text("No tenés cartas en tu equipo. Comprá cartas desde el Pokédex.",
                         color = MaterialTheme.colorScheme.onErrorContainer,
                         style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) { slot ->
                        val card = teamCards.getOrNull(slot)
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
                            contentAlignment = Alignment.Center) {
                            if (card != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally,
                                       verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    CardSprite(card, imageSize = 80.dp)
                                    Text(card.name, style = MaterialTheme.typography.labelSmall,
                                         fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    TypeBadge(card.primaryType)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(AppIcons.cards, contentDescription = null,
                                         tint = AppColors.dividerColor, modifier = Modifier.size(32.dp))
                                    Text("Slot ${slot + 1}", style = MaterialTheme.typography.labelSmall,
                                         color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentBattlesSection(battles: List<BattleResult>) {
    if (battles.isEmpty()) return
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Últimas batallas", fontWeight = FontWeight.Bold,
                 style = MaterialTheme.typography.titleMedium)
            battles.forEach { battle ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.battleOutcome(battle.winner), contentDescription = null,
                         modifier = Modifier.size(22.dp),
                         tint = when {
                             battle.playerWon -> AppColors.successColor
                             battle.isDraw    -> AppColors.drawColor
                             else             -> AppColors.defeatColor
                         })
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(battle.gymName.ifBlank { "Batalla" },
                             style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        WeatherLabel(battle.weatherCondition, color = AppColors.textSecondary,
                                     style = MaterialTheme.typography.labelSmall,
                                     fontWeight = FontWeight.Normal, iconSize = 12.dp)
                    }
                    val (coinText, coinColor) = when {
                        battle.coinsDelta > 0 -> "+${battle.coinsDelta}" to AppColors.successColor
                        battle.coinsDelta < 0 -> "${battle.coinsDelta}" to MaterialTheme.colorScheme.error
                        else                  -> "±0" to AppColors.drawColor
                    }
                    CoinText(coinText, color = coinColor)
                }
            }
        }
    }
}

@Composable
private fun QuickActionsRow(navigator: Navigator) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { navigator.navigateTo(Screen.Collection) }, modifier = Modifier.weight(1f)) {
            Text("Ir al Pokédex")
        }
        OutlinedButton(onClick = { navigator.navigateTo(Screen.Gyms(Region.KANTO)) }, modifier = Modifier.weight(1f)) {
            Text("Ver Gimnasios")
        }
    }
}
