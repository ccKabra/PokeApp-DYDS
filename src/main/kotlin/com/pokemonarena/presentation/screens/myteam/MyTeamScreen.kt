package com.pokemonarena.presentation.screens.myteam

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokemonarena.domain.entity.BattleFatigue
import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.CardPricing
import com.pokemonarena.domain.entity.Item
import com.pokemonarena.presentation.navigation.Navigator
import com.pokemonarena.presentation.navigation.Screen
import com.pokemonarena.presentation.theme.AppColors
import com.pokemonarena.presentation.theme.AppIcons
import com.pokemonarena.presentation.utils.*
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTeamScreen(viewModel: MyTeamViewModel, navigator: Navigator) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text("Mi Equipo") }, colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary))
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val s = state
            if (s.isLoading) LoadingIndicator()
            else Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {

                Column(Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.coin, contentDescription = "Monedas",
                                 tint = AppColors.coinColor, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Monedas", style = MaterialTheme.typography.labelSmall)
                                Text("${s.stats.coins}", fontWeight = FontWeight.ExtraBold,
                                     style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }

                    SectionTitle("Equipo de batalla (${s.teamCards.size}/3)")

                    if (!s.canBattle) {
                        Card(colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(
                                if (s.ownedCards.isEmpty()) "Comprá cartas desde el Pokédex para armar tu equipo."
                                else "Seleccioná ${3 - s.teamCards.size} carta${if (3 - s.teamCards.size > 1) "s" else ""} más para completar el equipo.",
                                Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    repeat(3) { slot ->
                        val card = s.teamCards.getOrNull(slot)
                        TeamSlot(slot + 1, card, onRemove = {
                            card?.let { viewModel.onEvent(MyTeamUiEvent.ToggleCard(it.id)) }
                        })
                    }

                    if (s.canBattle) {
                        Card(colors = CardDefaults.cardColors(containerColor = AppColors.successColor)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null,
                                     tint = AppColors.textIconsColor, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Equipo listo — ve a Gimnasios para batallar",
                                     color = AppColors.textIconsColor, style = MaterialTheme.typography.bodySmall,
                                     fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Column(Modifier.weight(1f)) {
                    SectionTitle("Mis cartas (${s.ownedCards.size}/${s.maxCards})")
                    Spacer(Modifier.height(4.dp))

                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(s.maxCards / 2) { rowIndex ->
                            Row(Modifier.weight(1f).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                repeat(2) { col ->
                                    val card = s.ownedCards.getOrNull(rowIndex * 2 + col)
                                    if (card != null) {
                                        val inTeam  = s.teamCards.any { it.id == card.id }
                                        val blocked = !inTeam && s.teamIsFull
                                        OwnedCardSlot(card, inTeam, blocked,
                                            availableItems = s.availableItems,
                                            fatigueCures   = s.fatigueCures,
                                            modifier  = Modifier.weight(1f).fillMaxHeight(),
                                            onToggle  = { viewModel.onEvent(MyTeamUiEvent.ToggleCard(card.id)) },
                                            onSell    = { viewModel.onEvent(MyTeamUiEvent.SellCard(card.id)) },
                                            onEquip   = { itemId -> viewModel.onEvent(MyTeamUiEvent.EquipItem(card.id, itemId)) },
                                            onUnequip = { viewModel.onEvent(MyTeamUiEvent.UnequipItem(card.id)) },
                                            onCure    = { viewModel.onEvent(MyTeamUiEvent.CureFatigue(card.id)) })
                                    } else {
                                        EmptyCardSlot(Modifier.weight(1f).fillMaxHeight()) {
                                            navigator.navigateTo(Screen.Collection)
                                        }
                                    }
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
private fun EmptyCardSlot(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(AppColors.dividerColor.copy(0.25f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "Comprar carta",
                 tint = AppColors.textSecondary.copy(0.6f), modifier = Modifier.size(30.dp))
            Text("Slot libre", style = MaterialTheme.typography.labelSmall,
                 color = AppColors.textSecondary.copy(0.7f))
        }
    }
}

@Composable
private fun TeamSlot(number: Int, card: Card?, onRemove: () -> Unit) {
    Card(
        border = BorderStroke(2.dp, if (card != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(0.4f)),
        colors = CardDefaults.cardColors(containerColor = if (card != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().height(72.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$number.", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
            if (card != null) {
                CardSprite(card, imageSize = 50.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(card.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("BST ${card.effectiveStats.total}", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Quitar del equipo",
                         modifier = Modifier.size(14.dp))
                }
            } else {
                Text("Slot vacío", style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OwnedCardSlot(card: Card, inTeam: Boolean, blocked: Boolean,
                          availableItems: List<Pair<Item, Int>>,
                          fatigueCures: Int,
                          modifier: Modifier,
                          onToggle: () -> Unit, onSell: () -> Unit,
                          onEquip: (String) -> Unit, onUnequip: () -> Unit,
                          onCure: () -> Unit) {
    Card(
        border   = if (inTeam) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(
            containerColor = when {
                inTeam  -> MaterialTheme.colorScheme.primaryContainer
                blocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else    -> MaterialTheme.colorScheme.surface
            }),
        modifier = modifier.clickable(enabled = !blocked || inTeam, onClick = onToggle)
    ) {
        Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            KamelImage(asyncPainterResource(card.imageUrlSmall), card.name,
                contentScale = ContentScale.Fit, modifier = Modifier.size(64.dp),
                onLoading = { CircularProgressIndicator(Modifier.size(20.dp)) },
                onFailure = { CardImagePlaceholder(24.dp) })
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (inTeam) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                         contentDescription = if (inTeam) "En el equipo" else "Fuera del equipo",
                         tint = if (inTeam) MaterialTheme.colorScheme.primary else AppColors.dividerColor,
                         modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(card.name, fontWeight = FontWeight.SemiBold, maxLines = 1,
                         modifier = Modifier.weight(1f))
                    if (blocked) Text("Equipo lleno", style = MaterialTheme.typography.labelSmall,
                                       color = MaterialTheme.colorScheme.secondary)
                }
                Text(
                    "BST: ${card.effectiveStats.total} · ${card.primaryType.replaceFirstChar { it.uppercase() }}" +
                        (card.rarity?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall, maxLines = 1,
                    color = when {
                        card.effectiveStats.total > card.stats.total -> AppColors.successColor
                        card.effectiveStats.total < card.stats.total -> AppColors.defeatColor
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                if (card.timesUsed > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Fatiga: ${card.timesUsed} · -${BattleFatigue.penaltyPercent(card.timesUsed)}%",
                             style = MaterialTheme.typography.labelSmall,
                             color = if (BattleFatigue.penaltyPercent(card.timesUsed) >= 20) AppColors.defeatColor
                                     else AppColors.coinColor)
                        if (fatigueCures > 0) {
                            TextButton(onClick = onCure, modifier = Modifier.height(22.dp),
                                       contentPadding = PaddingValues(horizontal = 6.dp)) {
                                Text("Curar (x$fatigueCures)",
                                     style = MaterialTheme.typography.labelSmall,
                                     fontWeight = FontWeight.Bold, color = AppColors.successColor)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { HeldItemControls(card, availableItems, onEquip, onUnequip) }
                    OutlinedButton(
                        onClick  = onSell,
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        CoinText("Vender ${CardPricing.sellValueOf(card)}",
                                 color = AppColors.coinColor,
                                 style = MaterialTheme.typography.labelSmall, iconSize = 12.dp)
                    }
                }
            }
        }
    }
}

