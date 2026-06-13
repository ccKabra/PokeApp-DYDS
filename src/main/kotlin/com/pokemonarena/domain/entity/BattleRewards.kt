package com.pokemonarena.domain.entity

import kotlin.math.roundToInt

object BattleRewards {

    private const val WIN_BASE         = 35
    private const val WIN_PER_LEVEL    = 45
    private const val DRAW_PER_LEVEL   = 10
    private const val LOSS_BASE        = 10
    private const val LOSS_PER_LEVEL   = 5
    private const val MIN_FAIRNESS     = 0.4f
    const val FIRST_WIN_BONUS          = 1000

    fun coinsFor(winner: Winner, playerScore: Float, botScore: Float, gymDifficulty: Int): Int =
        when (winner) {
            Winner.PLAYER -> {
                val base     = WIN_BASE + WIN_PER_LEVEL * gymDifficulty
                val ratio    = if (playerScore <= 0f) 1f else (botScore / playerScore).coerceIn(0f, 1f)
                val fairness = MIN_FAIRNESS + (1f - MIN_FAIRNESS) * ratio
                (base * fairness).roundToInt()
            }
            Winner.DRAW   -> DRAW_PER_LEVEL * gymDifficulty
            Winner.BOT    -> -(LOSS_BASE + LOSS_PER_LEVEL * gymDifficulty)
        }

    fun maxRewardFor(gymDifficulty: Int): Int = WIN_BASE + WIN_PER_LEVEL * gymDifficulty
}
