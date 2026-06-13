package com.pokemonarena.presentation.screens.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pokemonarena.domain.entity.*
import com.pokemonarena.presentation.theme.AppColors
import com.pokemonarena.presentation.utils.*
import com.pokemonarena.presentation.navigation.Navigator
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(pokemonName: String, viewModel: CardDetailViewModel, navigator: Navigator) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(pokemonName) { viewModel.onEvent(DetailUiEvent.Load(pokemonName)) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(pokemonName.replaceFirstChar { it.uppercase() }) },
            navigationIcon = {
                IconButton(onClick = { navigator.goBack() }) {
                    Icon(com.pokemonarena.presentation.theme.AppIcons.back, contentDescription = "Volver")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
        )
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is DetailUiState.Loading -> LoadingIndicator()
                is DetailUiState.Error   -> ErrorMessage(s.message,
                    onRetry = { viewModel.onEvent(DetailUiEvent.Load(pokemonName)) })
                is DetailUiState.Success -> DetailContent(
                    state      = s,
                    onSelect   = { viewModel.onEvent(DetailUiEvent.CardSelected(it)) },
                    onPurchase = { viewModel.onEvent(DetailUiEvent.PurchaseRequested) }
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    state:     DetailUiState.Success,
    onSelect:  (Card) -> Unit,
    onPurchase: () -> Unit
) {
    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {

        Column(
            Modifier.width(280.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KamelImage(asyncPainterResource(state.detail.imageUrl), state.detail.displayName,
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().height(220.dp),
                onLoading = { CircularProgressIndicator() },
                onFailure = { Text("?", style = MaterialTheme.typography.displayLarge) })

            Text(state.detail.displayName, style = MaterialTheme.typography.headlineMedium,
                 fontWeight = FontWeight.Bold)
            Text("#${state.detail.id.toString().padStart(3, '0')}",
                 color = MaterialTheme.colorScheme.secondary)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.detail.types.forEach { TypeBadge(it) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Altura", style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.secondary)
                    Text("%.1f m".format(state.detail.heightInMeters), fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Peso", style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.secondary)
                    Text("%.1f kg".format(state.detail.weightInKg), fontWeight = FontWeight.Bold)
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Stats base", fontWeight = FontWeight.Bold)
                    StatBar("HP",        state.detail.stats.hp)
                    StatBar("Ataque",    state.detail.stats.attack)
                    StatBar("Defensa",   state.detail.stats.defense)
                    StatBar("At. Esp.",  state.detail.stats.specialAttack)
                    StatBar("Def. Esp.", state.detail.stats.specialDefense)
                    StatBar("Velocidad", state.detail.stats.speed)
                    Divider()
                    Text("Total: ${state.detail.stats.total}", fontWeight = FontWeight.Bold)
                }
            }

            if (state.detail.evolutionChain.size > 1) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Evoluciones", fontWeight = FontWeight.Bold)
                        state.detail.evolutionChain.forEachIndexed { i, name ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (i > 0) Text("→ ", color = MaterialTheme.colorScheme.secondary)
                                Text(name.replaceFirstChar { it.uppercase() },
                                     fontWeight = if (name == state.detail.name) FontWeight.ExtraBold
                                                  else FontWeight.Normal,
                                     color = if (name == state.detail.name) MaterialTheme.colorScheme.primary
                                             else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            Text("Cartas disponibles (${state.cards.size})",
                 style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            state.message?.let { msg ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = if (msg.isSuccess) AppColors.successBackground
                                     else MaterialTheme.colorScheme.errorContainer)) {
                    Text(msg.text, Modifier.padding(10.dp),
                         color = if (msg.isSuccess) AppColors.successColor
                                 else MaterialTheme.colorScheme.onErrorContainer,
                         style = MaterialTheme.typography.bodySmall)
                }
            }

            state.selectedCard?.let { card ->
                val isOwned = card.id in state.ownedCardIds
                Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KamelImage(asyncPainterResource(card.imageUrlSmall), card.name,
                            contentScale = ContentScale.Fit, modifier = Modifier.size(72.dp),
                            onLoading = { CircularProgressIndicator(Modifier.size(20.dp)) },
                            onFailure = { CardImagePlaceholder() })
                        val price = CardPricing.priceOf(card)
                        Column(Modifier.weight(1f)) {
                            Text(card.name, fontWeight = FontWeight.Bold)
                            card.rarity?.let {
                                Text("$it (×${CardPricing.rarityMultiplier(it)})",
                                     style = MaterialTheme.typography.bodySmall,
                                     color = MaterialTheme.colorScheme.secondary)
                            }
                            Text(card.setName, style = MaterialTheme.typography.labelSmall,
                                 color = MaterialTheme.colorScheme.secondary)
                            val effective = card.effectiveStats.total
                            Text(
                                if (effective != card.stats.total)
                                    "BST en combate: $effective (base ${card.stats.total})"
                                else "BST en combate: $effective",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (effective > card.stats.total) AppColors.successColor
                                        else MaterialTheme.colorScheme.secondary)
                        }
                        if (isOwned) {
                            OutlinedButton(onClick = {}, enabled = false) { Text("Ya tenés esta carta") }
                        } else {
                            Button(onClick = onPurchase, enabled = state.coins >= price) {
                                Text(if (state.coins >= price) "Comprar por $price monedas"
                                     else "Te faltan ${price - state.coins} monedas",
                                     fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (state.cards.isNotEmpty()) {
                Text("Seleccioná una versión:",
                     style = MaterialTheme.typography.labelSmall,
                     color = MaterialTheme.colorScheme.secondary)
                LazyVerticalGrid(
                    columns               = GridCells.Adaptive(100.dp),
                    modifier              = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.cards, key = { it.id }) { card ->
                        val selected = state.selectedCard?.id == card.id
                        val owned    = card.id in state.ownedCardIds
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(card) },
                            shape    = RoundedCornerShape(10.dp),
                            border   = if (selected) CardDefaults.outlinedCardBorder() else null,
                            colors   = CardDefaults.cardColors(
                                containerColor = when {
                                    owned    -> Color(0xFFE8F5E9)
                                    selected -> MaterialTheme.colorScheme.primaryContainer
                                    else     -> MaterialTheme.colorScheme.surface
                                })
                        ) {
                            Column(Modifier.padding(6.dp),
                                   horizontalAlignment = Alignment.CenterHorizontally,
                                   verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                KamelImage(asyncPainterResource(card.imageUrlSmall), card.name,
                                    contentScale = ContentScale.Fit,
                                    modifier     = Modifier.fillMaxWidth().height(110.dp),
                                    onLoading    = { CircularProgressIndicator(Modifier.size(16.dp)) },
                                    onFailure    = { CardImagePlaceholder() })
                                if (owned) Text("Tuya", fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AppColors.successColor)
                                card.rarity?.let {
                                    Text(it, style = MaterialTheme.typography.labelSmall,
                                         maxLines = 1, color = MaterialTheme.colorScheme.secondary)
                                }
                                Text("BST ${card.effectiveStats.total}",
                                     style = MaterialTheme.typography.labelSmall,
                                     fontWeight = FontWeight.Bold,
                                     color = if (card.effectiveStats.total > card.stats.total)
                                                 AppColors.successColor
                                             else MaterialTheme.colorScheme.secondary)
                                CoinText("${CardPricing.priceOf(card)}",
                                         color = MaterialTheme.colorScheme.primary,
                                         style = MaterialTheme.typography.labelSmall, iconSize = 12.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBar(label: String, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, Modifier.width(72.dp), style = MaterialTheme.typography.bodySmall)
        Text(value.toString().padStart(3), Modifier.width(32.dp),
             style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(
            progress = (value / 255f).coerceIn(0f, 1f),
            modifier = Modifier.weight(1f).height(8.dp),
            color = when {
                value >= 150 -> Color(0xFF4CAF50); value >= 100 -> Color(0xFF8BC34A)
                value >= 70  -> Color(0xFFFFEB3B); value >= 50  -> Color(0xFFFF9800)
                else         -> Color(0xFFF44336)
            }
        )
    }
}
