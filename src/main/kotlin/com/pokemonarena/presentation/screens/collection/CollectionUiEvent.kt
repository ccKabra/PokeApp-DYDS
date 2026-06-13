package com.pokemonarena.presentation.screens.collection

sealed interface CollectionUiEvent {
    data class QueryChanged(val query: String) : CollectionUiEvent
    object ClearQuery : CollectionUiEvent
    object Retry      : CollectionUiEvent
}
