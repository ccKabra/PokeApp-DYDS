package com.pokemonarena.presentation.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.pokemonarena.domain.entity.WeatherCondition
import com.pokemonarena.domain.entity.Winner

object AppIcons {

    fun weather(condition: WeatherCondition): ImageVector = when (condition) {
        WeatherCondition.SUNNY       -> Icons.Filled.WbSunny
        WeatherCondition.EXTREME_SUN -> Icons.Filled.Whatshot
        WeatherCondition.RAIN        -> Icons.Filled.Opacity
        WeatherCondition.STORM       -> Icons.Filled.FlashOn
        WeatherCondition.SNOW        -> Icons.Filled.AcUnit
        WeatherCondition.SANDSTORM   -> Icons.Filled.Terrain
        WeatherCondition.FOG         -> Icons.Filled.CloudQueue
        WeatherCondition.CLEAR       -> Icons.Filled.WbCloudy
    }

    fun battleOutcome(winner: Winner): ImageVector = when (winner) {
        Winner.PLAYER -> Icons.Filled.EmojiEvents
        Winner.BOT    -> Icons.Filled.Close
        Winner.DRAW   -> Icons.Filled.Remove
    }

    val coin    = Icons.Filled.MonetizationOn
    val battle  = Icons.Filled.SportsMma
    val cards   = Icons.Filled.Style
    val trophy  = Icons.Filled.EmojiEvents
    val streak  = Icons.Filled.Whatshot
    val rate    = Icons.Filled.TrendingUp
    val place   = Icons.Filled.Place
    val star    = Icons.Filled.Star
    val refresh = Icons.Filled.Refresh
    val warning = Icons.Filled.Warning
    val back    = Icons.Filled.ArrowBack
    val empty   = Icons.Filled.BrokenImage
}
