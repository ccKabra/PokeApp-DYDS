package com.pokemonarena.data.repository

import com.pokemonarena.data.external.ItemsExternalSource
import com.pokemonarena.data.local.dao.ItemInventoryDao
import com.pokemonarena.domain.entity.Item
import com.pokemonarena.domain.entity.ItemCatalog
import com.pokemonarena.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow

class ItemRepositoryImpl(
    private val source: ItemsExternalSource,
    private val dao:    ItemInventoryDao
) : ItemRepository {

    private var cachedCatalog: List<Item>? = null

    override suspend fun getCatalog(): List<Item> =
        cachedCatalog ?: (ItemCatalog.ALL + ItemCatalog.CONSUMABLES + ItemCatalog.EXCLUSIVES).map { item ->
            source.getItemSpriteUrl(item.id)?.let { item.copy(imageUrl = it) } ?: item
        }.also { cachedCatalog = it }

    override fun getInventory(): Flow<Map<String, Int>> = dao.observeAll()

    override suspend fun addToInventory(itemId: String)      = dao.increment(itemId)
    override suspend fun removeFromInventory(itemId: String) = dao.decrement(itemId)
}
