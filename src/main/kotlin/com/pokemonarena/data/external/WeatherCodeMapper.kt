package com.pokemonarena.data.external

import com.pokemonarena.domain.entity.WeatherCondition

object WeatherCodeMapper {
    fun fromCode(code: Int, tempC: Double = 20.0): WeatherCondition = when {
        code == 0 && tempC >= 35 -> WeatherCondition.EXTREME_SUN
        code == 0                -> WeatherCondition.SUNNY
        code in 1..3             -> WeatherCondition.CLEAR
        code in 45..48           -> WeatherCondition.FOG
        code in 51..63           -> WeatherCondition.RAIN
        code in 64..67           -> WeatherCondition.STORM
        code in 71..77           -> WeatherCondition.SNOW
        code in 80..82           -> WeatherCondition.RAIN
        code in 85..86           -> WeatherCondition.SNOW
        code in 95..99           -> WeatherCondition.STORM
        else                     -> WeatherCondition.CLEAR
    }
}
