package com.pokemonarena.domain.entity

/** Equipo del modo Rogue: se equipa a UN Pokémon y potencia sus stats para sobrevivir más. */
data class RogueItem(
    val id:          String,
    val name:        String,
    val description: String,
    val attackMult:  Float = 1f,
    val defenseMult: Float = 1f,
    val speedMult:   Float = 1f,
    val hpMult:      Float = 1f
)

object RogueItems {
    val ALL: List<RogueItem> = listOf(
        RogueItem("brazal", "Brazal de Hierro",
                  "+40% Defensa. Para aguantar la paliza con dignidad.", defenseMult = 1.4f),
        RogueItem("cinta", "Cinta Fuerte",
                  "+35% Ataque. No vas a ganar, pero vas a doler.", attackMult = 1.35f),
        RogueItem("pluma", "Pluma Veloz",
                  "+30% Velocidad. Pegás primero… y huís antes.", speedMult = 1.3f),
        RogueItem("caparazon", "Caparazón Ancestral",
                  "+50% HP máximo. Más carne para el sacrificio.", hpMult = 1.5f),
        RogueItem("totem", "Tótem del Equilibrio",
                  "+15% en todo. Hace de todo un poco, como vos.",
                  attackMult = 1.15f, defenseMult = 1.15f, speedMult = 1.15f, hpMult = 1.15f),
        RogueItem("amuleto", "Amuleto del Cobarde",
                  "+25% Defensa y Velocidad. Diseñado para escapar con vida.",
                  defenseMult = 1.25f, speedMult = 1.25f),
        RogueItem("chaleco", "Chaleco Acolchado",
                  "+30% HP y +20% Defensa. El kit del que planea sobrevivir.",
                  defenseMult = 1.2f, hpMult = 1.3f)
    )

    fun random(random: kotlin.random.Random): RogueItem = ALL.random(random)
}
