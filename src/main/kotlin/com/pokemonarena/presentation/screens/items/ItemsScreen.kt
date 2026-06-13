package com.pokemonarena.presentation.screens.items

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pokemonarena.domain.entity.Item
import com.pokemonarena.domain.entity.ItemCatalog
import com.pokemonarena.presentation.theme.AppColors
import com.pokemonarena.presentation.theme.AppIcons
import com.pokemonarena.presentation.utils.CardImagePlaceholder
import com.pokemonarena.presentation.utils.CoinText
import com.pokemonarena.presentation.utils.LoadingIndicator
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(viewModel: ItemsViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text("Tienda de Items") },
            actions = {
                Row(Modifier.padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.coin, contentDescription = "Monedas", tint = AppColors.coinColor)
                    Spacer(Modifier.width(4.dp))
                    Text("${state.coins}", fontWeight = FontWeight.ExtraBold,
                         color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary))
    }) { padding ->
        if (state.isLoading) { LoadingIndicator(); return@Scaffold }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Items de combate: se equipan a una carta en Mi Equipo y " +
                     "mejoran sus stats en batalla.",
                     style = MaterialTheme.typography.bodySmall, color = AppColors.textSecondary)
            }
            state.message?.let { msg ->
                item {
                    Card(colors = CardDefaults.cardColors(
                            containerColor = if (msg.isSuccess) AppColors.successBackground
                                             else MaterialTheme.colorScheme.errorContainer)) {
                        Text(msg.text, Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall,
                             color = if (msg.isSuccess) AppColors.successColor
                                     else MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            items(state.catalog.filterNot { ItemCatalog.isExclusive(it.id) }, key = { it.id }) { item ->
                ItemRow(item,
                        owned   = state.inventory[item.id] ?: 0,
                        canBuy  = state.coins >= item.price,
                        onBuy   = { viewModel.onEvent(ItemsUiEvent.Purchase(item)) })
            }
        }
    }
}

@Composable
private fun ItemRow(item: Item, owned: Int, canBuy: Boolean, onBuy: () -> Unit) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(3.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KamelImage(asyncPainterResource(item.imageUrl), item.name,
                contentScale = ContentScale.Fit, modifier = Modifier.size(48.dp),
                onLoading = { CircularProgressIndicator(Modifier.size(18.dp)) },
                onFailure = { CardImagePlaceholder(28.dp) })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, fontWeight = FontWeight.Bold, color = AppColors.textPrimary)
                    if (owned > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text("x$owned en inventario", style = MaterialTheme.typography.labelSmall,
                             color = AppColors.successColor, fontWeight = FontWeight.Bold)
                    }
                }
                Text(item.description, style = MaterialTheme.typography.bodySmall,
                     color = AppColors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    boostLabels(item).forEach { BoostChip(it) }
                }
            }
            Button(onClick = onBuy, enabled = canBuy, shape = RoundedCornerShape(10.dp)) {
                CoinText("${item.price}", color = MaterialTheme.colorScheme.onPrimary,
                         style = MaterialTheme.typography.labelMedium, iconSize = 14.dp)
            }
        }
    }
}

@Composable
private fun BoostChip(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = AppColors.successColor.copy(alpha = 0.12f)) {
        Text(text, Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
             style = MaterialTheme.typography.labelSmall,
             color = AppColors.successColor, fontWeight = FontWeight.SemiBold)
    }
}

private fun boostLabels(item: Item): List<String> = buildList {
    val b = item.boosts
    if (b.attack > 1f)            add("Ataque ×${b.attack}")
    if (b.defense > 1f)           add("Defensa ×${b.defense}")
    if (b.specialAttack > 1f)     add("At. Esp. ×${b.specialAttack}")
    if (b.specialDefense > 1f)    add("Def. Esp. ×${b.specialDefense}")
    if (b.speed > 1f)             add("Velocidad ×${b.speed}")
    if (item.missReduction > 0f)  add("Precisión +${(item.missReduction * 100).toInt()}%")
    if (ItemCatalog.isConsumable(item.id)) add("Consumible")
}
