package com.pokemonarena.presentation.screens.myteam

import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.CollectionRules
import com.pokemonarena.domain.entity.Item
import com.pokemonarena.domain.entity.ItemCatalog
import com.pokemonarena.domain.entity.TeamRules
import com.pokemonarena.domain.entity.UserStatistics

data class MyTeamState(
    val ownedCards: List<Card>       = emptyList(),
    val teamCards:  List<Card>       = emptyList(),
    val stats:      UserStatistics   = UserStatistics(),
    val catalog:    List<Item>       = emptyList(),
    val inventory:  Map<String, Int> = emptyMap(),
    val isLoading:  Boolean          = true
) {
    val teamIsFull get() = teamCards.size >= TeamRules.SIZE
    val canBattle  get() = teamCards.size == TeamRules.SIZE
    val maxCards   get() = CollectionRules.MAX_OWNED_CARDS
    val availableItems: List<Pair<Item, Int>>
        get() = catalog.filterNot { ItemCatalog.isConsumable(it.id) }
            .mapNotNull { item -> inventory[item.id]?.takeIf { it > 0 }?.let { item to it } }
    val fatigueCures: Int get() = inventory[ItemCatalog.ENERGY_ROOT_ID] ?: 0
}
