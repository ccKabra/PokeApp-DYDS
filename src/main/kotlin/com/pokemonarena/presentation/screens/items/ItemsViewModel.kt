package com.pokemonarena.presentation.screens.items

import com.pokemonarena.domain.usecase.GetItemCatalogUseCase
import com.pokemonarena.domain.usecase.GetItemInventoryUseCase
import com.pokemonarena.domain.usecase.GetUserCoinsUseCase
import com.pokemonarena.domain.usecase.PurchaseItemUseCase
import com.pokemonarena.presentation.BaseViewModel
import com.pokemonarena.presentation.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ItemsViewModel(
    private val getCatalog:   GetItemCatalogUseCase,
    private val getInventory: GetItemInventoryUseCase,
    private val getUserCoins: GetUserCoinsUseCase,
    private val purchaseItem: PurchaseItemUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ItemsUiState())
    val uiState: StateFlow<ItemsUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            val catalog = runCatching { getCatalog.execute() }.getOrDefault(emptyList())
            _uiState.update { it.copy(catalog = catalog, isLoading = false) }
        }
        scope.launch {
            combine(getInventory.execute(), getUserCoins.execute()) { inv, coins -> inv to coins }
                .collect { (inv, coins) -> _uiState.update { it.copy(inventory = inv, coins = coins) } }
        }
    }

    fun onEvent(event: ItemsUiEvent) {
        when (event) {
            is ItemsUiEvent.Purchase -> scope.launch { purchase(event) }
        }
    }

    private suspend fun purchase(event: ItemsUiEvent.Purchase) {
        when (val result = purchaseItem.execute(event.item)) {
            is PurchaseItemUseCase.Result.Success ->
                _uiState.update { it.copy(message = UiMessage(
                    "${event.item.name} comprado. Te quedan ${result.remainingCoins} monedas.",
                    isSuccess = true)) }
            is PurchaseItemUseCase.Result.InsufficientCoins ->
                _uiState.update { it.copy(message = UiMessage(
                    "${event.item.name} cuesta ${result.price} monedas y solo tenés ${result.currentCoins}.",
                    isSuccess = false)) }
        }
    }
}
