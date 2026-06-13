package com.pokemonarena.domain.entity

data class RogueMove(val name: String, val type: String, val power: Float)

/**
 * Catálogo de ataques por tipo, ordenados de más débil a más fuerte.
 * El nivel requerido se deriva de la potencia: los golpes fuertes se aprenden más tarde.
 */
object RogueMoves {

    val BASIC = RogueMove("Placaje", "normal", 0.85f)

    private val catalog: Map<String, List<RogueMove>> = mapOf(
        "normal"   to listOf(BASIC,                              RogueMove("Golpe Cuerpo", "normal", 1.15f),  RogueMove("Hiperrayo",    "normal", 1.4f)),
        "fire"     to listOf(RogueMove("Ascuas", "fire", 0.9f),  RogueMove("Lanzallamas", "fire", 1.15f),     RogueMove("Llamarada",    "fire", 1.4f)),
        "water"    to listOf(RogueMove("Pistola Agua","water",0.9f), RogueMove("Surf", "water", 1.15f),       RogueMove("Hidrobomba",   "water", 1.4f)),
        "grass"    to listOf(RogueMove("Látigo Cepa","grass",0.9f),  RogueMove("Hoja Afilada","grass",1.15f), RogueMove("Rayo Solar",   "grass", 1.4f)),
        "electric" to listOf(RogueMove("Impactrueno","electric",0.9f), RogueMove("Rayo","electric",1.15f),    RogueMove("Trueno",       "electric", 1.4f)),
        "ice"      to listOf(RogueMove("Viento Hielo","ice",0.9f),   RogueMove("Rayo Hielo","ice",1.15f),     RogueMove("Ventisca",     "ice", 1.4f)),
        "fighting" to listOf(RogueMove("Golpe Kárate","fighting",0.9f), RogueMove("Sumisión","fighting",1.15f), RogueMove("A Bocajarro","fighting", 1.4f)),
        "poison"   to listOf(RogueMove("Ácido","poison",0.9f),       RogueMove("Bomba Lodo","poison",1.15f),  RogueMove("Lanzamugre",   "poison", 1.4f)),
        "ground"   to listOf(RogueMove("Bofetón Lodo","ground",0.9f), RogueMove("Excavar","ground",1.15f),    RogueMove("Terremoto",    "ground", 1.4f)),
        "flying"   to listOf(RogueMove("Tornado","flying",0.9f),     RogueMove("Acróbata","flying",1.15f),    RogueMove("Pájaro Osado", "flying", 1.4f)),
        "psychic"  to listOf(RogueMove("Confusión","psychic",0.9f),  RogueMove("Psicorrayo","psychic",1.15f), RogueMove("Psíquico",     "psychic", 1.4f)),
        "bug"      to listOf(RogueMove("Picadura","bug",0.9f),       RogueMove("Zumbido","bug",1.15f),        RogueMove("Tijera X",     "bug", 1.4f)),
        "rock"     to listOf(RogueMove("Lanzarrocas","rock",0.9f),   RogueMove("Avalancha","rock",1.15f),     RogueMove("Roca Afilada", "rock", 1.4f)),
        "ghost"    to listOf(RogueMove("Lengüetazo","ghost",0.9f),   RogueMove("Bola Sombra","ghost",1.15f),  RogueMove("Golpe Fantasma","ghost", 1.4f)),
        "dragon"   to listOf(RogueMove("Furia Dragón","dragon",0.9f), RogueMove("Garra Dragón","dragon",1.15f), RogueMove("Enfado",      "dragon", 1.4f)),
        "dark"     to listOf(RogueMove("Mordisco","dark",0.9f),      RogueMove("Pulso Umbrío","dark",1.15f),  RogueMove("Triturar",     "dark", 1.4f)),
        "steel"    to listOf(RogueMove("Garra Metal","steel",0.9f),  RogueMove("Cabeza Hierro","steel",1.15f), RogueMove("Foco Resplandor","steel", 1.4f)),
        "fairy"    to listOf(RogueMove("Viento Feérico","fairy",0.9f), RogueMove("Voz Cautivadora","fairy",1.15f), RogueMove("Fuerza Lunar","fairy", 1.4f))
    )

    fun movesOf(type: String): List<RogueMove> = catalog[type] ?: listOf(BASIC)

    /** Set inicial del jugador: golpe básico + el ataque más débil de su tipo primario. */
    fun starterSet(types: List<String>): List<RogueMove> {
        val set = LinkedHashSet<RogueMove>().apply { add(BASIC) }
        types.forEach { movesOf(it).firstOrNull()?.let(set::add) }
        if (set.size < 2) set.add(movesOf(types.firstOrNull() ?: "normal").getOrElse(1) { BASIC })
        return set.toList()
    }

    /** Los rivales arrancan con un repertorio algo más amplio para que cada pelea se sienta distinta. */
    fun enemySet(types: List<String>): List<RogueMove> {
        val set = LinkedHashSet<RogueMove>().apply { add(BASIC) }
        types.forEach { type -> movesOf(type).take(2).forEach(set::add) }
        return set.toList().take(RogueRules.MOVE_CAP)
    }

    private fun requiredLevel(power: Float): Int = when {
        power >= 1.4f  -> 13
        power >= 1.15f -> 8
        else           -> 1
    }

    /** El mejor ataque de los tipos del Pokémon que aún no conoce y ya puede aprender por nivel. */
    fun nextLearnable(current: List<RogueMove>, types: List<String>, level: Int): RogueMove? =
        (types + "normal").distinct().flatMap { movesOf(it) }.distinct()
            .filter { it !in current && requiredLevel(it.power) <= level }
            .maxByOrNull { it.power }
}
