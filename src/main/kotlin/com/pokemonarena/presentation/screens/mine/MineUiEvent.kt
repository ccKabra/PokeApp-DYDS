package com.pokemonarena.presentation.screens.mine

sealed interface MineUiEvent {
    object Dig : MineUiEvent
    data class AimHit(val sizeFraction: Float) : MineUiEvent
    object AimMiss : MineUiEvent
}
