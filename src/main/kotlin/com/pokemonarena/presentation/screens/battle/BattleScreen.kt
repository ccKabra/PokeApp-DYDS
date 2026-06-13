package com.pokemonarena.presentation.screens.battle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.pokemonarena.presentation.navigation.Navigator
import com.pokemonarena.presentation.navigation.Screen
import com.pokemonarena.presentation.theme.AppIcons
import com.pokemonarena.presentation.utils.ErrorMessage
import com.pokemonarena.presentation.utils.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleScreen(gymName: String, viewModel: BattleViewModel, navigator: Navigator) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(gymName) { viewModel.onEvent(BattleUiEvent.Load(gymName)) }
    LaunchedEffect(viewModel) {
        viewModel.navigateToResult.collect { navigator.navigateTo(Screen.BattleResult(gymName)) }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(gymName) },
            navigationIcon = {
                if (state is BattleUiState.Error) {
                    IconButton(onClick = { navigator.goBack() }) {
                        Icon(AppIcons.back, contentDescription = "Volver")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
        )
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is BattleUiState.Loading -> LoadingIndicator()
                is BattleUiState.Error   -> ErrorMessage(s.message,
                    onRetry = { viewModel.onEvent(BattleUiEvent.Load(gymName)) })
                is BattleUiState.Ready   -> BattleStrategyContent(
                    gym = s.gym, teamCards = s.teamCards, botCards = s.botCards, weather = s.weather,
                    fightLabel = "¡PELEAR!  (mejor de 3 rondas)",
                    onMove = { index, up -> viewModel.onEvent(BattleUiEvent.MoveCard(index, up)) },
                    onFight = { viewModel.onEvent(BattleUiEvent.Fight) })
                is BattleUiState.Combat  -> BattleCombatContent(
                    result = s.result, weather = s.weather, itemDropNotice = s.itemDropNotice,
                    finalLabel = "Ver resultado completo",
                    onContinue = { viewModel.onEvent(BattleUiEvent.ContinueToResult) })
            }
        }
    }
}
