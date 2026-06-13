package com.pokemonarena.presentation.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokemonarena.domain.entity.PlayerProfile
import com.pokemonarena.domain.entity.Region
import com.pokemonarena.presentation.theme.AppColors
import com.pokemonarena.presentation.theme.AppIcons
import com.pokemonarena.presentation.utils.spriteUrl
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun NavigationSidebar(
    navigator: Navigator, coins: Int, locked: Boolean = false,
    unlockedRegions: Set<Region> = Region.entries.toSet(),
    profile: PlayerProfile? = null
) {
    val current = navigator.current
    Column(
        modifier = Modifier.width(200.dp).fillMaxHeight()
            .background(Brush.verticalGradient(listOf(
                AppColors.darkPrimaryColor, AppColors.primaryColor, AppColors.darkPrimaryColor)))
    ) {
        Row(
            Modifier.fillMaxWidth().background(Color.Black.copy(0.2f)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (profile != null) {
                KamelImage(asyncPainterResource(profile.gender.spriteUrl), profile.name,
                    modifier = Modifier.size(38.dp).clip(CircleShape)
                        .background(Color.Black.copy(0.25f)),
                    onLoading = {},
                    onFailure = {
                        Icon(Icons.Filled.CatchingPokemon, contentDescription = "PokeApp",
                             tint = AppColors.textIconsColor, modifier = Modifier.size(34.dp))
                    })
            } else {
                Icon(Icons.Filled.CatchingPokemon, contentDescription = "PokeApp",
                     tint = AppColors.textIconsColor, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("PokeApp", color = AppColors.textIconsColor,
                     fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                profile?.let {
                    Text(it.name, color = AppColors.textIconsColor.copy(0.7f), fontSize = 11.sp)
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(0.25f)).padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.coin, contentDescription = "Monedas",
                     tint = AppColors.coinColor, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Monedas", color = AppColors.textIconsColor.copy(0.7f), fontSize = 11.sp)
                    Text("$coins", color = AppColors.textIconsColor,
                         fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }
        }
        HorizontalDivider(color = AppColors.textIconsColor.copy(0.2f))
        Spacer(Modifier.height(4.dp))

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            mainNavDestinations.forEach { dest ->
                SidebarItem(dest.icon, dest.label, current::class == dest.screen::class, enabled = !locked) {
                    navigator.navigateTo(dest.screen)
                }
            }

            val inBattleSection = current is Screen.Gyms || current is Screen.League ||
                                  current is Screen.Battle || current is Screen.BattleResult ||
                                  current is Screen.Rogue
            var battleOpen by remember { mutableStateOf(false) }
            LaunchedEffect(inBattleSection) { if (inBattleSection) battleOpen = true }
            SidebarItem(AppIcons.battle, "Batalla", inBattleSection, enabled = !locked) {
                battleOpen = !battleOpen
            }
            if (battleOpen) {
                Region.entries.forEach { region ->
                    val regionLocked = region !in unlockedRegions
                    SubSidebarItem("Gimnasios de ${region.displayName}",
                                   selected = current == Screen.Gyms(region),
                                   enabled = !locked, regionLocked = regionLocked) {
                        navigator.navigateTo(Screen.Gyms(region))
                    }
                    SubSidebarItem("Liga Pokémon de ${region.displayName}",
                                   selected = current == Screen.League(region),
                                   enabled = !locked, regionLocked = regionLocked) {
                        navigator.navigateTo(Screen.League(region))
                    }
                }
                SubSidebarItem("★ Expedición Rogue",
                               selected = current is Screen.Rogue, enabled = !locked) {
                    navigator.navigateTo(Screen.Rogue)
                }
            }

            secondaryNavDestinations.forEach { dest ->
                SidebarItem(dest.icon, dest.label, current::class == dest.screen::class, enabled = !locked) {
                    navigator.navigateTo(dest.screen)
                }
            }
        }

        if (locked) {
            Text("Desafío en curso — terminálo para poder salir",
                 color = AppColors.textIconsColor.copy(0.6f),
                 style = MaterialTheme.typography.labelSmall,
                 modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        Text("v1.0.0", color = AppColors.textIconsColor.copy(0.35f),
             style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(16.dp))
    }
}

private val invalidCursor: PointerIcon by lazy {
    runCatching { PointerIcon(java.awt.Cursor.getSystemCustomCursor("Invalid.32x32")) }
        .getOrDefault(PointerIcon.Default)
}

@Composable
private fun SidebarItem(icon: ImageVector, label: String, selected: Boolean,
                        enabled: Boolean = true, onClick: () -> Unit) {
    val bgAlpha by animateFloatAsState(if (selected) 0.25f else 0f, label = "bg")
    val scale   by animateFloatAsState(if (selected) 1.0f  else 0.95f, label = "sc")
    val tint    = AppColors.textIconsColor.copy(if (enabled) 1f else 0.35f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp)).background(AppColors.textIconsColor.copy(bgAlpha))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp).scale(scale)
    ) {
        Icon(icon, contentDescription = label, tint = tint,
             modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = tint,
             fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
             fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (selected) Box(Modifier.size(6.dp).clip(CircleShape).background(AppColors.textIconsColor))
    }
}

@Composable
private fun SubSidebarItem(label: String, selected: Boolean, enabled: Boolean,
                           regionLocked: Boolean = false, onClick: () -> Unit) {
    val clickEnabled = enabled && !regionLocked
    val tint = AppColors.textIconsColor.copy(
        if (regionLocked) 0.3f else if (enabled) (if (selected) 1f else 0.8f) else 0.35f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 8.dp, top = 1.dp, bottom = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.textIconsColor.copy(if (selected) 0.22f else 0f))
            .clickable(enabled = clickEnabled, onClick = onClick)
            .pointerHoverIcon(if (regionLocked) invalidCursor else PointerIcon.Default)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(label, color = tint, fontSize = 12.sp,
             fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
             modifier = Modifier.weight(1f))
        if (regionLocked) Icon(Icons.Filled.Lock, contentDescription = "Región bloqueada",
                               tint = tint, modifier = Modifier.size(12.dp))
    }
}
