package com.pokemonarena.domain.usecase

import com.pokemonarena.TestFixtures
import com.pokemonarena.domain.entity.*
import com.pokemonarena.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetPokemonsUseCaseTest {

    private val repo    = mockk<PokemonRepository>()
    private val useCase = GetPokemonsUseCaseImpl(repo)

    @Test
    fun `invoke_withDefaultParams_returnsListFromRepository`() = runTest {
        coEvery { repo.getPokemonList(any(), any()) } returns TestFixtures.pokemonList
        val result = useCase.execute()
        assertEquals(TestFixtures.pokemonList, result)
    }

    @Test
    fun `invoke_withCustomParams_passesThemToRepository`() = runTest {
        coEvery { repo.getPokemonList(50, 100) } returns emptyList()
        useCase.execute(limit = 50, offset = 100)
        coVerify { repo.getPokemonList(50, 100) }
    }

    @Test
    fun `invoke_whenRepositoryReturnsEmpty_returnsEmptyList`() = runTest {
        coEvery { repo.getPokemonList(any(), any()) } returns emptyList()
        assertTrue(useCase.execute().isEmpty())
    }

    @Test
    fun `invoke_whenRepositoryThrows_propagatesException`() = runTest {
        coEvery { repo.getPokemonList(any(), any()) } throws RuntimeException("Network error")
        val result = runCatching { useCase.execute() }
        assertTrue(result.isFailure)
    }
}

class GetPokemonDetailUseCaseTest {

    private val repo    = mockk<PokemonRepository>()
    private val useCase = GetPokemonDetailUseCaseImpl(repo)

    @Test
    fun `invoke_returnsDetailFromRepository`() = runTest {
        coEvery { repo.getPokemonDetail("pikachu") } returns TestFixtures.fireDetail
        val result = useCase.execute("pikachu")
        assertEquals(TestFixtures.fireDetail, result)
    }

    @Test
    fun `invoke_whenNotFound_propagatesException`() = runTest {
        coEvery { repo.getPokemonDetail(any()) } throws RuntimeException("404")
        assertTrue(runCatching { useCase.execute("fake") }.isFailure)
    }
}

class GetCardsForPokemonUseCaseTest {

    private val repo    = mockk<CardRepository>()
    private val useCase = GetCardsForPokemonUseCase(repo)

    @Test
    fun `invoke_returnsCardsFromRepository`() = runTest {
        val cards = listOf(TestFixtures.fireCard, TestFixtures.waterCard)
        coEvery { repo.getCardsForPokemon("charizard") } returns cards
        assertEquals(cards, useCase.execute("charizard"))
    }

    @Test
    fun `invoke_whenNoCardsFound_returnsEmptyList`() = runTest {
        coEvery { repo.getCardsForPokemon(any()) } returns emptyList()
        assertTrue(useCase.execute("notapokemon").isEmpty())
    }
}

class GetOwnedCardsUseCaseTest {

    private val repo    = mockk<CardRepository>()
    private val useCase = GetOwnedCardsUseCase(repo)

    @Test
    fun `invoke_returnsFlowFromRepository`() = runTest {
        val cards = listOf(TestFixtures.fireCard)
        every { repo.getOwnedCards() } returns flowOf(cards)
        useCase.execute().collect { assertEquals(cards, it) }
    }

    @Test
    fun `invoke_whenNoOwnedCards_emitsEmptyList`() = runTest {
        every { repo.getOwnedCards() } returns flowOf(emptyList())
        useCase.execute().collect { assertTrue(it.isEmpty()) }
    }
}

class GetGymsUseCaseTest {

    private val repo    = mockk<GymRepository>()
    private val useCase = GetGymsUseCase(repo)

    @Test
    fun `invoke_returnsGymsFromRepository`() = runTest {
        val gyms = listOf(
            Gym("Pewter", "City", 0.0, 0.0, "rock",
                listOf(TestFixtures.fireCard, TestFixtures.waterCard, TestFixtures.grassCard))
        )
        coEvery { repo.getGyms() } returns gyms
        assertEquals(gyms, useCase.execute())
    }
}

class GetWeatherConditionUseCaseTest {

    private val repo    = mockk<WeatherRepository>()
    private val useCase = GetWeatherConditionUseCase(repo)

    @Test
    fun `invoke_returnsConditionFromRepository`() = runTest {
        coEvery { repo.getWeatherCondition(-38.7, -62.3) } returns WeatherCondition.RAIN
        assertEquals(WeatherCondition.RAIN, useCase.execute(-38.7, -62.3))
    }

    @Test
    fun `invoke_passesCoordinatesToRepository`() = runTest {
        coEvery { repo.getWeatherCondition(any(), any()) } returns WeatherCondition.CLEAR
        useCase.execute(35.6, 139.6)
        coVerify { repo.getWeatherCondition(35.6, 139.6) }
    }
}

class SaveBattleResultUseCaseTest {

    private val repo        = mockk<BattleRepository>(relaxed = true)
    private val cardRepo    = mockk<CardRepository>(relaxed = true)
    private val updateStats = mockk<UpdateStatisticsAfterBattleUseCase>(relaxed = true)
    private val useCase     = SaveBattleResultUseCase(repo, cardRepo, updateStats)

    @Test
    fun `invoke_savesBattleBeforeUpdatingStats`() = runTest {
        useCase.execute(TestFixtures.playerWin)
        coVerifyOrder {
            repo.saveBattleResult(TestFixtures.playerWin)
            updateStats.execute(TestFixtures.playerWin)
        }
    }

    @Test
    fun `invoke_callsUpdateStatisticsExactlyOnce`() = runTest {
        useCase.execute(TestFixtures.playerWin)
        coVerify(exactly = 1) { updateStats.execute(TestFixtures.playerWin) }
    }

    @Test
    fun `invoke_registersBattleUsageForPlayerCards`() = runTest {
        useCase.execute(TestFixtures.playerWin)
        coVerify { cardRepo.registerBattleUsage(TestFixtures.playerWin.playerCards.map { it.id }) }
    }
}
