package com.pokemonarena.data.repository

import com.pokemonarena.data.external.WeatherCodeMapper
import com.pokemonarena.data.external.WeatherSource
import com.pokemonarena.domain.entity.WeatherCondition
import com.pokemonarena.domain.repository.WeatherRepository

class WeatherRepositoryImpl(private val source: WeatherSource) : WeatherRepository {
    override suspend fun getWeatherCondition(lat: Double, lon: Double): WeatherCondition =
        runCatching {
            val r = source.getCurrentWeather(lat, lon)
            WeatherCodeMapper.fromCode(r.currentWeather.weathercode, r.currentWeather.temperature)
        }.getOrDefault(WeatherCondition.CLEAR)
}
