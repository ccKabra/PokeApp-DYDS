package com.pokemonarena.domain.entity

data class RogueUpgrade(
    val id:          String,
    val displayName: String,
    val description: String,
    val baseCost:    Int,
    val maxLevel:    Int
) {
    fun costAt(level: Int): Int = baseCost * (level + 1)
}

object RogueUpgrades {
    val SEED_GOLD = RogueUpgrade("seed_gold", "Alcancía",
        "Empezás cada run con +60 de oro por nivel.", baseCost = 150, maxLevel = 5)
    val VETERAN = RogueUpgrade("veteran", "Veteranía",
        "Tu inicial arranca con +1 nivel por nivel comprado.", baseCost = 200, maxLevel = 5)
    val VITALITY = RogueUpgrade("vitality", "Vitalidad",
        "+8% de HP máximo inicial para tu equipo por nivel.", baseCost = 220, maxLevel = 4)
    val LUCKY = RogueUpgrade("lucky", "Trébol de la Suerte",
        "Empezás cada run con la bendición Fortuna activa.", baseCost = 400, maxLevel = 1)
    val GEAR = RogueUpgrade("gear", "Mochila Preparada",
        "Tu inicial arranca con un objeto equipado al azar.", baseCost = 350, maxLevel = 1)

    val ALL: List<RogueUpgrade> = listOf(SEED_GOLD, VETERAN, VITALITY, LUCKY, GEAR)

    fun byId(id: String): RogueUpgrade? = ALL.firstOrNull { it.id == id }
}

data class RogueMetaState(val levels: Map<String, Int> = emptyMap()) {

    fun levelOf(upgrade: RogueUpgrade): Int = levels[upgrade.id] ?: 0

    val startingGold:       Int     get() = levelOf(RogueUpgrades.SEED_GOLD) * 60
    val starterLevelBonus:  Int     get() = levelOf(RogueUpgrades.VETERAN)
    val startingHpFactor:   Float   get() = 1f + 0.08f * levelOf(RogueUpgrades.VITALITY)
    val startsWithFortune:  Boolean get() = levelOf(RogueUpgrades.LUCKY) > 0
    val startsWithGear:     Boolean get() = levelOf(RogueUpgrades.GEAR) > 0
}
