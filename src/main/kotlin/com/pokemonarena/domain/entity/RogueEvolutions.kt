package com.pokemonarena.domain.entity

/**
 * Cadenas evolutivas del modo Rogue. Al alcanzar el nivel umbral, un Pokémon evoluciona
 * a la siguiente especie del pool: deja de ser "de mierda" y crece de verdad.
 */
object RogueEvolutions {

    // pokeId actual -> (pokeId de la evolución, nivel mínimo para evolucionar)
    private val chain: Map<Int, Pair<Int, Int>> = mapOf(
        16 to (17 to 12), 17 to (18 to 22),   // pidgey -> pidgeotto -> pidgeot
        43 to (44 to 12), 44 to (45 to 22),   // oddish -> gloom -> vileplume
        60 to (61 to 12), 61 to (62 to 22),   // poliwag -> poliwhirl -> poliwrath
        63 to (64 to 12), 64 to (65 to 22),   // abra -> kadabra -> alakazam
        66 to (67 to 12), 67 to (68 to 22),   // machop -> machoke -> machamp
        74 to (75 to 12), 75 to (76 to 22),   // geodude -> graveler -> golem
        92 to (93 to 12), 93 to (94 to 22),   // gastly -> haunter -> gengar
        81 to (82 to 14),                     // magnemite -> magneton
        58 to (59 to 18),                     // growlithe -> arcanine
        120 to (121 to 18),                   // staryu -> starmie
        2  to (3 to 22),                      // ivysaur -> venusaur
        5  to (6 to 22),                      // charmeleon -> charizard
        8  to (9 to 22)                       // wartortle -> blastoise
    )

    /** Devuelve el pokeId de la evolución inmediata si el nivel ya la habilita, o null. */
    fun nextStage(pokeId: Int, level: Int): Int? =
        chain[pokeId]?.takeIf { level >= it.second }?.first
}
