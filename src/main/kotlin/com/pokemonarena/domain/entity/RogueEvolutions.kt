package com.pokemonarena.domain.entity

object RogueEvolutions {

    private val chain: Map<Int, Pair<Int, Int>> = mapOf(
        16 to (17 to 12), 17 to (18 to 22),
        43 to (44 to 12), 44 to (45 to 22),
        60 to (61 to 12), 61 to (62 to 22),
        63 to (64 to 12), 64 to (65 to 22),
        66 to (67 to 12), 67 to (68 to 22),
        74 to (75 to 12), 75 to (76 to 22),
        92 to (93 to 12), 93 to (94 to 22),
        81 to (82 to 14),
        58 to (59 to 18),
        120 to (121 to 18),
        2  to (3 to 22),
        5  to (6 to 22),
        8  to (9 to 22)
    )

    fun nextStage(pokeId: Int, level: Int): Int? =
        chain[pokeId]?.takeIf { level >= it.second }?.first

    fun nextStageAny(pokeId: Int): Int? = chain[pokeId]?.first
}
