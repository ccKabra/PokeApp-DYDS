package com.pokemonarena.data.repository

import com.pokemonarena.data.external.WeatherSource
import com.pokemonarena.data.external.WeatherResponse
import com.pokemonarena.data.external.CurrentWeatherDto
import com.pokemonarena.data.local.dao.BattleHistoryDao
import com.pokemonarena.data.local.dao.UserStatisticsDao
import com.pokemonarena.domain.entity.*
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeatherRepositoryImplTest {

    private val source = mockk<WeatherSource>()
    private val repo   = WeatherRepositoryImpl(source)

    @Test
    fun `getWeatherCondition_withCode0_returnsSunny`() = runTest {
        coEvery { source.getCurrentWeather(any(), any()) } returns
            WeatherResponse(CurrentWeatherDto(weathercode = 0, temperature = 25.0))
        assertEquals(WeatherCondition.SUNNY, repo.getWeatherCondition(0.0, 0.0))
    }

    @Test
    fun `getWeatherCondition_withCode61_returnsRain`() = runTest {
        coEvery { source.getCurrentWeather(any(), any()) } returns
            WeatherResponse(CurrentWeatherDto(weathercode = 61, temperature = 5.0))
        assertEquals(WeatherCondition.RAIN, repo.getWeatherCondition(0.0, 0.0))
    }

    @Test
    fun `getWeatherCondition_withCode65_returnsStorm`() = runTest {
        coEvery { source.getCurrentWeather(any(), any()) } returns
            WeatherResponse(CurrentWeatherDto(weathercode = 65, temperature = 5.0))
        assertEquals(WeatherCondition.STORM, repo.getWeatherCondition(0.0, 0.0))
    }

    @Test
    fun `getWeatherCondition_whenSourceFails_returnsClr`() = runTest {
        coEvery { source.getCurrentWeather(any(), any()) } throws RuntimeException("Network error")
        assertEquals(WeatherCondition.CLEAR, repo.getWeatherCondition(0.0, 0.0))
    }

    @Test
    fun `getWeatherCondition_withCode999_returnsClear`() = runTest {
        coEvery { source.getCurrentWeather(any(), any()) } returns
            WeatherResponse(CurrentWeatherDto(weathercode = 999, temperature = 20.0))
        assertEquals(WeatherCondition.CLEAR, repo.getWeatherCondition(0.0, 0.0))
    }

    @Test
    fun `getWeatherCondition_passesCoordinates`() = runTest {
        coEvery { source.getCurrentWeather(48.8, 2.3) } returns
            WeatherResponse(CurrentWeatherDto(weathercode = 0, temperature = 20.0))
        repo.getWeatherCondition(48.8, 2.3)
        coVerify { source.getCurrentWeather(48.8, 2.3) }
    }
}

class BattleRepositoryImplTest {

    private val historyDao = mockk<BattleHistoryDao>(relaxed = true)
    private val statsDao   = mockk<UserStatisticsDao>(relaxed = true)
    private val repo       = BattleRepositoryImpl(historyDao, statsDao)

    @Test
    fun `getUserStatistics_withNoData_returnsDefaultStats`() = runTest {
        every { statsDao.observe() } returns flowOf(null)
        val stats = repo.getUserStatistics().first()
        assertEquals(UserStatistics(), stats)
    }

    @Test
    fun `saveBattleResult_callsHistoryDaoInsert`() = runTest {
        val result = TestFixtures.playerWin
        repo.saveBattleResult(result)
        coVerify { historyDao.insert(any()) }
    }
}

private object TestFixtures {
    val playerWin = BattleResult(
        winner = Winner.PLAYER,
        playerCard = stubCard("bulbasaur"), botCard = stubCard("charmander"),
        playerCards = emptyList(), botCards = emptyList(),
        playerScore = 0.8f, botScore = 0.5f,
        weatherCondition = WeatherCondition.CLEAR, gymName = "TestGym", date = ""
    )

    private fun stubCard(name: String): Card {
        val detail = PokemonDetail(0, name, "", emptyList(), 0, 0, Stats(0,0,0,0,0,0), emptyList())
        return Card("", name, "", "", null, "", detail)
    }
}
