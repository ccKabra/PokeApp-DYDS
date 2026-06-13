package com.pokemonarena.presentation.screens.mine

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokemonarena.domain.entity.AimGame
import com.pokemonarena.presentation.theme.AppColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val SPEED_FRACTION_PER_SECOND = 0.30f
private const val MIN_RADIUS_FRACTION       = 0.05f
private const val MAX_RADIUS_FRACTION       = 0.12f
private const val HIT_TOLERANCE             = 1.15f

private data class Balloon(
    val center:       Offset,
    val direction:    Offset,
    val sizeFraction: Float,
    val radiusPx:     Float,
    val bornAtNanos:  Long
)

private fun spawnBalloon(random: Random, area: IntSize, nowNanos: Long): Balloon {
    val sizeFraction = random.nextFloat()
    val radius = (MIN_RADIUS_FRACTION +
                  sizeFraction * (MAX_RADIUS_FRACTION - MIN_RADIUS_FRACTION)) * area.height
    val angle  = random.nextFloat() * 2f * PI.toFloat()
    fun randomWithin(limit: Int) = radius + random.nextFloat() * (limit - 2 * radius)
    return Balloon(
        center       = Offset(randomWithin(area.width), randomWithin(area.height)),
        direction    = Offset(cos(angle), sin(angle)),
        sizeFraction = sizeFraction,
        radiusPx     = radius,
        bornAtNanos  = nowNanos
    )
}

private fun Balloon.advanced(deltaSeconds: Float, area: IntSize): Balloon {
    val speed = SPEED_FRACTION_PER_SECOND * area.height
    var x  = center.x + direction.x * speed * deltaSeconds
    var y  = center.y + direction.y * speed * deltaSeconds
    var dx = direction.x
    var dy = direction.y
    if (x < radiusPx) { x = radiusPx; dx = -dx }
    if (x > area.width - radiusPx)  { x = area.width - radiusPx;  dx = -dx }
    if (y < radiusPx) { y = radiusPx; dy = -dy }
    if (y > area.height - radiusPx) { y = area.height - radiusPx; dy = -dy }
    return copy(center = Offset(x, y), direction = Offset(dx, dy))
}

@Composable
fun AimGameArea(state: MineUiState, onHit: (Float) -> Unit, onMiss: () -> Unit) {
    val random = remember { Random.Default }
    var areaSize by remember { mutableStateOf(IntSize.Zero) }
    var balloons by remember { mutableStateOf(emptyList<Balloon>()) }

    LaunchedEffect(areaSize) {
        if (areaSize == IntSize.Zero) return@LaunchedEffect
        var last = 0L
        balloons = List(AimGame.BALLOON_COUNT) { spawnBalloon(random, areaSize, 0L) }
        while (true) {
            withFrameNanos { now ->
                val deltaSeconds = if (last == 0L) 0f else (now - last) / 1_000_000_000f
                last = now
                balloons = balloons.map { balloon ->
                    val expired = now - balloon.bornAtNanos > AimGame.LIFETIME_MS * 1_000_000
                    if (expired || balloon.bornAtNanos == 0L) spawnBalloon(random, areaSize, now)
                    else balloon.advanced(deltaSeconds, areaSize)
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(
                    AppColors.legendaryDark, AppColors.legendaryMid, AppColors.legendaryDark)))
                .onSizeChanged { areaSize = it }
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val hit = balloons.lastOrNull {
                            (tap - it.center).getDistance() <= it.radiusPx * HIT_TOLERANCE
                        }
                        if (hit != null) {
                            onHit(hit.sizeFraction)
                            balloons = balloons.map {
                                if (it === hit) spawnBalloon(random, areaSize, 0L) else it
                            }
                        } else {
                            onMiss()
                        }
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                balloons.forEach { balloon ->
                    val color = androidx.compose.ui.graphics.lerp(
                        AppColors.goldColor, AppColors.accentColor, balloon.sizeFraction)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color, color.copy(alpha = 0.35f)),
                            center = balloon.center, radius = balloon.radiusPx),
                        radius = balloon.radiusPx,
                        center = balloon.center
                    )
                }
            }
            AimFeedback(state, Modifier.align(Alignment.TopEnd).padding(12.dp))
        }
        Text(
            "Puntería: reventá los globos antes de que exploten solos (${AimGame.LIFETIME_MS / 1000}s). " +
            "Cuanto más chico el globo, más paga (+${AimGame.MIN_HIT_REWARD} a +${AimGame.MAX_HIT_REWARD}). " +
            "Si errás, perdés ${AimGame.MISS_PENALTY} monedas.",
            style = MaterialTheme.typography.labelSmall, color = AppColors.textSecondary
        )
    }
}

@Composable
private fun AimFeedback(state: MineUiState, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.End) {
        AnimatedContent(
            targetState = state.aimShots to state.lastAimDelta,
            transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith fadeOut() },
            label = "aimDelta"
        ) { (_, delta) ->
            if (delta != null) {
                val positive = delta > 0
                Text(
                    if (positive) "+$delta" else "$delta",
                    color = if (positive) AppColors.successColor else AppColors.defeatColor,
                    fontWeight = FontWeight.ExtraBold, fontSize = 20.sp
                )
            }
        }
        if (state.aimShots > 0) {
            Text("Neto: ${state.aimNet}", color = AppColors.textIconsColor.copy(0.6f),
                 style = MaterialTheme.typography.labelSmall)
        }
    }
}
