package com.pokemonarena.data.external

import com.pokemonarena.core.Constants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class WeatherSource(private val client: HttpClient) {

    suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherResponse =
        client.get("${Constants.OPEN_METEO_BASE_URL}/forecast") {
            parameter("latitude",        lat)
            parameter("longitude",       lon)
            parameter("current_weather", true)
        }.body()
}
