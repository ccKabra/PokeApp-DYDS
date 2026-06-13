package com.pokemonarena.presentation.screens.collection

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.pokemonarena.domain.entity.Pokemon
import com.pokemonarena.presentation.utils.CardImagePlaceholder
import com.pokemonarena.presentation.utils.ErrorMessage
import com.pokemonarena.presentation.utils.LoadingIndicator
import com.pokemonarena.presentation.utils.TypeBadge
import com.pokemonarena.presentation.navigation.Navigator
import com.pokemonarena.presentation.navigation.Screen
import com.pokemonarena.presentation.theme.AppColors
import com.pokemonarena.presentation.theme.typeColors
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(viewModel: CollectionViewModel, navigator: Navigator) {
    val state by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Pokédex") }, colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary))

        OutlinedTextField(
            value         = state.query,
            onValueChange = { viewModel.onEvent(CollectionUiEvent.QueryChanged(it)) },
            placeholder   = { Text("Buscar por nombre (ej: bul, char, pika…)") },
            trailingIcon  = {
                if (state.query.isNotBlank())
                    IconButton(onClick = { viewModel.onEvent(CollectionUiEvent.ClearQuery) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Limpiar búsqueda")
                    }
            },
            singleLine = true,
            modifier   = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        when {
            state.isLoading -> LoadingIndicator()
            state.error != null -> ErrorMessage(state.error!!, onRetry = { viewModel.onEvent(CollectionUiEvent.Retry) })
            state.filteredPokemons.isEmpty() && state.query.isNotBlank() -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No se encontraron Pokémon con \"${state.query}\"")
                }
            }
            else -> {
                Text("${state.filteredPokemons.size} Pokémon${if (state.query.isNotBlank()) " encontrados" else " desbloqueados"} · Regiones: ${state.unlockedRegions.joinToString { it.displayName }}",
                     style = MaterialTheme.typography.labelMedium,
                     color = MaterialTheme.colorScheme.secondary,
                     modifier = Modifier.padding(horizontal = 16.dp))
                state.nextRegionHint?.let { hint ->
                    Text(hint, style = MaterialTheme.typography.labelSmall,
                         color = AppColors.coinColor,
                         modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(130.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(state.filteredPokemons, key = { _, p -> p.id }) { index, pokemon ->
                        PokedexCard(pokemon, index) { navigator.navigateTo(Screen.CardDetail(pokemon.name)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PokedexCard(pokemon: Pokemon, index: Int, onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 30L); visible = true }
    val scale by animateFloatAsState(if (visible) 1f else 0.8f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy), label = "s")
    val typeColor = typeColors[pokemon.primaryType] ?: Color.Gray

    Card(modifier = Modifier.fillMaxWidth().scale(scale).clickable(onClick = onClick),
         shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(3.dp),
         colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Box(Modifier.background(Brush.verticalGradient(listOf(typeColor.copy(0.12f), Color.White)))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                   modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                Text("#${pokemon.id.toString().padStart(3, '0')}",
                     style = MaterialTheme.typography.labelSmall, color = typeColor.copy(0.7f))
                KamelImage(asyncPainterResource(pokemon.imageUrl), pokemon.displayName,
                    contentScale = ContentScale.Fit, modifier = Modifier.size(80.dp),
                    onLoading = { CircularProgressIndicator(Modifier.size(32.dp), color = typeColor) },
                    onFailure = { CardImagePlaceholder() })
                Text(pokemon.displayName, style = MaterialTheme.typography.labelMedium,
                     fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = AppColors.textPrimary)
                TypeBadge(pokemon.primaryType)
            }
        }
    }
}
