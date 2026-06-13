package com.pokemonarena.presentation.screens.collection

import com.pokemonarena.domain.entity.Region
import com.pokemonarena.domain.usecase.GetPokemonsUseCase
import com.pokemonarena.domain.usecase.GetRegionProgressUseCase
import com.pokemonarena.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CollectionViewModel(
    private val getPokemons: GetPokemonsUseCase,
    private val getProgress: GetRegionProgressUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    init { loadPokemons() }

    fun onEvent(event: CollectionUiEvent) {
        when (event) {
            is CollectionUiEvent.QueryChanged -> onQueryChange(event.query)
            is CollectionUiEvent.ClearQuery   -> clearQuery()
            is CollectionUiEvent.Retry        -> loadPokemons()
        }
    }

    fun loadPokemons() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val progress = getProgress.execute()
                val list     = getPokemons.execute(limit = progress.maxPokedexId)
                val locked   = Region.entries.firstOrNull { it !in progress.unlockedRegions }
                _uiState.update { CollectionUiState(
                    allPokemons = list, filteredPokemons = list,
                    unlockedRegions = progress.unlockedRegions,
                    nextRegionHint = locked?.let {
                        "Conquistá los gimnasios y la Liga de ${it.previous?.displayName} para desbloquear la Pokédex de ${it.displayName}."
                    }) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onQueryChange(query: String) {
        val filtered = if (query.isBlank()) _uiState.value.allPokemons
                       else _uiState.value.allPokemons.filter { it.name.contains(query.lowercase()) }
        _uiState.update { it.copy(query = query, filteredPokemons = filtered) }
    }

    fun clearQuery() {
        _uiState.update { it.copy(query = "", filteredPokemons = it.allPokemons) }
    }
}
