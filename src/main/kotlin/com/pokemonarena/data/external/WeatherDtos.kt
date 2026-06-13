package com.pokemonarena.data.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    @SerialName("current_weather") val currentWeather: CurrentWeatherDto
)

@Serializable
data class CurrentWeatherDto(val weathercode: Int, val temperature: Double)
