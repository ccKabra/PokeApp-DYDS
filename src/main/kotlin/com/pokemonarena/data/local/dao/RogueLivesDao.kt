package com.pokemonarena.data.local.dao

import com.pokemonarena.data.local.database.RogueLivesTable
import com.pokemonarena.domain.entity.RogueLives
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class RogueLivesDao {

    suspend fun get(nowMs: Long): RogueLives = withContext(Dispatchers.IO) {
        transaction {
            val row = RogueLivesTable.selectAll().firstOrNull()
            if (row == null) {
                RogueLivesTable.insert {
                    it[lives]      = RogueLives.MAX
                    it[lastRefill] = nowMs
                }
                RogueLives(RogueLives.MAX, nowMs)
            } else {
                RogueLives(row[RogueLivesTable.lives], row[RogueLivesTable.lastRefill])
            }
        }
    }

    suspend fun save(state: RogueLives) = withContext(Dispatchers.IO) {
        transaction {
            val updated = RogueLivesTable.update({ RogueLivesTable.id eq 1 }) {
                it[lives]      = state.lives
                it[lastRefill] = state.lastRefillMs
            }
            if (updated == 0) RogueLivesTable.insert {
                it[id]         = 1
                it[lives]      = state.lives
                it[lastRefill] = state.lastRefillMs
            }
        }
    }
}
