package com.pokemonarena.domain.entity

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
                  "+40% Defensa. Aguanta los golpes más duros.", defenseMult = 1.4f),
        RogueItem("cinta", "Cinta Fuerte",
                  "+35% Ataque. Para pegar todavía más fuerte.", attackMult = 1.35f),
        RogueItem("pluma", "Pluma Veloz",
                  "+30% Velocidad. Casi siempre golpeás primero.", speedMult = 1.3f),
        RogueItem("caparazon", "Caparazón Ancestral",
                  "+50% HP máximo. Un muro difícil de tumbar.", hpMult = 1.5f),
        RogueItem("totem", "Tótem del Equilibrio",
                  "+15% en todas las stats. Mejora pareja y confiable.",
                  attackMult = 1.15f, defenseMult = 1.15f, speedMult = 1.15f, hpMult = 1.15f),
        RogueItem("amuleto", "Amuleto del Vigía",
                  "+25% Defensa y Velocidad. Reacciona rápido y resiste.",
                  defenseMult = 1.25f, speedMult = 1.25f),
        RogueItem("chaleco", "Chaleco Acolchado",
                  "+30% HP y +20% Defensa. El kit del tanque.",
                  defenseMult = 1.2f, hpMult = 1.3f)
    )

    fun random(random: kotlin.random.Random): RogueItem = ALL.random(random)
}
