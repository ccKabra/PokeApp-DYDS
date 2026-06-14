package com.pokemonarena.domain.entity

import kotlin.random.Random

sealed interface RogueEffect {
    data class Loot(val delta: Int) : RogueEffect
    data class Heal(val fraction: Float) : RogueEffect
    data class Hurt(val fraction: Float) : RogueEffect
    data class MaxHp(val factor: Float) : RogueEffect
    data class Levels(val count: Int) : RogueEffect
    object Evolve : RogueEffect
    object GiveItem : RogueEffect
    object Fight : RogueEffect
    data class Chance(val prob: Float, val onWin: List<RogueEffect>, val onLose: List<RogueEffect>) : RogueEffect
    data class CostLoot(val amount: Int, val then: List<RogueEffect>) : RogueEffect
}

data class RogueEventOption(
    val label:       String,
    val description: String,
    val effects:     List<RogueEffect>
)

data class RogueEvent(
    val id:        String,
    val title:     String,
    val narrative: String,
    val options:   List<RogueEventOption>
)

object RogueEvents {

    val ALL: List<RogueEvent> = listOf(
        RogueEvent("entrenador", "Entrenador Rival",
            "Un entrenador te corta el camino con una sonrisa desafiante.",
            listOf(
                RogueEventOption("Pelear", "Combatís: si ganás, te llevás su oro.",
                    listOf(RogueEffect.Fight)),
                RogueEventOption("Pagar", "Le das 80 de oro y te deja pasar.",
                    listOf(RogueEffect.Loot(-80))),
                RogueEventOption("Intimidar", "50% se va asustado · 50% no le creés nada y peleás.",
                    listOf(RogueEffect.Chance(0.5f, emptyList(), listOf(RogueEffect.Fight))))
            )),
        RogueEvent("altar", "Altar Antiguo",
            "Un altar de piedra emana una energía expectante.",
            listOf(
                RogueEventOption("Ofrecer entrenamiento", "Un Pokémon sube de nivel.",
                    listOf(RogueEffect.Levels(1))),
                RogueEventOption("Ofrecer vida", "−20% PS actual, pero +10% PS máximo permanente.",
                    listOf(RogueEffect.Hurt(0.2f), RogueEffect.MaxHp(1.1f))),
                RogueEventOption("Rezar", "30% cura total · 70% una maldición te daña.",
                    listOf(RogueEffect.Chance(0.3f,
                        listOf(RogueEffect.Heal(1f)),
                        listOf(RogueEffect.Hurt(0.25f)))))
            )),
        RogueEvent("comerciante", "Comerciante Errante",
            "Un mercader despliega su manta llena de objetos curiosos.",
            listOf(
                RogueEventOption("Comprar caro (200)", "Un objeto raro para tu equipo.",
                    listOf(RogueEffect.CostLoot(200, listOf(RogueEffect.GiveItem)))),
                RogueEventOption("Comprar barato (50)", "Una poción que cura al equipo.",
                    listOf(RogueEffect.CostLoot(50, listOf(RogueEffect.Heal(0.4f))))),
                RogueEventOption("Robar", "60% te llevás un objeto gratis · 40% pierdes 150 de oro.",
                    listOf(RogueEffect.Chance(0.6f,
                        listOf(RogueEffect.GiveItem),
                        listOf(RogueEffect.Loot(-150)))))
            )),
        RogueEvent("manantial", "Manantial Escondido",
            "Un manantial cristalino brilla entre las rocas.",
            listOf(
                RogueEventOption("Beber", "Cura 30% al equipo… pero aparece un combate.",
                    listOf(RogueEffect.Heal(0.3f), RogueEffect.Fight)),
                RogueEventOption("Llenar la cantimplora", "Cura tranquila del 20% al equipo.",
                    listOf(RogueEffect.Heal(0.2f))),
                RogueEventOption("Explorar la zona", "Encontrás algo de oro escondido.",
                    listOf(RogueEffect.Loot(30)))
            )),
        RogueEvent("cueva", "Cueva Brillante",
            "Cristales fosforescentes laten en la oscuridad.",
            listOf(
                RogueEventOption("Excavar", "50% un filón de oro · 50% una roca te lastima un poco.",
                    listOf(RogueEffect.Chance(0.5f,
                        listOf(RogueEffect.Loot(120)),
                        listOf(RogueEffect.Hurt(0.15f))))),
                RogueEventOption("Meditar", "Descansás y curás 50% al equipo.",
                    listOf(RogueEffect.Heal(0.5f))),
                RogueEventOption("Tocar el cristal", "40% te entrega un objeto · 60% se apaga y pierdes 60 de oro.",
                    listOf(RogueEffect.Chance(0.4f,
                        listOf(RogueEffect.GiveItem),
                        listOf(RogueEffect.Loot(-60)))))
            )),
        RogueEvent("reliquia", "Reliquia Evolutiva",
            "Una piedra antigua late con una energía evolutiva difícil de ignorar.",
            listOf(
                RogueEventOption("Usar la reliquia", "Tus Pokémon que puedan evolucionar, lo hacen ahora.",
                    listOf(RogueEffect.Evolve)),
                RogueEventOption("Venderla", "Un coleccionista paga muy bien por ella.",
                    listOf(RogueEffect.Loot(120))),
                RogueEventOption("Absorber su energía", "Su poder cura a tu equipo.",
                    listOf(RogueEffect.Heal(0.4f)))
            )),
        RogueEvent("sabio", "Viejo Sabio",
            "Un anciano te observa desde la sombra de un árbol.",
            listOf(
                RogueEventOption("Escuchar su consejo", "Su sabiduría hace madurar a un Pokémon.",
                    listOf(RogueEffect.Levels(1))),
                RogueEventOption("Pedirle oro", "Te comparte parte de sus ahorros.",
                    listOf(RogueEffect.Loot(90))),
                RogueEventOption("Seguir camino", "Te despide con un té que reanima al equipo.",
                    listOf(RogueEffect.Heal(0.15f)))
            ))
    )

    fun random(random: Random): RogueEvent = ALL.random(random)
}
