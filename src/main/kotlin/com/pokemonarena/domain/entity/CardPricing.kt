package com.pokemonarena.domain.entity

import kotlin.math.roundToInt

object CardPricing {

    private const val PRICE_PER_STAT = 1.0f
    private const val MIN_PRICE      = 100
    private const val SELL_RATIO     = 0.5f
    private const val MIN_SELL_VALUE = 50

    fun priceOf(card: Card): Int =
        roundToFives(card.stats.total * PRICE_PER_STAT * rarityMultiplier(card.rarity))
            .coerceAtLeast(MIN_PRICE)

    fun sellValueOf(card: Card): Int =
        roundToFives(priceOf(card) * SELL_RATIO * BattleFatigue.multiplierFor(card.timesUsed))
            .coerceAtLeast(MIN_SELL_VALUE)

    fun rarityMultiplier(rarity: String?): Float {
        val r = rarity?.lowercase() ?: return 1.0f
        return when {
            listOf("secret", "rainbow", "gold", "hyper", "crown",
                   "special illustration", "three star").any { it in r }       -> 2.6f
            listOf("ultra", "vmax", "vstar", "illustration",
                   "two star", "shiny").any { it in r }                        -> 2.2f
            listOf("gx", "ex", " v", "holo v", "legend", "radiant",
                   "double rare", "lv.x", "one star").any { it in r }          -> 2.0f
            listOf("holo", "four diamond").any { it in r }                     -> 1.6f
            "promo" in r                                                       -> 1.5f
            listOf("rare", "three diamond").any { it in r }                    -> 1.35f
            listOf("uncommon", "two diamond").any { it in r }                  -> 1.15f
            else                                                               -> 1.0f
        }
    }

    private fun roundToFives(value: Float): Int = (value / 5f).roundToInt() * 5
}
