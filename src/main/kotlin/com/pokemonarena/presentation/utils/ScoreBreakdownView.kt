package com.pokemonarena.presentation.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.ScoreBreakdown
import com.pokemonarena.domain.entity.WeatherCondition
import com.pokemonarena.presentation.theme.AppColors

@Composable
fun ScoreBreakdownColumn(
    card: Card, weather: WeatherCondition, typeMultiplier: Float,
    crit: Boolean, missed: Boolean, score: Float,
    modifier: Modifier = Modifier
) {
    val b = ScoreBreakdown.of(card, weather, typeMultiplier, crit, missed)
    Column(
        modifier.clip(RoundedCornerShape(10.dp))
            .background(AppColors.surfaceColor.copy(alpha = 0.65f))
            .padding(8.dp)
    ) {
        Text(card.name, style = MaterialTheme.typography.labelSmall,
             fontWeight = FontWeight.Bold, color = AppColors.textPrimary)
        BreakdownLine("Stats", "%.3f".format(b.statsBase), AppColors.textSecondary)
        if (b.missed) {
            BreakdownLine("¡Falló el ataque!", "×0", AppColors.defeatColor)
        } else {
            card.heldItem?.let { item ->
                if (b.itemMultiplier != 1f)
                    BreakdownLine("Item (${item.name})", multiplier(b.itemMultiplier),
                                  boostColor(b.itemMultiplier))
            }
            if (b.rarityMultiplier != 1f)
                BreakdownLine("Rareza (${card.rarity})", multiplier(b.rarityMultiplier),
                              boostColor(b.rarityMultiplier))
            if (b.fatigueMultiplier != 1f)
                BreakdownLine("Fatiga (${card.timesUsed} usos)", multiplier(b.fatigueMultiplier),
                              AppColors.defeatColor)
            if (b.weatherMultiplier != 1f)
                BreakdownLine("Clima (${weather.displayName})", multiplier(b.weatherMultiplier),
                              boostColor(b.weatherMultiplier))
            if (b.typeMultiplier != 1f)
                BreakdownLine("Ventaja de tipo", multiplier(b.typeMultiplier),
                              boostColor(b.typeMultiplier))
            if (crit)
                BreakdownLine("¡Crítico!", multiplier(b.critMultiplier), AppColors.critColor)
        }
        HorizontalDivider(Modifier.padding(vertical = 3.dp), color = AppColors.dividerColor.copy(0.5f))
        BreakdownLine("Total", "%.3f pts".format(score), AppColors.textPrimary, bold = true)
    }
}

@Composable
private fun BreakdownLine(label: String, value: String, color: Color, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
             color = color, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Normal)
        Text(value, style = MaterialTheme.typography.labelSmall,
             color = color, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold)
    }
}

private fun multiplier(value: Float) = "×%.2f".format(value)

private fun boostColor(value: Float) =
    if (value >= 1f) AppColors.successColor else AppColors.defeatColor
