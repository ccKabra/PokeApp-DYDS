package com.pokemonarena.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.pokemonarena.domain.entity.Region

sealed class Screen(val isRoot: Boolean = false) {
    object Home        : Screen(isRoot = true)
    object Collection  : Screen(isRoot = true)
    object MyTeam      : Screen(isRoot = true)
    object Statistics  : Screen(isRoot = true)
    object Mine        : Screen(isRoot = true)
    object Items       : Screen(isRoot = true)
    data class Gyms(val region: Region = Region.KANTO)   : Screen(isRoot = true)
    data class League(val region: Region = Region.KANTO) : Screen(isRoot = true)
    object Rogue : Screen(isRoot = true)
    data class CardDetail(val pokemonName: String) : Screen()
    data class Battle(val gymName: String)         : Screen()
    data class BattleResult(val gymName: String)   : Screen()
}

class Navigator {
    private val _stack = mutableStateListOf<Screen>(Screen.Home)
    val current: Screen get() = _stack.last()

    fun navigateTo(screen: Screen) {
        if (screen.isRoot) _stack.clear()
        _stack.add(screen)
    }

    fun goBack() { if (_stack.size > 1) _stack.removeLast() }
    val canGoBack: Boolean get() = _stack.size > 1
}

data class NavDestination(val screen: Screen, val label: String, val icon: ImageVector)

val mainNavDestinations = listOf(
    NavDestination(Screen.Home,       "Inicio",    Icons.Filled.Home),
    NavDestination(Screen.Collection, "Pokédex",   Icons.Filled.MenuBook)
)

val secondaryNavDestinations = listOf(
    NavDestination(Screen.MyTeam,     "Mi Equipo", Icons.Filled.Style),
    NavDestination(Screen.Items,      "Items",     Icons.Filled.ShoppingCart),
    NavDestination(Screen.Statistics, "Stats",     Icons.Filled.BarChart),
    NavDestination(Screen.Mine,       "Mina",      Icons.Filled.TouchApp)
)
