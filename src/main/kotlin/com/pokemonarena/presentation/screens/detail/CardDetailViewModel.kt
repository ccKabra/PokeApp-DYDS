package com.pokemonarena.presentation.screens.detail

import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.usecase.GetCardsForPokemonUseCase
import com.pokemonarena.domain.usecase.GetOwnedCardsUseCase
import com.pokemonarena.domain.usecase.GetPokemonDetailUseCase
import com.pokemonarena.domain.usecase.GetUserStatisticsUseCase
import com.pokemonarena.domain.usecase.PurchaseCardUseCase
import com.pokemonarena.presentation.BaseViewModel
import com.pokemonarena.presentation.UiMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CardDetailViewModel(
    private val getDetail: GetPokemonDetailUseCase,
    private val getCards:  GetCardsForPokemonUseCase,
    private val purchase:  PurchaseCardUseCase,
    private val getOwned:  GetOwnedCardsUseCase,
    private val getStats:  GetUserStatisticsUseCase
) : BaseViewModel() {

    private var observeJob: Job? = null

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun onEvent(event: DetailUiEvent) {
        when (event) {
            is DetailUiEvent.Load              -> load(event.pokemonName)
            is DetailUiEvent.CardSelected      -> selectCard(event.card)
            is DetailUiEvent.PurchaseRequested -> purchaseSelected()
        }
    }

    private fun load(pokemonName: String) {
        observeJob?.cancel()
        observeJob = scope.launch {
            _uiState.value = DetailUiState.Loading
            runCatching {
                val detail = getDetail.execute(pokemonName)
                val cards  = getCards.execute(pokemonName)
                combine(getOwned.execute(), getStats.execute()) { owned, stats ->
                    val ownedIds = owned.map { it.id }.toSet()
                    (_uiState.value as? DetailUiState.Success)
                        ?.copy(ownedCardIds = ownedIds, coins = stats.coins)
                        ?: DetailUiState.Success(detail, cards, cards.firstOrNull(), ownedIds, stats.coins)
                }.collect { _uiState.value = it }
            }.onFailure { _uiState.value = DetailUiState.Error(it.message ?: "Error") }
        }
    }

    private fun selectCard(card: Card) {
        (_uiState.value as? DetailUiState.Success)?.let {
            _uiState.value = it.copy(selectedCard = card, message = null)
        }
    }

    private fun purchaseSelected() {
        scope.launch {
            val s    = _uiState.value as? DetailUiState.Success ?: return@launch
            val card = s.selectedCard ?: return@launch
            if (card.id in s.ownedCardIds) return@launch
            when (val result = purchase.execute(card)) {
                is PurchaseCardUseCase.Result.Success ->
                    _uiState.value = s.copy(message = UiMessage(
                        "Carta comprada por ${result.pricePaid} monedas. Te quedan ${result.remainingCoins}.",
                        isSuccess = true))
                is PurchaseCardUseCase.Result.InsufficientCoins ->
                    _uiState.value = s.copy(message = UiMessage(
                        "Esta carta cuesta ${result.price} monedas y solo tenés ${result.currentCoins}.",
                        isSuccess = false))
                is PurchaseCardUseCase.Result.CollectionFull ->
                    _uiState.value = s.copy(message = UiMessage(
                        "Colección llena: solo podés tener ${result.maxCards} cartas. Vendé alguna en Mi Equipo para hacer lugar.",
                        isSuccess = false))
            }
        }
    }
}
