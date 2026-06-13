package com.pokemonarena.domain.entity

object BattleScore {

    const val CRIT_MULTIPLIER = 1.5f

    fun weightedOf(stats: Stats): Float =
        (stats.attack * 0.25f + stats.defense * 0.20f + stats.speed * 0.20f +
         stats.hp * 0.15f + stats.specialAttack * 0.10f + stats.specialDefense * 0.10f) / 255f
}

data class ScoreBreakdown(
    val statsBase:         Float,
    val itemMultiplier:    Float,
    val rarityMultiplier:  Float,
    val fatigueMultiplier: Float,
    val weatherMultiplier: Float,
    val typeMultiplier:    Float,
    val critMultiplier:    Float,
    val missed:            Boolean
) {
    companion object {
        fun of(card: Card, weather: WeatherCondition, typeMultiplier: Float,
               crit: Boolean, missed: Boolean): ScoreBreakdown {
            val base     = BattleScore.weightedOf(card.stats)
            val withItem = card.heldItem?.boosts?.applyTo(card.stats) ?: card.stats
            return ScoreBreakdown(
                statsBase         = base,
                itemMultiplier    = if (base == 0f) 1f else BattleScore.weightedOf(withItem) / base,
                rarityMultiplier  = RarityBoost.multiplierFor(card.rarity),
                fatigueMultiplier = BattleFatigue.multiplierFor(card.timesUsed),
                weatherMultiplier = TypeEffectiveness.multiplierFor(card.primaryType, weather) ?: 1f,
                typeMultiplier    = typeMultiplier,
                critMultiplier    = if (crit) BattleScore.CRIT_MULTIPLIER else 1f,
                missed            = missed
            )
        }
    }
}

object RarityBoost {

    private const val SCALE = 0.25f

    fun multiplierFor(rarity: String?): Float =
        1f + (CardPricing.rarityMultiplier(rarity) - 1f) * SCALE
}

object BattleFatigue {

    const val PENALTY_PER_USE = 0.06f
    const val MIN_MULTIPLIER  = 0.50f

    fun multiplierFor(timesUsed: Int): Float =
        (1f - timesUsed * PENALTY_PER_USE).coerceAtLeast(MIN_MULTIPLIER)

    fun penaltyPercent(timesUsed: Int): Int =
        ((1f - multiplierFor(timesUsed)) * 100).toInt()
}
