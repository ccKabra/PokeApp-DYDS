package com.pokemonarena.domain.entity

import kotlin.test.Test
import kotlin.test.assertTrue

class WeatherConditionTest {

    @Test
    fun `boostedTypes_areCorrectForEachCondition`() {
        assertTrue(WeatherCondition.SUNNY.boostedTypes.contains("fire"))
        assertTrue(WeatherCondition.RAIN.boostedTypes.contains("water"))
        assertTrue(WeatherCondition.STORM.boostedTypes.contains("electric"))
        assertTrue(WeatherCondition.SNOW.boostedTypes.contains("ice"))
        assertTrue(WeatherCondition.CLEAR.boostedTypes.isEmpty())
    }

    @Test
    fun `multiplier_isBiggerThan1_forAllBoostedConditions`() {
        val boostedConditions = listOf(
            WeatherCondition.SUNNY, WeatherCondition.EXTREME_SUN,
            WeatherCondition.RAIN, WeatherCondition.STORM,
            WeatherCondition.SNOW, WeatherCondition.SANDSTORM, WeatherCondition.FOG
        )
        boostedConditions.forEach { condition ->
            assertTrue(condition.multiplier > 1.0f,
                "Expected multiplier > 1.0 for ${condition.name} but was ${condition.multiplier}")
        }
    }
}
