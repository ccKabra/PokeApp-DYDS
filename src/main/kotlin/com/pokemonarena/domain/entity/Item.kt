package com.pokemonarena.domain.entity

import kotlin.math.roundToInt

data class StatBoosts(
    val attack:         Float = 1f,
    val defense:        Float = 1f,
    val specialAttack:  Float = 1f,
    val specialDefense: Float = 1f,
    val speed:          Float = 1f
) {
    fun applyTo(stats: Stats): Stats = stats.copy(
        attack         = (stats.attack         * attack).roundToInt(),
        defense        = (stats.defense        * defense).roundToInt(),
        specialAttack  = (stats.specialAttack  * specialAttack).roundToInt(),
        specialDefense = (stats.specialDefense * specialDefense).roundToInt(),
        speed          = (stats.speed          * speed).roundToInt()
    )
}

data class Item(
    val id:            String,
    val name:          String,
    val description:   String,
    val price:         Int,
    val imageUrl:      String,
    val boosts:        StatBoosts,
    val missReduction: Float = 0f
)

object ItemCatalog {

    private fun spriteUrl(id: String) =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/$id.png"

    const val ENERGY_ROOT_ID = "energy-root"

    val EXCLUSIVES: List<Item> = listOf(
        Item("expert-belt", "Cinto Experto",
             "Recompensa exclusiva de gimnasio: potencia TODAS las stats de combate.",
             0, spriteUrl("expert-belt"), StatBoosts(1.25f, 1.25f, 1.25f, 1.25f, 1.25f)),
        Item("muscle-band", "Cinta Fuerte",
             "Recompensa exclusiva de gimnasio: un Ataque físico descomunal.",
             0, spriteUrl("muscle-band"), StatBoosts(attack = 1.8f)),
        Item("zoom-lens", "Telescopio",
             "Recompensa exclusiva de gimnasio: puntería casi perfecta y mejor Ataque Especial.",
             0, spriteUrl("zoom-lens"), StatBoosts(specialAttack = 1.3f), missReduction = 0.15f)
    )

    val CONSUMABLES: List<Item> = listOf(
        Item(ENERGY_ROOT_ID, "Raíz Energía",
             "Amarga pero efectiva: elimina toda la fatiga de un Pokémon (reinicia su contador de combates).",
             600, spriteUrl(ENERGY_ROOT_ID), StatBoosts())
    )

    val ALL: List<Item> = listOf(
        Item("choice-band",  "Cinta Elección",    "Un clásico: potencia muchísimo el Ataque físico.",
             450, spriteUrl("choice-band"),  StatBoosts(attack = 1.5f)),
        Item("choice-specs", "Gafas Elección",    "Anteojos que disparan el Ataque Especial.",
             450, spriteUrl("choice-specs"), StatBoosts(specialAttack = 1.5f)),
        Item("choice-scarf", "Pañuelo Elección",  "Una bufanda liviana que aumenta mucho la Velocidad.",
             450, spriteUrl("choice-scarf"), StatBoosts(speed = 1.5f)),
        Item("assault-vest", "Chaleco Asalto",    "Un chaleco táctico que refuerza la Defensa Especial.",
             350, spriteUrl("assault-vest"), StatBoosts(specialDefense = 1.5f)),
        Item("eviolite",     "Mineral Evolutivo", "Un mineral extraño que endurece ambas defensas.",
             350, spriteUrl("eviolite"),     StatBoosts(defense = 1.3f, specialDefense = 1.3f)),
        Item("life-orb",     "Vidasfera",         "Una esfera misteriosa que potencia ambos ataques.",
             550, spriteUrl("life-orb"),     StatBoosts(attack = 1.3f, specialAttack = 1.3f)),
        Item("wide-lens",    "Lupa",              "Una lente que afina la puntería: reduce un poco la chance de fallar el ataque.",
             500, spriteUrl("wide-lens"),    StatBoosts(), missReduction = 0.05f)
    )

    fun byId(id: String): Item? = (ALL + CONSUMABLES + EXCLUSIVES).firstOrNull { it.id == id }

    fun isConsumable(id: String): Boolean = CONSUMABLES.any { it.id == id }

    fun isExclusive(id: String): Boolean = EXCLUSIVES.any { it.id == id }
}
