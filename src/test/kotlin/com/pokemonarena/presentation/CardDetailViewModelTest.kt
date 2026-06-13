package com.pokemonarena.presentation

import com.pokemonarena.TestFixtures
import com.pokemonarena.domain.usecase.GetCardsForPokemonUseCase
import com.pokemonarena.domain.usecase.GetOwnedCardsUseCase
import com.pokemonarena.domain.usecase.GetPokemonDetailUseCase
import com.pokemonarena.domain.usecase.GetUserStatisticsUseCase
import com.pokemonarena.domain.usecase.PurchaseCardUseCase
import com.pokemonarena.presentation.screens.detail.CardDetailViewModel
import com.pokemonarena.presentation.screens.detail.DetailUiEvent
import com.pokemonarena.presentation.screens.detail.DetailUiState
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
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class CardDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getDetail = mockk<GetPokemonDetailUseCase>()
    private val getCards  = mockk<GetCardsForPokemonUseCase>()
    private val purchase  = mockk<PurchaseCardUseCase>(relaxed = true)
    private val getOwned  = mockk<GetOwnedCardsUseCase>()
    private val getStats  = mockk<GetUserStatisticsUseCase>()

    @BeforeTest
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = CardDetailViewModel(getDetail, getCards, purchase, getOwned, getStats)

    @Test
    fun `onEvent_load_transitionsToSuccessWithCards`() = runTest {
        coEvery { getDetail.execute("bulbasaur") } returns TestFixtures.fireCard.pokemonDetail
        coEvery { getCards.execute("bulbasaur")  } returns listOf(TestFixtures.fireCard)
        every   { getOwned.execute() } returns flowOf(emptyList())
        every   { getStats.execute() } returns flowOf(TestFixtures.emptyStats)

        val viewModel = vm()
        viewModel.onEvent(DetailUiEvent.Load("bulbasaur"))

        assertIs<DetailUiState.Success>(viewModel.uiState.value)
        val success = viewModel.uiState.value as DetailUiState.Success
        assertEquals(1, success.cards.size)
        assertEquals(TestFixtures.fireCard.id, success.selectedCard?.id)
    }

    @Test
    fun `onEvent_load_whenDetailFails_transitionsToError`() = runTest {
        coEvery { getDetail.execute(any()) } throws RuntimeException("404")

        val viewModel = vm()
        viewModel.onEvent(DetailUiEvent.Load("missingno"))

        assertIs<DetailUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun `onEvent_cardSelected_updatesSelectedCard`() = runTest {
        coEvery { getDetail.execute(any()) } returns TestFixtures.fireCard.pokemonDetail
        coEvery { getCards.execute(any())  } returns listOf(TestFixtures.fireCard, TestFixtures.waterCard)
        every   { getOwned.execute() } returns flowOf(emptyList())
        every   { getStats.execute() } returns flowOf(TestFixtures.emptyStats)

        val viewModel = vm()
        viewModel.onEvent(DetailUiEvent.Load("bulbasaur"))
        viewModel.onEvent(DetailUiEvent.CardSelected(TestFixtures.waterCard))

        val success = viewModel.uiState.value as DetailUiState.Success
        assertEquals(TestFixtures.waterCard.id, success.selectedCard?.id)
    }
}
