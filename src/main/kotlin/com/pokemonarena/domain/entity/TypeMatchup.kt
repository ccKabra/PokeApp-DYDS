package com.pokemonarena.domain.entity

object TypeMatchup {

    const val ADVANTAGE    = 1.25f
    const val DISADVANTAGE = 0.8f
    const val NEUTRAL      = 1.0f

    private val STRONG_AGAINST: Map<String, List<String>> = mapOf(
        "fire"     to listOf("grass", "ice", "bug", "steel"),
        "water"    to listOf("fire", "rock", "ground"),
        "grass"    to listOf("water", "rock", "ground"),
        "electric" to listOf("water", "flying"),
        "ice"      to listOf("grass", "ground", "flying", "dragon"),
        "fighting" to listOf("normal", "rock", "steel", "ice", "dark"),
        "poison"   to listOf("grass", "fairy"),
        "ground"   to listOf("fire", "electric", "poison", "rock", "steel"),
        "flying"   to listOf("grass", "fighting", "bug"),
        "psychic"  to listOf("fighting", "poison"),
        "bug"      to listOf("grass", "psychic", "dark"),
        "rock"     to listOf("fire", "ice", "flying", "bug"),
        "ghost"    to listOf("psychic", "ghost"),
        "dragon"   to listOf("dragon"),
        "dark"     to listOf("psychic", "ghost"),
        "steel"    to listOf("ice", "rock", "fairy"),
        "fairy"    to listOf("fighting", "dragon", "dark")
    )

    private val WEAK_AGAINST: Map<String, List<String>> = mapOf(
        "fire"     to listOf("water", "rock", "fire", "dragon"),
        "water"    to listOf("grass", "water", "dragon"),
        "grass"    to listOf("fire", "grass", "poison", "flying", "bug", "dragon", "steel"),
        "electric" to listOf("grass", "electric", "dragon", "ground"),
        "ice"      to listOf("fire", "water", "ice", "steel"),
        "fighting" to listOf("poison", "flying", "psychic", "bug", "fairy", "ghost"),
        "poison"   to listOf("poison", "ground", "rock", "ghost", "steel"),
        "ground"   to listOf("grass", "bug", "flying"),
        "flying"   to listOf("electric", "rock", "steel"),
        "psychic"  to listOf("psychic", "steel", "dark"),
        "bug"      to listOf("fire", "fighting", "poison", "flying", "ghost", "steel", "fairy"),
        "rock"     to listOf("fighting", "ground", "steel"),
        "ghost"    to listOf("dark", "normal"),
        "dragon"   to listOf("steel", "fairy"),
        "dark"     to listOf("fighting", "dark", "fairy"),
        "steel"    to listOf("fire", "water", "electric", "steel"),
        "fairy"    to listOf("fire", "poison", "steel"),
        "normal"   to listOf("rock", "steel", "ghost")
    )

    fun multiplier(attackerType: String, defenderType: String): Float = when {
        STRONG_AGAINST[attackerType]?.contains(defenderType) == true -> ADVANTAGE
        WEAK_AGAINST[attackerType]?.contains(defenderType)   == true -> DISADVANTAGE
        else                                                         -> NEUTRAL
    }

    const val SUPER_EFFECTIVE = 2.0f
    const val NOT_EFFECTIVE   = 0.5f

    fun effectiveness(attackerType: String, defenderType: String): Float = when {
        STRONG_AGAINST[attackerType]?.contains(defenderType) == true -> SUPER_EFFECTIVE
        WEAK_AGAINST[attackerType]?.contains(defenderType)   == true -> NOT_EFFECTIVE
        else                                                         -> NEUTRAL
    }
}
