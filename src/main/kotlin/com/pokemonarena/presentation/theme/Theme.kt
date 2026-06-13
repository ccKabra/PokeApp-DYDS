package com.pokemonarena.presentation.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val typeColors = mapOf(
    "fire"     to Color(0xFFFF6B35), "water"    to Color(0xFF2196F3),
    "grass"    to Color(0xFF4CAF50), "electric" to Color(0xFFFFD600),
    "ice"      to Color(0xFF80DEEA), "fighting" to Color(0xFFE53935),
    "poison"   to Color(0xFFAB47BC), "ground"   to Color(0xFFBCAAA4),
    "flying"   to Color(0xFF7986CB), "psychic"  to Color(0xFFEC407A),
    "bug"      to Color(0xFF8BC34A), "rock"     to Color(0xFF8D6E63),
    "ghost"    to Color(0xFF5E35B1), "dragon"   to Color(0xFF3F51B5),
    "dark"     to Color(0xFF4E342E), "steel"    to Color(0xFF90A4AE),
    "fairy"    to Color(0xFFF48FB1), "normal"   to Color(0xFF9E9E9E),
    "legendary" to AppColors.goldColor
)

val typeBgColors = mapOf(
    "fire"  to Color(0xFFFFF3E0), "water"    to Color(0xFFE3F2FD),
    "grass" to Color(0xFFE8F5E9), "electric" to Color(0xFFFFFDE7),
    "rock"  to Color(0xFFEFEBE9), "psychic"  to Color(0xFFFCE4EC),
    "ice"   to Color(0xFFE0F7FA), "ghost"    to Color(0xFFEDE7F6),
    "dark"  to Color(0xFFEFEBE9), "dragon"   to Color(0xFFE8EAF6),
    "steel" to Color(0xFFECEFF1), "fairy"    to Color(0xFFFCE4EC),
    "legendary" to AppColors.legendaryDark
)

private val ColorScheme = lightColorScheme(
    primary            = AppColors.primaryColor,
    onPrimary          = AppColors.textIconsColor,
    primaryContainer   = AppColors.lightPrimaryColor,
    onPrimaryContainer = AppColors.darkPrimaryColor,
    secondary          = AppColors.textSecondary,
    onSecondary        = AppColors.textIconsColor,
    secondaryContainer = AppColors.lightPrimaryColor,
    onSecondaryContainer = AppColors.textPrimary,
    tertiary           = AppColors.accentColor,
    onTertiary         = AppColors.textIconsColor,
    tertiaryContainer  = Color(0xFFFBE9E7),
    background         = AppColors.backgroundColor,
    onBackground       = AppColors.textPrimary,
    surface            = AppColors.surfaceColor,
    onSurface          = AppColors.textPrimary,
    surfaceVariant     = AppColors.surfaceVariant,
    onSurfaceVariant   = AppColors.textPrimary,
    error              = AppColors.darkPrimaryColor,
    outline            = AppColors.dividerColor
)

@Composable
fun PokemonArenaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography  = Typography(
            headlineLarge  = MaterialTheme.typography.headlineLarge.copy(color = AppColors.textPrimary),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(color = AppColors.textPrimary),
            titleLarge     = MaterialTheme.typography.titleLarge.copy(color = AppColors.textPrimary),
        ),
        content = content
    )
}
