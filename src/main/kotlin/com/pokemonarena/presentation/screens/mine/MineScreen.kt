package com.pokemonarena.presentation.screens.mine

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokemonarena.domain.entity.CoinMine
import com.pokemonarena.domain.entity.MiningReward
import com.pokemonarena.domain.entity.MiningTier
import com.pokemonarena.presentation.theme.AppColors
import com.pokemonarena.presentation.theme.AppIcons
import com.pokemonarena.presentation.utils.CoinText

private fun tierColor(tier: MiningTier): Color = when (tier) {
    MiningTier.NOTHING -> AppColors.dividerColor
    MiningTier.COMMON  -> AppColors.textSecondary
    MiningTier.NICE    -> AppColors.successColor
    MiningTier.GREAT   -> AppColors.infoColor
    MiningTier.EPIC    -> AppColors.epicColor
    MiningTier.JACKPOT -> AppColors.goldColor
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineScreen(viewModel: MineViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text("Mina de Monedas") }, colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary))
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CoinsHeader(state.coins)

            CaveArea(state) { viewModel.onEvent(MineUiEvent.Dig) }

            SessionStats(state)

            AimGameArea(state,
                onHit  = { viewModel.onEvent(MineUiEvent.AimHit(it)) },
                onMiss = { viewModel.onEvent(MineUiEvent.AimMiss) })

            OddsTable()
        }
    }
}

@Composable
private fun CoinsHeader(coins: Int) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(3.dp)) {
        Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(
                listOf(AppColors.lightPrimaryColor, AppColors.surfaceColor, AppColors.lightPrimaryColor)))) {
            Row(Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.coin, contentDescription = "Monedas",
                     tint = AppColors.coinColor, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(8.dp))
                Text("$coins", style = MaterialTheme.typography.headlineMedium,
                     fontWeight = FontWeight.ExtraBold, color = AppColors.textPrimary)
            }
        }
    }
}

@Composable
private fun CaveArea(state: MineUiState, onDig: () -> Unit) {
    val bounce = remember { Animatable(1f) }
    val wobble = remember { Animatable(0f) }
    val glow   = remember { Animatable(0f) }

    val pressure by animateFloatAsState(state.pressure, tween(300), label = "pressure")

    LaunchedEffect(state.clicks) {
        if (state.clicks > 0) {
            bounce.snapTo(0.90f)
            bounce.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium))
        }
    }
    LaunchedEffect(state.clicks) {
        if (state.clicks > 0) {
            wobble.snapTo(if (state.clicks % 2 == 0) -7f else 7f)
            wobble.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    LaunchedEffect(state.clicks) {
        if (state.lastReward?.tier == MiningTier.JACKPOT) {
            glow.snapTo(1f)
            glow.animateTo(0f, tween(1600))
        } else {
            glow.snapTo(0f)
        }
    }
    LaunchedEffect(state.isBroken) {
        if (state.isBroken) {
            bounce.snapTo(1.35f)
            repeat(4) {
                wobble.animateTo(14f, tween(45))
                wobble.animateTo(-14f, tween(45))
            }
            wobble.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            bounce.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    val idlePulse by rememberInfiniteTransition(label = "idle").animateFloat(
        initialValue = 1f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse), label = "idlePulse")

    val balloonTop = if (state.isBroken) AppColors.textSecondary
                     else lerp(AppColors.primaryColor, AppColors.accentColor, pressure)
    val balloonBottom = if (state.isBroken) AppColors.legendaryMid
                        else lerp(AppColors.darkPrimaryColor, AppColors.accentColor, pressure * 0.6f)
    val inflation = 1f + pressure * 0.30f

    Box(
        Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(
                AppColors.legendaryDark, AppColors.legendaryMid, AppColors.legendaryDark))),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(290.dp).background(Brush.radialGradient(listOf(
                AppColors.goldColor.copy(alpha = 0.55f * glow.value), Color.Transparent)), CircleShape))

        Column(horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.spacedBy(12.dp)) {

            RewardBanner(state.clicks, state.lastReward, state.isBroken)

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(190.dp)) {
                CircularProgressIndicator(
                    progress    = pressure,
                    modifier    = Modifier.size(186.dp),
                    color       = lerp(AppColors.goldColor, AppColors.accentColor, pressure),
                    trackColor  = AppColors.textIconsColor.copy(0.08f),
                    strokeWidth = 7.dp
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp)
                        .scale(bounce.value * inflation * (if (state.isBroken) 1f else idlePulse))
                        .rotate(wobble.value)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(balloonTop, balloonBottom)))
                        .clickable(enabled = !state.isBroken, onClick = onDig)
                ) {
                    if (state.isBroken) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.HourglassEmpty, contentDescription = "Globo explotado",
                                 tint = AppColors.textIconsColor.copy(0.85f), modifier = Modifier.size(36.dp))
                            Text("¡EXPLOTÓ!", color = AppColors.textIconsColor.copy(0.85f),
                                 fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text("${state.cooldownSeconds}s", color = AppColors.goldColor,
                                 fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.TouchApp, contentDescription = "Minar",
                                 tint = AppColors.textIconsColor, modifier = Modifier.size(42.dp))
                            Text("¡MINAR!", color = AppColors.textIconsColor,
                                 fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                }
            }

            Text(
                when {
                    state.isBroken      -> "¡Lo inflaste de más! Esperá a que se recomponga…"
                    state.pressure > 0.6f -> "Cuidado… está a punto de explotar. Frená un segundo y se desinfla."
                    else                -> "Cada golpe lo infla un poco (nunca igual). Si explota, hay castigo."
                },
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    state.isBroken        -> AppColors.goldColor.copy(0.9f)
                    state.pressure > 0.6f -> AppColors.accentColor
                    else                  -> AppColors.textIconsColor.copy(0.6f)
                }
            )
        }
    }
}

@Composable
private fun RewardBanner(clicks: Int, reward: MiningReward?, isBroken: Boolean) {
    Box(Modifier.height(56.dp), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = clicks to isBroken,
            transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith fadeOut() },
            label = "reward"
        ) { (_, broken) ->
            when {
                broken -> Text("El mango no aguantó la presión",
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = AppColors.textIconsColor.copy(0.5f))

                reward == null -> Text("Tu primer golpe te espera…",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textIconsColor.copy(0.55f))

                reward.tier == MiningTier.NOTHING -> Text(reward.tier.displayName,
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = AppColors.textIconsColor.copy(0.45f))

                else -> {
                    val color   = tierColor(reward.tier)
                    val jackpot = reward.tier == MiningTier.JACKPOT
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp))
                            .background(color.copy(alpha = if (jackpot) 0.30f else 0.18f))
                            .padding(horizontal = 18.dp, vertical = 8.dp)) {
                        Icon(AppIcons.coin, contentDescription = null, tint = color,
                             modifier = Modifier.size(if (jackpot) 30.dp else 20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("+${reward.coins}  ·  ${reward.tier.displayName}",
                             color = color, fontWeight = FontWeight.ExtraBold,
                             fontSize = if (jackpot) 22.sp else 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionStats(state: MineUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SessionChip("Golpes", state.clicks.toString())
        SessionChip("Minado en la sesión", state.totalMined.toString())
    }
}

@Composable
private fun SessionChip(label: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
               horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.textSecondary)
        }
    }
}

@Composable
private fun OddsTable() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Probabilidades", fontWeight = FontWeight.Bold,
                 style = MaterialTheme.typography.titleSmall)
            CoinMine.ODDS.forEach { odds ->
                val color = tierColor(odds.tier)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                        Spacer(Modifier.width(8.dp))
                        Text(odds.tier.displayName, Modifier.weight(1f),
                             style = MaterialTheme.typography.bodySmall, color = AppColors.textPrimary)
                        CoinText("+${odds.coins}", color = color,
                                 style = MaterialTheme.typography.labelSmall, iconSize = 12.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(odds.chanceLabel, style = MaterialTheme.typography.labelSmall,
                             color = AppColors.textSecondary, modifier = Modifier.width(38.dp))
                    }
                    Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                            .background(AppColors.surfaceVariant)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(odds.chance)
                                .clip(RoundedCornerShape(3.dp)).background(color.copy(0.75f)))
                    }
                }
            }
        }
    }
}
