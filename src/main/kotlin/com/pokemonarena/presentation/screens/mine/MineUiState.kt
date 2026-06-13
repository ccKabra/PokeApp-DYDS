package com.pokemonarena.presentation.screens.mine

import com.pokemonarena.domain.entity.Economy
import com.pokemonarena.domain.entity.MiningReward

data class MineUiState(
    val coins:           Int           = Economy.STARTING_COINS,
    val lastReward:      MiningReward? = null,
    val totalMined:      Int           = 0,
    val clicks:          Int           = 0,
    val pressure:        Float         = 0f,
    val isBroken:        Boolean       = false,
    val cooldownSeconds: Int           = 0,
    val lastAimDelta:    Int?          = null,
    val aimShots:        Int           = 0,
    val aimNet:          Int           = 0
)
