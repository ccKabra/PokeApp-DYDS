package com.pokemonarena.presentation

import com.pokemonarena.domain.entity.Pokemon
import com.pokemonarena.domain.entity.Region
import com.pokemonarena.domain.usecase.GetRegionProgressUseCase
import com.pokemonarena.presentation.fakes.FakeGetPokemonsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import com.pokemonarena.presentation.screens.collection.CollectionUiEvent
import com.pokemonarena.presentation.screens.collection.CollectionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var useCase: FakeGetPokemonsUseCase
    private lateinit var viewModel: CollectionViewModel

    private val getProgress = mockk<GetRegionProgressUseCase> {
        coEvery { execute() } returns GetRegionProgressUseCase.Progress(
            setOf(Region.KANTO), Region.KANTO.maxPokedexId, emptySet())
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        useCase    = FakeGetPokemonsUseCase()
        viewModel  = CollectionViewModel(useCase, getProgress)
    }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `init deberia cargar todos los pokemones`() = runTest {
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(useCase.pokemons.size, viewModel.uiState.value.allPokemons.size)
    }

    @Test
    fun `onQueryChange deberia filtrar por nombre`() = runTest {
        viewModel.onQueryChange("char")
        val filtered = viewModel.uiState.value.filteredPokemons
        assertTrue(filtered.all { it.name.contains("char") })
    }

    @Test
    fun `onQueryChange con query vacia deberia mostrar todos`() = runTest {
        viewModel.onQueryChange("char")
        viewModel.onQueryChange("")
        assertEquals(useCase.pokemons.size, viewModel.uiState.value.filteredPokemons.size)
    }

    @Test
    fun `clearQuery deberia resetear el filtro`() = runTest {
        viewModel.onQueryChange("bulb")
        viewModel.clearQuery()
        assertEquals("", viewModel.uiState.value.query)
        assertEquals(useCase.pokemons.size, viewModel.uiState.value.filteredPokemons.size)
    }

    @Test
    fun `loadPokemons con error deberia setear campo error`() = runTest {
        useCase.shouldFail = true
        viewModel.loadPokemons()
        assertTrue(viewModel.uiState.value.error != null)
    }
    @Test
    fun `onEvent_queryChanged_filtersList`() = runTest {
        viewModel.onEvent(CollectionUiEvent.QueryChanged("bulb"))
        assertEquals("bulb", viewModel.uiState.value.query)
        assertTrue(viewModel.uiState.value.filteredPokemons.all { it.name.contains("bulb") })
    }

}
