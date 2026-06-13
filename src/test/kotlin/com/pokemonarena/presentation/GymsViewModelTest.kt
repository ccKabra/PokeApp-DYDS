package com.pokemonarena.presentation

import com.pokemonarena.TestFixtures
import com.pokemonarena.domain.entity.Gym
import com.pokemonarena.domain.entity.WeatherCondition
import com.pokemonarena.domain.entity.Region
import com.pokemonarena.domain.usecase.GetGymsUseCase
import com.pokemonarena.domain.usecase.GetRegionProgressUseCase
import com.pokemonarena.domain.usecase.GetTeamUseCase
import com.pokemonarena.domain.usecase.GetWeatherConditionUseCase
import com.pokemonarena.presentation.screens.gyms.GymsUiEvent
import com.pokemonarena.presentation.screens.gyms.GymsViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GymsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getGyms    = mockk<GetGymsUseCase>()
    private val getWeather = mockk<GetWeatherConditionUseCase>()
    private val getTeam    = mockk<GetTeamUseCase>()
    private val getProgress = mockk<GetRegionProgressUseCase> {
        coEvery { execute() } returns GetRegionProgressUseCase.Progress(
            setOf(Region.KANTO), Region.KANTO.maxPokedexId, emptySet())
    }

    private val gymA = Gym(
        "Pewter", "Cairo", 30.0, 31.2, "rock",
        listOf(TestFixtures.fireCard, TestFixtures.waterCard, TestFixtures.grassCard)
    )

    private val gymB = Gym(
        "Cerulean", "Venice", 45.4, 12.3, "water",
        listOf(TestFixtures.fireCard, TestFixtures.waterCard, TestFixtures.grassCard)
    )

    @BeforeTest
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = GymsViewModel(getGyms, getWeather, getTeam, getProgress)

    @Test
    fun `onEvent_load_populatesGymsAndTeamSize`() = runTest {
        coEvery { getGyms.execute() } returns listOf(gymA, gymB)
        coEvery { getWeather.execute(any(), any()) } returns WeatherCondition.SUNNY
        every   { getTeam.execute() } returns flowOf(listOf(TestFixtures.fireCard))

        val viewModel = vm()
        viewModel.onEvent(GymsUiEvent.Load(Region.KANTO))

        assertEquals(2, viewModel.uiState.value.gyms.size)
        assertEquals(1, viewModel.uiState.value.teamSize)
    }

    @Test
    fun `onEvent_load_attachesWeatherToEachGym`() = runTest {
        coEvery { getGyms.execute() } returns listOf(gymA)
        coEvery { getWeather.execute(any(), any()) } returns WeatherCondition.RAIN
        every   { getTeam.execute() } returns flowOf(emptyList())

        val viewModel = vm()
        viewModel.onEvent(GymsUiEvent.Load(Region.KANTO))

        assertNotNull(viewModel.uiState.value.gyms.first().weather)
        assertEquals(WeatherCondition.RAIN, viewModel.uiState.value.gyms.first().weather)
    }

    @Test
    fun `onEvent_load_whenGymsFail_setsError`() = runTest {
        coEvery { getGyms.execute() } throws RuntimeException("API down")
        every   { getTeam.execute() } returns flowOf(emptyList())

        val viewModel = vm()
        viewModel.onEvent(GymsUiEvent.Load(Region.KANTO))

        assertTrue(viewModel.uiState.value.error != null)
    }
}
