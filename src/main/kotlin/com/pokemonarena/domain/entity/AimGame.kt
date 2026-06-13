package com.pokemonarena.domain.entity

import kotlin.math.roundToInt

object AimGame {
    const val BALLOON_COUNT  = 3
    const val LIFETIME_MS    = 3_000L
    const val MISS_PENALTY   = 2
    const val MIN_HIT_REWARD = 2
    const val MAX_HIT_REWARD = 12

    /** [sizeFraction]: 0 = globo más chico (paga más), 1 = globo más grande (paga menos). */
    fun hitRewardFor(sizeFraction: Float): Int {
        val fraction = sizeFraction.coerceIn(0f, 1f)
        return (MAX_HIT_REWARD - (MAX_HIT_REWARD - MIN_HIT_REWARD) * fraction).roundToInt()
    }
}
