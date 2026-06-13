package com.pokemonarena.presentation.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.WeatherCondition
import com.pokemonarena.presentation.theme.AppColors
import com.pokemonarena.presentation.theme.AppIcons
import com.pokemonarena.presentation.theme.typeColors
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorMessage(message: String, onRetry: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(AppIcons.warning, contentDescription = null,
                 tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
            Text(message, color = MaterialTheme.colorScheme.error)
            onRetry?.let { retry ->
                Button(onClick = retry) { Text("Reintentar") }
            }
        }
    }
}

@Composable
fun TypeBadge(type: String) {
    val color = typeColors[type] ?: Color.Gray
    Box(Modifier.padding(horizontal = 4.dp)) {
        Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.2f),
                contentColor = color) {
            Text(type.replaceFirstChar { it.uppercase() },
                 Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                 style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall,
         fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
fun CoinText(
    text: String,
    color: Color = AppColors.coinColor,
    style: TextStyle = MaterialTheme.typography.labelMedium,
    fontWeight: FontWeight = FontWeight.Bold,
    iconSize: Dp = 15.dp
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(AppIcons.coin, contentDescription = "monedas", tint = color,
             modifier = Modifier.size(iconSize))
        Spacer(Modifier.width(3.dp))
        Text(text, color = color, style = style, fontWeight = fontWeight)
    }
}

@Composable
fun WeatherLabel(
    weather: WeatherCondition,
    color: Color = AppColors.textPrimary,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    fontWeight: FontWeight = FontWeight.Bold,
    iconSize: Dp = 18.dp
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(AppIcons.weather(weather), contentDescription = weather.displayName,
             tint = color, modifier = Modifier.size(iconSize))
        Spacer(Modifier.width(5.dp))
        Text(weather.displayName, color = color, style = style, fontWeight = fontWeight)
    }
}

@Composable
fun CardImagePlaceholder(size: Dp = 32.dp) {
    Icon(AppIcons.empty, contentDescription = "Imagen no disponible",
         tint = AppColors.dividerColor, modifier = Modifier.size(size))
}

@Composable
fun CardSprite(card: Card, imageSize: Dp, modifier: Modifier = Modifier) {
    val badgeSize = (imageSize.value / 2.4f).dp.coerceAtLeast(18.dp)
    Box(modifier, contentAlignment = Alignment.Center) {
        KamelImage(asyncPainterResource(card.imageUrlSmall), card.name,
            contentScale = ContentScale.Fit, modifier = Modifier.size(imageSize),
            onLoading = { CircularProgressIndicator(Modifier.size(imageSize / 3)) },
            onFailure = { CardImagePlaceholder(imageSize / 2) })
        card.heldItem?.let { item ->
            Box(
                Modifier.align(Alignment.BottomEnd).size(badgeSize).clip(CircleShape)
                    .background(AppColors.surfaceColor)
                    .border(1.dp, AppColors.dividerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                KamelImage(asyncPainterResource(item.imageUrl), item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(badgeSize - 4.dp),
                    onLoading = {}, onFailure = {})
            }
        }
    }
}
