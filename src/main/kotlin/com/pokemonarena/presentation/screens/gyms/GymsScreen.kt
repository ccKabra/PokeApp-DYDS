package com.pokemonarena.presentation.screens.gyms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.scrollBy
import com.pokemonarena.domain.entity.BattleRewards
import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.Gym
import com.pokemonarena.domain.entity.Region
import kotlinx.coroutines.delay
import com.pokemonarena.presentation.theme.AppColors
import com.pokemonarena.presentation.theme.AppIcons
import com.pokemonarena.presentation.theme.typeBgColors
import com.pokemonarena.presentation.theme.typeColors
import com.pokemonarena.presentation.utils.CardSprite
import com.pokemonarena.presentation.utils.CoinText
import com.pokemonarena.presentation.utils.ErrorMessage
import com.pokemonarena.presentation.utils.LoadingIndicator
import com.pokemonarena.presentation.utils.WeatherLabel
import com.pokemonarena.presentation.navigation.Navigator
import com.pokemonarena.presentation.navigation.Screen
import com.pokemonarena.presentation.screens.battle.DifficultyStars
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymsScreen(viewModel: GymsViewModel, navigator: Navigator, region: Region) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(region) { viewModel.onEvent(GymsUiEvent.Load(region)) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Gimnasios de ${region.displayName}") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary))
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> LoadingIndicator()
                state.error != null -> ErrorMessage(state.error!!,
                    onRetry = { viewModel.onEvent(GymsUiEvent.Load(region)) })
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        if (!state.regionUnlocked) LockedRegionBanner(region)
                        if (state.regionUnlocked && state.teamSize < 3) NoTeamBanner()
                        LazyColumn(contentPadding = PaddingValues(16.dp),
                                   verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(state.gyms, key = { it.gym.name }) { gww ->
                                GymCard(gww,
                                        canChallenge = state.teamSize >= 3 && state.regionUnlocked,
                                        earned = gww.gym.name in state.earnedBadges) {
                                    navigator.navigateTo(Screen.Battle(gww.gym.name))
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
private fun LockedRegionBanner(region: Region) {
    val previous = region.previous?.displayName ?: ""
    Card(colors = CardDefaults.cardColors(containerColor = AppColors.legendaryMid),
         modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Región bloqueada: conquistá las 8 medallas y la Liga Pokémon de $previous para desbloquear ${region.displayName}.",
             Modifier.padding(14.dp), color = AppColors.goldColor,
             fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NoTeamBanner() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
         modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Necesitás 3 cartas en Mi Equipo para desafiar un gimnasio.",
             Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun GymCard(gww: GymWithWeather, canChallenge: Boolean, earned: Boolean, onChallenge: () -> Unit) {
    val isLegendary = gww.gym.typeSpecialty == "legendary"
    val typeColor   = if (isLegendary) AppColors.goldColor else (typeColors[gww.gym.typeSpecialty] ?: Color.Gray)
    val bgGradient  = if (isLegendary) Brush.horizontalGradient(listOf(AppColors.legendaryDark, AppColors.legendaryMid))
                      else Brush.horizontalGradient(listOf(typeColor, typeColor.copy(0.6f)))

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
         elevation = CardDefaults.cardElevation(if (isLegendary) 12.dp else 6.dp)) {
        Column {
            GymHeader(gww.gym, isLegendary, bgGradient, earned)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WeatherBox(gww, isLegendary, typeColor)
                BotCardsRow(gww.gym, isLegendary, typeColor)
                ChallengeButton(gww, isLegendary, typeColor, canChallenge, onChallenge)
            }
        }
    }
}

@Composable
private fun GymHeader(gym: Gym, isLegendary: Boolean, bgGradient: Brush, earned: Boolean) {
    val headlineColor = if (isLegendary) AppColors.goldColor else AppColors.textIconsColor
    Box(Modifier.fillMaxWidth().background(bgGradient).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(AppColors.textIconsColor.copy(0.2f)),
                contentAlignment = Alignment.Center) {
                if (earned && gym.badgeImageUrl != null) {
                    KamelImage(asyncPainterResource(gym.badgeImageUrl), "Medalla de ${gym.name}",
                        contentScale = ContentScale.Fit, modifier = Modifier.size(40.dp),
                        onLoading = {}, onFailure = {
                            Text(gym.typeSpecialty.take(1).uppercase(),
                                 color = headlineColor, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                        })
                } else if (earned) {
                    Icon(AppIcons.trophy, contentDescription = "Medalla de ${gym.name}",
                         tint = AppColors.goldColor, modifier = Modifier.size(32.dp))
                } else {
                    Text(gym.typeSpecialty.take(1).uppercase(),
                         color = headlineColor, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(gym.name, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = headlineColor)
                    if (earned) {
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(AppColors.goldColor.copy(0.25f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("CONQUISTADO", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                 color = if (isLegendary) AppColors.goldColor else AppColors.textIconsColor)
                        }
                    }
                }
                Text(gym.city, style = MaterialTheme.typography.bodySmall,
                     color = headlineColor.copy(if (isLegendary) 0.7f else 0.85f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DifficultyStars(gym.difficulty)
                    Spacer(Modifier.width(8.dp))
                    CoinText("hasta ${BattleRewards.maxRewardFor(gym.difficulty)}",
                             color = headlineColor.copy(0.9f),
                             style = MaterialTheme.typography.labelSmall)
                }
            }
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(AppColors.textIconsColor.copy(0.2f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(gym.typeSpecialty.replaceFirstChar { it.uppercase() },
                     color = headlineColor, fontWeight = FontWeight.Bold,
                     style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun WeatherBox(gww: GymWithWeather, isLegendary: Boolean, typeColor: Color) {
    val bgColor = if (isLegendary) AppColors.legendaryMid else (typeBgColors[gww.gym.typeSpecialty] ?: AppColors.surfaceVariant)
    val mainColor = if (isLegendary) AppColors.textIconsColor else AppColors.textPrimary
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bgColor).padding(12.dp)) {
        if (gww.loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = typeColor)
                Spacer(Modifier.width(8.dp))
                Text("Consultando clima…", style = MaterialTheme.typography.bodySmall, color = mainColor)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.place, contentDescription = null,
                         tint = typeColor, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(gww.gym.city, style = MaterialTheme.typography.labelSmall,
                         color = typeColor, fontWeight = FontWeight.SemiBold)
                }
                gww.weather?.let { w ->
                    WeatherLabel(w, color = mainColor, style = MaterialTheme.typography.bodyMedium)
                    if (w.boostedTypes.isNotEmpty())
                        Text("Potencia: ${w.boostedTypes.joinToString { t -> t.replaceFirstChar { it.uppercase() } }} ×${w.multiplier}",
                             style = MaterialTheme.typography.bodySmall, color = typeColor)
                }
            }
        }
    }
}

@Composable
private fun BotCardsRow(gym: Gym, isLegendary: Boolean, typeColor: Color) {
    val bgColor = if (isLegendary) AppColors.legendaryMid else (typeBgColors[gym.typeSpecialty] ?: AppColors.surfaceVariant)
    val pool    = gym.cardPool
    Text("Pool del líder (${pool.size} Pokémon — saca 3 al azar cuando lo desafiás):",
         style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
         color = AppColors.textPrimary)
    val startIndex = (Int.MAX_VALUE / 2 / pool.size) * pool.size
    val listState  = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    LaunchedEffect(pool) {
        while (true) {
            listState.scrollBy(1.2f)
            delay(30)
        }
    }
    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Int.MAX_VALUE) { index ->
            BotCardItem(pool[index % pool.size], isLegendary, bgColor, typeColor)
        }
    }
}

@Composable
private fun BotCardItem(card: Card, isLegendary: Boolean, bgColor: Color, typeColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
           modifier = Modifier.width(116.dp).clip(RoundedCornerShape(10.dp)).background(bgColor).padding(8.dp)) {
        CardSprite(card, imageSize = 80.dp)
        Text(card.name, style = MaterialTheme.typography.labelSmall, maxLines = 1,
             color = if (isLegendary) AppColors.textIconsColor else AppColors.textPrimary)
        card.heldItem?.let {
            Text(it.name, style = MaterialTheme.typography.labelSmall, maxLines = 1,
                 fontWeight = FontWeight.Bold, color = typeColor)
        }
    }
}

@Composable
private fun ChallengeButton(gww: GymWithWeather, isLegendary: Boolean, typeColor: Color,
                             canChallenge: Boolean, onChallenge: () -> Unit) {
    Button(onClick = onChallenge, enabled = canChallenge && gww.weather != null,
           colors = ButtonDefaults.buttonColors(
               containerColor = if (isLegendary) AppColors.goldColor else typeColor,
               contentColor   = if (isLegendary) AppColors.textPrimary else AppColors.textIconsColor),
           shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text(when {
            !canChallenge       -> "Seleccioná tu equipo en Mi Equipo"
            gww.weather == null -> "Cargando clima…"
            else                -> "¡Desafiar!"
        }, fontWeight = FontWeight.ExtraBold)
    }
}
