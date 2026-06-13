package com.pokemonarena.data.local.dao

import com.pokemonarena.data.local.database.ItemInventory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class ItemInventoryDao {
    private val _changes = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    fun observeAll(): Flow<Map<String, Int>> = _changes.map {
        transaction {
            ItemInventory.selectAll().associate {
                it[ItemInventory.itemId] to it[ItemInventory.quantity]
            }.filterValues { qty -> qty > 0 }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun increment(itemId: String) = withContext(Dispatchers.IO) {
        transaction {
            val updated = ItemInventory.update({ ItemInventory.itemId eq itemId }) {
                with(SqlExpressionBuilder) { it[quantity] = quantity + 1 }
            }
            if (updated == 0) ItemInventory.insert {
                it[ItemInventory.itemId]   = itemId
                it[ItemInventory.quantity] = 1
            }
        }
        _changes.emit(Unit)
    }

    suspend fun decrement(itemId: String) = withContext(Dispatchers.IO) {
        transaction {
            ItemInventory.update({ (ItemInventory.itemId eq itemId) and (ItemInventory.quantity greater 0) }) {
                with(SqlExpressionBuilder) { it[quantity] = quantity - 1 }
            }
        }
        _changes.emit(Unit)
    }
}
