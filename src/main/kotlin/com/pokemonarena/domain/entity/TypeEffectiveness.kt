package com.pokemonarena.domain.entity

object TypeEffectiveness {

    private val MULTIPLIERS: Map<WeatherCondition, Map<String, Float>> = mapOf(
        WeatherCondition.SUNNY        to mapOf("fire" to 1.5f, "grass" to 1.1f, "water" to 0.8f),
        WeatherCondition.EXTREME_SUN  to mapOf("fire" to 2.0f, "water" to 0.5f, "grass" to 0.75f),
        WeatherCondition.RAIN         to mapOf("water" to 1.5f, "electric" to 1.1f, "fire" to 0.8f),
        WeatherCondition.STORM        to mapOf("electric" to 1.5f, "dragon" to 1.25f, "fire" to 0.6f),
        WeatherCondition.SNOW         to mapOf("ice" to 1.5f, "water" to 1.1f, "fire" to 0.75f),
        WeatherCondition.SANDSTORM    to mapOf("rock" to 1.5f, "ground" to 1.25f, "steel" to 1.1f),
        WeatherCondition.FOG          to mapOf("ghost" to 1.3f, "psychic" to 1.2f, "dark" to 1.1f),
        WeatherCondition.CLEAR        to emptyMap()
    )

    fun multiplierFor(primaryType: String, weather: WeatherCondition): Float? =
        MULTIPLIERS[weather]?.get(primaryType)
}
