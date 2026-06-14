package com.pokemonarena

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import com.pokemonarena.core.Constants
import com.pokemonarena.data.local.database.DatabaseFactory
import com.pokemonarena.di.DependencyInjector
import com.pokemonarena.presentation.navigation.*
import com.pokemonarena.presentation.screens.battle.BattleScreen
import com.pokemonarena.presentation.screens.battle.BattleViewModel
import com.pokemonarena.presentation.screens.collection.CollectionScreen
import com.pokemonarena.presentation.screens.collection.CollectionViewModel
import com.pokemonarena.presentation.screens.detail.CardDetailScreen
import com.pokemonarena.presentation.screens.detail.CardDetailViewModel
import com.pokemonarena.presentation.screens.gyms.GymsScreen
import com.pokemonarena.presentation.screens.gyms.GymsViewModel
import com.pokemonarena.presentation.screens.home.HomeScreen
import com.pokemonarena.presentation.screens.home.HomeViewModel
import com.pokemonarena.presentation.screens.items.ItemsScreen
import com.pokemonarena.presentation.screens.items.ItemsViewModel
import com.pokemonarena.domain.entity.PlayerProfile
import com.pokemonarena.presentation.screens.league.LeagueScreen
import com.pokemonarena.presentation.screens.league.LeagueUiState
import com.pokemonarena.presentation.screens.league.LeagueViewModel
import com.pokemonarena.presentation.screens.mine.MineScreen
import com.pokemonarena.presentation.screens.profile.ProfileSetupScreen
import com.pokemonarena.presentation.screens.rogue.RogueScreen
import com.pokemonarena.presentation.screens.profile.ProfileUiEvent
import com.pokemonarena.presentation.screens.profile.ProfileUiState
import com.pokemonarena.presentation.utils.LoadingIndicator
import com.pokemonarena.presentation.screens.mine.MineViewModel
import com.pokemonarena.presentation.screens.myteam.MyTeamScreen
import com.pokemonarena.presentation.screens.myteam.MyTeamViewModel
import com.pokemonarena.presentation.screens.result.ResultScreen
import com.pokemonarena.presentation.screens.statistics.StatisticsScreen
import com.pokemonarena.presentation.screens.statistics.StatisticsViewModel
import com.pokemonarena.presentation.theme.PokemonArenaTheme

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    DatabaseFactory.init()
    application {
        val state = rememberWindowState(width = Constants.WINDOW_WIDTH.dp, height = Constants.WINDOW_HEIGHT.dp)
        CompositionLocalProvider(
            LocalWindowExceptionHandlerFactory provides WindowExceptionHandlerFactory { _ ->
                WindowExceptionHandler { throwable ->
                    if (!throwable.isKnownComposeFocusBug()) throw throwable
                }
            }
        ) {
            Window(onCloseRequest = ::exitApplication, title = Constants.WINDOW_TITLE,
                   state = state, resizable = true) {
                LaunchedEffect(Unit) {
                    window.minimumSize = java.awt.Dimension(Constants.MIN_WIDTH, Constants.MIN_HEIGHT)
                }
                PokemonArenaApp()
            }
        }
    }
}

private fun Throwable.isKnownComposeFocusBug(): Boolean =
    this is IllegalArgumentException && message?.contains("ActiveParent with no focused child") == true

@Composable
fun PokemonArenaApp() {
    val profileVM = remember { DependencyInjector.profileViewModel() }
    val profileState by profileVM.uiState.collectAsState()

    PokemonArenaTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (val p = profileState) {
                is ProfileUiState.Loading -> LoadingIndicator()
                is ProfileUiState.Missing -> ProfileSetupScreen { name, gender ->
                    profileVM.onEvent(ProfileUiEvent.Save(name, gender))
                }
                is ProfileUiState.Ready   -> MainContent(p.profile)
            }
        }
    }
}

@Composable
private fun MainContent(profile: PlayerProfile) {
    val navigator    = remember { Navigator() }
    val homeVM       = remember { DependencyInjector.homeViewModel() }
    val collectionVM = remember { DependencyInjector.collectionViewModel() }
    val gymsVM       = remember { DependencyInjector.gymsViewModel() }
    val myTeamVM     = remember { DependencyInjector.myTeamViewModel() }
    val statsVM      = remember { DependencyInjector.statisticsViewModel() }
    val mineVM       = remember { DependencyInjector.mineViewModel() }
    val itemsVM      = remember { DependencyInjector.itemsViewModel() }
    val battleVM     = remember { DependencyInjector.battleViewModel() }
    val rogueVM      = remember { DependencyInjector.rogueViewModel() }
    val navVM        = remember { DependencyInjector.navigationViewModel() }
    val coins by navVM.coins.collectAsState()

    val screen = navigator.current
    val leagueVM: LeagueViewModel? = if (screen is Screen.League) {
        val vm = remember(screen.region) { DependencyInjector.leagueViewModel() }
        DisposableEffect(vm) { onDispose { vm.dispose() } }
        vm
    } else null
    val leagueState = leagueVM?.uiState?.collectAsState()?.value
    val inLeagueChallenge = leagueState is LeagueUiState.Prep ||
                            leagueState is LeagueUiState.Strategy ||
                            leagueState is LeagueUiState.Combat

    val rogueState = rogueVM.uiState.collectAsState().value
    val inRogueRun = screen is Screen.Rogue && rogueState.locksNavigation

    Row(Modifier.fillMaxSize()) {
        NavigationSidebar(navigator, coins,
                          locked = screen is Screen.Battle || inLeagueChallenge || inRogueRun,
                          unlockedRegions = navVM.unlockedRegions.collectAsState().value,
                          profile = profile)
        Box(Modifier.weight(1f).fillMaxHeight()) {
            when (screen) {
                is Screen.Home        -> HomeScreen(homeVM, navigator)
                is Screen.Collection  -> CollectionScreen(collectionVM, navigator)
                is Screen.Gyms        -> GymsScreen(gymsVM, navigator, screen.region)
                is Screen.League      -> leagueVM?.let { LeagueScreen(it, navigator, screen.region) }
                is Screen.Rogue       -> RogueScreen(rogueVM)
                is Screen.MyTeam      -> MyTeamScreen(myTeamVM, navigator)
                is Screen.Statistics  -> StatisticsScreen(statsVM)
                is Screen.Mine        -> MineScreen(mineVM)
                is Screen.Items       -> ItemsScreen(itemsVM)
                is Screen.CardDetail  -> {
                    val vm = remember(screen.pokemonName) { DependencyInjector.cardDetailViewModel() }
                    DisposableEffect(vm) { onDispose { vm.dispose() } }
                    CardDetailScreen(screen.pokemonName, vm, navigator)
                }
                is Screen.Battle      -> BattleScreen(screen.gymName, battleVM, navigator)
                is Screen.BattleResult -> ResultScreen(battleVM, navigator)
            }
        }
    }
}
