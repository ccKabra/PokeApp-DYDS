package com.pokemonarena.domain.entity

enum class MiningTier(val displayName: String) {
    NOTHING("La roca no soltó nada"),
    COMMON("Polvo de moneda"),
    NICE("Buen golpe"),
    GREAT("¡Gran pepita!"),
    EPIC("¡Veta épica!"),
    JACKPOT("¡¡JACKPOT!!")
}

data class MiningReward(val coins: Int, val tier: MiningTier)

data class MiningOdds(val tier: MiningTier, val coins: Int, val chance: Float, val chanceLabel: String)

object CoinMine {

    val ODDS = listOf(
        MiningOdds(MiningTier.JACKPOT, 100, 0.005f, "0.5%"),
        MiningOdds(MiningTier.EPIC,     25, 0.025f, "2.5%"),
        MiningOdds(MiningTier.GREAT,    10, 0.07f,  "7%"),
        MiningOdds(MiningTier.NICE,      3, 0.15f,  "15%"),
        MiningOdds(MiningTier.COMMON,    1, 0.20f,  "20%"),
        MiningOdds(MiningTier.NOTHING,   0, 0.55f,  "55%")
    )

    fun rewardFor(roll: Float): MiningReward {
        var cumulative = 0f
        for (odds in ODDS) {
            cumulative += odds.chance
            if (roll < cumulative) return MiningReward(odds.coins, odds.tier)
        }
        return MiningReward(0, MiningTier.NOTHING)
    }
}
