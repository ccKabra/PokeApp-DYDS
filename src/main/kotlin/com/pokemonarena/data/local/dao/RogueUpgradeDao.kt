package com.pokemonarena.data.local.dao

import com.pokemonarena.data.local.database.RogueUpgradesTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class RogueUpgradeDao {
    private val _changes = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    fun observeAll(): Flow<Map<String, Int>> = _changes.map {
        transaction {
            RogueUpgradesTable.selectAll().associate {
                it[RogueUpgradesTable.upgradeId] to it[RogueUpgradesTable.level]
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun setLevel(upgradeId: String, level: Int) = withContext(Dispatchers.IO) {
        transaction {
            val updated = RogueUpgradesTable.update({ RogueUpgradesTable.upgradeId eq upgradeId }) {
                it[RogueUpgradesTable.level] = level
            }
            if (updated == 0) RogueUpgradesTable.insert {
                it[RogueUpgradesTable.upgradeId] = upgradeId
                it[RogueUpgradesTable.level]     = level
            }
        }
        _changes.emit(Unit)
    }
}
