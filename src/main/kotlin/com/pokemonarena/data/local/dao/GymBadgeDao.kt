package com.pokemonarena.data.local.dao

import com.pokemonarena.data.local.database.GymBadges
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class GymBadgeDao {
    private val _changes = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    fun observeAll(): Flow<Set<String>> = _changes.map {
        transaction { GymBadges.selectAll().map { it[GymBadges.gymName] }.toSet() }
    }.flowOn(Dispatchers.IO)

    suspend fun contains(gymName: String): Boolean = withContext(Dispatchers.IO) {
        transaction { GymBadges.select { GymBadges.gymName eq gymName }.count() > 0 }
    }

    suspend fun insert(gymName: String) = withContext(Dispatchers.IO) {
        transaction {
            GymBadges.insertIgnore {
                it[GymBadges.gymName]    = gymName
                it[GymBadges.earnedDate] = LocalDateTime.now().toString()
            }
        }
        _changes.emit(Unit)
    }
}
